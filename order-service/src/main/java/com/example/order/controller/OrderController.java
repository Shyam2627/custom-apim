package com.example.order.controller;

import com.example.order.model.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private static final List<OrderResponse> ORDERS = List.of(
            new OrderResponse(5001L, "CREATED", "order-service"),
            new OrderResponse(5002L, "PROCESSING", "order-service"),
            new OrderResponse(5003L, "SHIPPED", "order-service")
    );

    @GetMapping("/orders")
    public List<OrderResponse> getOrders() {
        log.info("Fetching all orders, count={}", ORDERS.size());
        return ORDERS;
    }

    @GetMapping("/orders/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        log.info("Fetching order by id={}", id);
        return ORDERS.stream()
                .filter(order -> order.id().equals(id))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Order not found for id={}", id);
                    return new IllegalArgumentException("Order not found");
                });
    }
}