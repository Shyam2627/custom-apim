package com.example.apim;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("local")
@TestPropertySource(properties = "LOCAL_ADMIN_TOKEN=test-local-admin-token")
class LocalAdminTokenTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private Map<String, RouteLocator> routeLocators;

    @Test
    void localAdminTokenCanReadApiDefinitions() {
        webTestClient.get()
                .uri("/admin/apis")
                .headers(headers -> headers.setBearerAuth("test-local-admin-token"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void invalidLocalTokenIsRejected() {
        webTestClient.get()
                .uri("/admin/apis")
                .headers(headers -> headers.setBearerAuth("wrong-token"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

            @Test
            void adminCanApproveHostAndRegisterApiWithoutRestart() {
            webTestClient.post()
                .uri("/admin/hosts")
                .headers(headers -> headers.setBearerAuth("test-local-admin-token"))
                .bodyValue(Map.of("host", "httpbin.org"))
                .exchange()
                .expectStatus().isCreated();

            webTestClient.post()
                .uri("/admin/apis")
                .headers(headers -> headers.setBearerAuth("test-local-admin-token"))
                .bodyValue(Map.of(
                    "id", "httpbin-test",
                    "path", "/api/v1/httpbin/**",
                    "uri", "https://httpbin.org",
                    "stripPrefix", 3,
                    "requiredScope", "external.read",
                    "removeAuthorizationHeader", true))
                .exchange()
                .expectStatus().isCreated();

            Boolean routeLoaded = Flux.fromIterable(routeLocators.values())
                .flatMap(RouteLocator::getRoutes)
                .any(route -> "httpbin-test".equals(route.getId()))
                .block();

            assertTrue(Boolean.TRUE.equals(routeLoaded));
            }
}