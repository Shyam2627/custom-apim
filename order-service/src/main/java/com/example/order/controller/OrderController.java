package com.example.order.controller;

import com.example.order.model.OrderResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class OrderController {

    private static final List<OrderResponse> ORDERS = List.of(
            new OrderResponse(5001L, "CREATED", "order-service"),
            new OrderResponse(5002L, "PROCESSING", "order-service"),
            new OrderResponse(5003L, "SHIPPED", "order-service")
    );

    @GetMapping("/orders")
    public List<OrderResponse> getOrders() {
        return ORDERS;
    }

    @GetMapping("/orders/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return ORDERS.stream()
                .filter(order -> order.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }
}
