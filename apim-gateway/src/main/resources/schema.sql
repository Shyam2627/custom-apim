CREATE TABLE IF NOT EXISTS api_definitions (
    id VARCHAR(64) PRIMARY KEY,
    route_path VARCHAR(256) NOT NULL UNIQUE,
    gateway_context_path VARCHAR(128),
    upstream_uri VARCHAR(2048) NOT NULL,
    upstream_context_path VARCHAR(512),
    strip_prefix INTEGER NOT NULL DEFAULT 0,
    required_scope VARCHAR(128) NOT NULL,
    remove_authorization_header BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS api_allowed_hosts (
    host VARCHAR(253) PRIMARY KEY
);