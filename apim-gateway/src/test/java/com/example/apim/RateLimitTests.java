package com.example.apim;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class RateLimitTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @WithMockUser(authorities = "SCOPE_api.read")
    void rateLimitReturns429() {
        for (int i = 0; i < 101; i++) {
            webTestClient.get()
                    .uri("/api/v1/users")
                    .exchange();
        }

        webTestClient.get()
            .uri("/api/v1/users")
                .exchange()
                .expectStatus().isEqualTo(429);
    }
}
