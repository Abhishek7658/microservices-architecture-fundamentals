package order_service.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import order_service.client.UserServiceClient;
import order_service.dto.CreateOrderRequest;
import order_service.dto.OrderResponse;
import order_service.dto.UpdateOrderRequest;
import order_service.dto.UserResponse;
import order_service.event.OrderCreatedEvent;
import order_service.event.OrderEventPublisher;
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
    private final OrderEventPublisher orderEventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:order:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserServiceClient userServiceClient,
            OrderEventPublisher orderEventPublisher,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userServiceClient = userServiceClient;
        this.orderEventPublisher = orderEventPublisher;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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
            CreateOrderRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required");
        }

        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;

        String existingResponse =
                redisTemplate.opsForValue().get(redisKey);

        if (existingResponse != null) {

            if ("PROCESSING".equals(existingResponse)) {
                throw new IllegalStateException(
                        "Request with this Idempotency-Key is already being processed");
            }

            try {
                return objectMapper.readValue(
                        existingResponse,
                        OrderResponse.class);

            } catch (JsonProcessingException e) {
                throw new IllegalStateException(
                        "Unable to read stored idempotent response",
                        e);
            }
        }

        Boolean keyCreated = redisTemplate.opsForValue()
                .setIfAbsent(
                        redisKey,
                        "PROCESSING",
                        IDEMPOTENCY_TTL);

        if (!Boolean.TRUE.equals(keyCreated)) {

            String storedResponse =
                    redisTemplate.opsForValue().get(redisKey);

            if ("PROCESSING".equals(storedResponse)) {
                throw new IllegalStateException(
                        "Request with this Idempotency-Key is already being processed");
            }

            try {
                return objectMapper.readValue(
                        storedResponse,
                        OrderResponse.class);

            } catch (JsonProcessingException e) {
                throw new IllegalStateException(
                        "Unable to read stored idempotent response",
                        e);
            }
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

            Order savedOrder =
                    orderRepository.save(order);

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(savedOrder);
            orderItem.setProductId(savedOrder.getProductId());
            orderItem.setProductName(savedOrder.getProductName());
            orderItem.setQuantity(savedOrder.getQuantity());
            orderItem.setAmount(savedOrder.getAmount());

            orderItemRepository.save(orderItem);

            OrderCreatedEvent event =
                    new OrderCreatedEvent(
                            savedOrder.getId(),
                            savedOrder.getUserId(),
                            savedOrder.getProductId(),
                            savedOrder.getProductName(),
                            savedOrder.getQuantity(),
                            BigDecimal.valueOf(
                                    savedOrder.getAmount()),
                            "ORDER_CREATED",
                            savedOrder.getCreatedAt()
                    );

            orderEventPublisher.publishOrderCreated(event);

            OrderResponse response =
                    mapToResponse(savedOrder);

            String responseJson =
                    objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue().set(
                    redisKey,
                    responseJson,
                    IDEMPOTENCY_TTL);

            return response;

        } catch (Exception e) {

            redisTemplate.delete(redisKey);

            throw new RuntimeException(
                    "Order creation failed",
                    e);
        }
    }

    public OrderResponse getOrderById(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(
                        () -> new OrderNotFoundException(id));

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
                .orElseThrow(
                        () -> new OrderNotFoundException(id));

        order.setStatus(request.getStatus());
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder =
                orderRepository.save(order);

        return mapToResponse(updatedOrder);
    }

    public void cancelOrder(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(
                        () -> new OrderNotFoundException(id));

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