package com.example.apim.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("test")
public class TestGatewayTargetController {

    @GetMapping("/test-target")
    public Map<String, String> testTarget() {
        return Map.of("status", "ok");
    }
}