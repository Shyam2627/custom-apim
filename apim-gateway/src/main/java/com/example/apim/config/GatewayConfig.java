package com.example.apim.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewayServiceProperties.class)
public class GatewayConfig {

    @Bean
    public RouteLocator actuatorRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("actuator", routeSpec -> routeSpec.path("/actuator/**")
                        .filters(filterSpec -> filterSpec
                                .rewritePath("/actuator/(?<segment>.*)", "/actuator/${segment}"))
                        .uri("http://localhost:8080"))
                .build();
    }
}