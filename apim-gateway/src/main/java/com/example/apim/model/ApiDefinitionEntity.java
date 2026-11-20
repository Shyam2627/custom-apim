package com.example.apim.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("api_definitions")
public class ApiDefinitionEntity implements Persistable<String> {

    @Id
    private String id;
    @Column("route_path")
    private String path;
    @Column("gateway_context_path")
    private String gatewayContextPath;
    @Column("upstream_uri")
    private String uri;
    @Column("upstream_context_path")
    private String upstreamContextPath;
    @Column("strip_prefix")
    private int stripPrefix;
    @Column("required_scope")
    private String requiredScope;
    @Column("remove_authorization_header")
    private boolean removeAuthorizationHeader;
    @Transient
    private boolean newEntity;

    public ApiDefinitionEntity() {
        this.newEntity = false;
    }

    public ApiDefinitionEntity(String id, String path, String uri, int stripPrefix,
                               String requiredScope, boolean removeAuthorizationHeader) {
        this(id, path, inferGatewayContext(path), uri,
                inferUpstreamContext(uri), stripPrefix, requiredScope, removeAuthorizationHeader);
    }

    public ApiDefinitionEntity(String id, String path, String gatewayContextPath, String uri,
                               String upstreamContextPath, int stripPrefix,
                               String requiredScope, boolean removeAuthorizationHeader) {
        this.id = id;
        this.path = path;
        this.gatewayContextPath = gatewayContextPath;
        this.uri = uri;
        this.upstreamContextPath = upstreamContextPath;
        this.stripPrefix = stripPrefix;
        this.requiredScope = requiredScope;
        this.removeAuthorizationHeader = removeAuthorizationHeader;
        this.newEntity = true;
    }

    private static String inferGatewayContext(String path) {
        if (path == null) return "";
        String[] parts = path.split("/");
        return parts.length > 3 ? parts[3] : "";
    }

    private static String inferUpstreamContext(String uri) {
        if (uri == null) return "";
        try {
            String parsedPath = java.net.URI.create(uri).getRawPath();
            return parsedPath == null || parsedPath.isBlank() || "/".equals(parsedPath) ? "" : parsedPath;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return newEntity;
    }

    @Override
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }

    public String getGatewayContextPath() { return gatewayContextPath; }

    public void setGatewayContextPath(String gatewayContextPath) { this.gatewayContextPath = gatewayContextPath; }

    public String getUri() { return uri; }

    public void setUri(String uri) { this.uri = uri; }

    public String getUpstreamContextPath() { return upstreamContextPath; }

    public void setUpstreamContextPath(String upstreamContextPath) { this.upstreamContextPath = upstreamContextPath; }

    public int getStripPrefix() { return stripPrefix; }

    public void setStripPrefix(int stripPrefix) { this.stripPrefix = stripPrefix; }

    public String getRequiredScope() { return requiredScope; }

    public void setRequiredScope(String requiredScope) { this.requiredScope = requiredScope; }

    public boolean isRemoveAuthorizationHeader() { return removeAuthorizationHeader; }

    public void setRemoveAuthorizationHeader(boolean removeAuthorizationHeader) { this.removeAuthorizationHeader = removeAuthorizationHeader; }
}