package order_service.service;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import order_service.exception.OrderNotFoundException;
import order_service.model.Order;
import order_service.repository.OrderRepository;

@Service
public class OrderService {

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;

    public OrderService(RestTemplate restTemplate, OrderRepository orderRepository) {
        this.restTemplate = restTemplate;
        this.orderRepository = orderRepository;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public Object getUserById(Long userId) {
        String url = "http://localhost:8080/api/users/" + userId;
        try {
            return restTemplate.getForObject(url, Object.class);
        } catch (RestClientException e) {
            // Handle the exception (e.g., log it, throw a custom exception, etc.)
            //System.err.println("Error fetching user data: " + e.getMessage());
            throw new RuntimeException("User not found: " + userId);
        }
        //return restTemplate.getForObject(url, Object.class);
    }
}