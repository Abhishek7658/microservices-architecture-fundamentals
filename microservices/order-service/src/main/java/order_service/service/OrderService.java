
package order_service.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.client.UserServiceClient;
import order_service.dto.CreateOrderRequest;
import order_service.dto.OrderResponse;
import order_service.dto.UpdateOrderRequest;
import order_service.dto.UserResponse;
import order_service.event.OrderCreatedEvent;
import order_service.event.OutboxEvent;
import order_service.event.OutboxEventRepository;
import order_service.exception.IdempotencyConflictException;
import order_service.exception.OrderNotFoundException;
import order_service.model.Order;
import order_service.model.OrderItem;
import order_service.repository.OrderItemRepository;
import order_service.repository.OrderRepository;

enum OrderStatus {
    CREATED, CANCELLED
}

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserServiceClient userServiceClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:order:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final String PROCESSING_PREFIX = "PROCESSING:";

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserServiceClient userServiceClient,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            OutboxEventRepository outboxEventRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userServiceClient = userServiceClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public OrderResponse createOrder(
            String idempotencyKey,
            CreateOrderRequest request,
            String correlationId) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Order request is required");
        }

        String fingerprint = createRequestFingerprint(request);
        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;

        String existing = redisTemplate.opsForValue().get(redisKey);

        if (existing != null) {
            return handleExistingRequest(existing, fingerprint);
        }

        String processingValue = PROCESSING_PREFIX + fingerprint;

        Boolean reserved = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, processingValue, IDEMPOTENCY_TTL);

        if (!Boolean.TRUE.equals(reserved)) {
            String current = redisTemplate.opsForValue().get(redisKey);

            if (current == null) {
                throw new IllegalStateException(
                        "Unable to determine idempotency state. Retry with the same key.");
            }

            return handleExistingRequest(current, fingerprint);
        }

        try {
            UserResponse user =
                    userServiceClient.getUserById(request.getUserId());

            Order order = new Order();
            order.setUserId(user.getId());
            order.setProductId(request.getProductId());
            order.setProductName(request.getProductName());
            order.setQuantity(request.getQuantity());
            order.setAmount(request.getAmount());
            order.setStatus(OrderStatus.CREATED.name());

            LocalDateTime now = LocalDateTime.now();
            order.setCreatedAt(now);
            order.setUpdatedAt(now);

            Order savedOrder = orderRepository.save(order);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductId(savedOrder.getProductId());
            orderItem.setProductName(savedOrder.getProductName());
            orderItem.setQuantity(savedOrder.getQuantity());
            orderItem.setAmount(savedOrder.getAmount());

            orderItemRepository.save(orderItem);

            OrderCreatedEvent event = new OrderCreatedEvent(
                    savedOrder.getId(),
                    savedOrder.getUserId(),
                    savedOrder.getProductId(),
                    savedOrder.getProductName(),
                    savedOrder.getQuantity(),
                    BigDecimal.valueOf(savedOrder.getAmount()),
                    "ORDER_CREATED",
                    savedOrder.getCreatedAt());

            String eventPayload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    "ORDER_CREATED",
                    savedOrder.getId().toString(),
                    eventPayload,
                    correlationId);

            outboxEventRepository.save(outboxEvent);

            OrderResponse response = mapToResponse(savedOrder);

            String responseJson = objectMapper.writeValueAsString(response);
            String cachedValue = objectMapper.writeValueAsString(
                    Map.of(
                            "fingerprint", fingerprint,
                            "response", response));

            if (!TransactionSynchronizationManager
                    .isSynchronizationActive()) {
                throw new IllegalStateException(
                        "Transaction synchronization is not active; "
                        + "cannot safely finalize idempotency state");
            }

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            try {
                                redisTemplate.opsForValue().set(
                                        redisKey,
                                        cachedValue,
                                        IDEMPOTENCY_TTL);
                            } catch (RuntimeException redisFailure) {
                                // Keep the PROCESSING reservation.
                                // Removing it here could allow a retry
                                // to create a second order after DB commit.
                                // Recovery is required if Redis remains
                                // unavailable; see the notes below.
                            }
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                try {
                                    // Delete only if our reservation is
                                    // still present. Avoid deleting a value
                                    // written by another request.
                                    String current =
                                            redisTemplate.opsForValue().get(redisKey);

                                    if (processingValue.equals(current)) {
                                        redisTemplate.delete(redisKey);
                                    }
                                } catch (RuntimeException ignored) {
                                    // The reservation expires after its TTL.
                                }
                            }
                        }
                    });

            return response;

        } catch (Exception e) {
            try {
                String current = redisTemplate.opsForValue().get(redisKey);

                if (processingValue.equals(current)) {
                    redisTemplate.delete(redisKey);
                }
            } catch (RuntimeException ignored) {
                // Preserve the original failure. The reservation has a TTL.
            }

            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new IllegalStateException("Order creation failed", e);
        }
    }

    private OrderResponse handleExistingRequest(
            String storedValue,
            String requestedFingerprint) {

        if (storedValue.startsWith(PROCESSING_PREFIX)) {
            String reservedFingerprint =
                    storedValue.substring(PROCESSING_PREFIX.length());

            if (!reservedFingerprint.equals(requestedFingerprint)) {
                throw new IdempotencyConflictException(
                        "This Idempotency-Key is already associated "
                        + "with a different order request.");
            }

            throw new IllegalStateException(
                    "A request with this Idempotency-Key is still being processed. "
                    + "Retry the same request shortly.");
        }

        try {
            JsonNode root = objectMapper.readTree(storedValue);
            JsonNode fingerprintNode = root.get("fingerprint");
            JsonNode responseNode = root.get("response");

            if (fingerprintNode == null || responseNode == null) {
                // A response written by the previous implementation has
                // no fingerprint. Do not assume its payload matches.
                throw new IdempotencyConflictException(
                        "This key contains a legacy idempotency response "
                        + "without a request fingerprint. Use a new key.");
            }

            if (!requestedFingerprint.equals(fingerprintNode.asText())) {
                throw new IdempotencyConflictException(
                        "This Idempotency-Key was already used "
                        + "with a different order request.");
            }

            return objectMapper.treeToValue(
                    responseNode, OrderResponse.class);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Unable to read stored idempotency response", e);
        }
    }

    private String createRequestFingerprint(CreateOrderRequest request) {
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("userId", request.getUserId());
            fields.put("productId", request.getProductId());
            fields.put("productName", request.getProductName());
            fields.put("quantity", request.getQuantity());
            fields.put("amount", request.getAmount());

            String canonicalRequest = objectMapper.writeValueAsString(fields);

            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Unable to calculate order request fingerprint", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is not available", e);
        }
    }

    private OrderResponse readStoredResponse(String storedResponse) {
        try {
            return objectMapper.readValue(
                    storedResponse, OrderResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Unable to read stored idempotent response", e);
        }
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public OrderResponse updateOrderStatus(
            Long id,
            UpdateOrderRequest request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        order.setStatus(request.getStatus());
        order.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(orderRepository.save(order));
    }

    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        order.setStatus(OrderStatus.CANCELLED.name());
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setProductName(order.getProductName());
        response.setQuantity(order.getQuantity());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        return response;
    }
}
