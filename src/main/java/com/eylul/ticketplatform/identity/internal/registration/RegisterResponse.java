package com.eylul.ticketplatform.identity.internal.registration;

import com.eylul.ticketplatform.identity.Role;

import java.util.UUID;

public record RegisterResponse(UUID id , String email, Role role) {
}
