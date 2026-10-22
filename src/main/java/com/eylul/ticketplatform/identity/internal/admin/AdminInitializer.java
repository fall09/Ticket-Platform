package com.eylul.ticketplatform.identity.internal.admin;

import com.eylul.ticketplatform.identity.Role;
import com.eylul.ticketplatform.identity.internal.user.AppUser;
import com.eylul.ticketplatform.identity.internal.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        boolean isAdminCreatedBefore = appUserRepository.existsByRole(Role.ADMIN);
        if (isAdminCreatedBefore) {
            return;
        }
        String email = adminProperties.email();
        String password = adminProperties.password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Admin settings are missing, no admin account was created");
            return;
        }
        String normalizedEmail = email.trim().toLowerCase();
        String passwordHash = passwordEncoder.encode(password);
        appUserRepository.save(new AppUser(normalizedEmail, passwordHash, Role.ADMIN));
        log.info("Admin account created for {}", normalizedEmail);
    }
}