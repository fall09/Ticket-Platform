package com.eylul.ticketplatform.identity.internal.registration;

import com.eylul.ticketplatform.identity.Role;
import com.eylul.ticketplatform.identity.internal.user.AppUser;
import com.eylul.ticketplatform.identity.internal.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RegistrationService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUser register(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyUsedException("Email already exists");
        }
        String passwordHash = passwordEncoder.encode(password);
        AppUser appUser = new AppUser(normalizedEmail, passwordHash, Role.USER);
        return appUserRepository.save(appUser);
    }

}
