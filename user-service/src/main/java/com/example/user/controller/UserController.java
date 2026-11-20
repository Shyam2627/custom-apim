package com.example.user.controller;

import com.example.user.model.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class UserController {

    private static final List<UserResponse> USERS = List.of(
            new UserResponse(100L, "Test User", "user-service"),
            new UserResponse(101L, "Jane Doe", "user-service"),
            new UserResponse(102L, "John Smith", "user-service")
    );

    @GetMapping("/users")
    public List<UserResponse> getUsers() {
        return USERS;
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return USERS.stream()
                .filter(user -> user.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
