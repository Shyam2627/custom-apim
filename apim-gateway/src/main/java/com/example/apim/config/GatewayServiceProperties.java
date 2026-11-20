package com.example.apim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class GatewayServiceProperties {

    private List<ServiceRoute> services = new ArrayList<>();
    private List<String> allowedHosts = new ArrayList<>();

    public List<ServiceRoute> getServices() {
        return services;
    }

    public void setServices(List<ServiceRoute> services) {
        this.services = services;
    }

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    public static class ServiceRoute {
        private String id;
        private String path;
        private String uri;
        private String gatewayContextPath;
        private String upstreamContextPath;
        private int stripPrefix;
        private String requiredScope;
        private boolean removeAuthorizationHeader;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getGatewayContextPath() {
            return gatewayContextPath;
        }

        public void setGatewayContextPath(String gatewayContextPath) {
            this.gatewayContextPath = gatewayContextPath;
        }

        public String getUpstreamContextPath() {
            return upstreamContextPath;
        }

        public void setUpstreamContextPath(String upstreamContextPath) {
            this.upstreamContextPath = upstreamContextPath;
        }

        public int getStripPrefix() {
            return stripPrefix;
        }

        public void setStripPrefix(int stripPrefix) {
            this.stripPrefix = stripPrefix;
        }

        public String getRequiredScope() {
            return requiredScope;
        }

        public void setRequiredScope(String requiredScope) {
            this.requiredScope = requiredScope;
        }

        public boolean isRemoveAuthorizationHeader() {
            return removeAuthorizationHeader;
        }

        public void setRemoveAuthorizationHeader(boolean removeAuthorizationHeader) {
            this.removeAuthorizationHeader = removeAuthorizationHeader;
        }
    }
}