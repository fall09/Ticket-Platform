package com.eylul.ticketplatform.identity.internal.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {}
