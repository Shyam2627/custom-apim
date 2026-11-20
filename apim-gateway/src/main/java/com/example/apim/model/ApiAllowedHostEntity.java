package com.example.apim.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table("api_allowed_hosts")
public class ApiAllowedHostEntity implements Persistable<String> {

    @Id
    private String host;
    @Transient
    private boolean newEntity;

    public ApiAllowedHostEntity() {
        this.newEntity = false;
    }

    public ApiAllowedHostEntity(String host) {
        this.host = host;
        this.newEntity = true;
    }

    @Override
    public String getId() {
        return host;
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return newEntity;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}