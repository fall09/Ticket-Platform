package com.eylul.ticketplatform.identity.internal;

public record LoginResponse(
        String accessToken,
        String tokenType
) {}