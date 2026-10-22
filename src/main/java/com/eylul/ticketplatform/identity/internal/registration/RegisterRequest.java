package com.eylul.ticketplatform.identity.internal.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank
        @Size(min = 8, max = 72, message = "Password must have 8 to 72 characters")
        @Pattern(regexp = ".*[A-Z].*", message = "Password must contain an upper case letter")
        @Pattern(regexp = ".*[0-9].*", message = "Password must contain a digit")
        @Pattern(regexp = ".*[^A-Za-z0-9].*", message = "Password must contain a special character")
        String password) {
}
