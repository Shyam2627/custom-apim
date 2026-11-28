package com.example.user.controller;

import com.example.user.model.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final List<UserResponse> USERS = List.of(
            new UserResponse(100L, "Test User", "user-service"),
            new UserResponse(101L, "Jane Doe", "user-service"),
            new UserResponse(102L, "John Smith", "user-service")
    );

    @GetMapping("/users")
    public List<UserResponse> getUsers() {
        log.info("Fetching all users, count={}", USERS.size());
        return USERS;
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        log.info("Fetching user by id={}", id);
        return USERS.stream()
                .filter(user -> user.id().equals(id))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("User not found for id={}", id);
                    return new IllegalArgumentException("User not found");
                });
    }
}