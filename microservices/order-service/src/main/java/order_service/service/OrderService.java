package order_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserServiceClient userServiceClient,
            OrderEventPublisher orderEventPublisher) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userServiceClient = userServiceClient;
        this.orderEventPublisher = orderEventPublisher;
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

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
                savedOrder.getCreatedAt()
        );

        orderEventPublisher.publishOrderCreated(event);

        return mapToResponse(savedOrder);
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

        Order updatedOrder = orderRepository.save(order);

        return mapToResponse(updatedOrder);
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