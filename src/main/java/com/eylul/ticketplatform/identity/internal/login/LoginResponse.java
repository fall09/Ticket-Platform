package com.eylul.ticketplatform.identity.internal.login;

public record LoginResponse(
        String accessToken,
        String tokenType
) {}