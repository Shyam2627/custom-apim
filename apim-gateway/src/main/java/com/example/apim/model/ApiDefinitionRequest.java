package com.example.apim.model;

public record ApiDefinitionRequest(
        String id,
        String path,
        String gatewayContextPath,
        String uri,
        String upstreamContextPath,
        int stripPrefix,
        String requiredScope,
        boolean removeAuthorizationHeader) {
}