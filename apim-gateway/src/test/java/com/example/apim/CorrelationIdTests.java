//package com.example.apim;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import org.springframework.test.context.ActiveProfiles;
//
//@SpringBootTest
//@AutoConfigureWebTestClient
//@ActiveProfiles("test")
//class CorrelationIdTests {
//
//    @Autowired
//    private WebTestClient webTestClient;
//
//    @Test
//    @WithMockUser(authorities = "SCOPE_api.read")
//    void correlationIdGeneratedWhenMissing() {
//        webTestClient.get()
//                .uri("/api/v1/users")
//                .exchange()
//                .expectStatus().isOk()
//                .expectHeader().exists("X-Correlation-ID");
//    }
//
//    @Test
//    @WithMockUser(authorities = "SCOPE_api.read")
//    void correlationIdPropagatedWhenProvided() {
//        webTestClient.get()
//                .uri("/api/v1/users")
//                .header("X-Correlation-ID", "abc-123")
//                .exchange()
//                .expectStatus().isOk()
//                .expectHeader().valueEquals("X-Correlation-ID", "abc-123");
//    }
//}
