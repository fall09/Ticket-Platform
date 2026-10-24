package com.eylul.ticketplatform.identity.internal.admin;

import com.eylul.ticketplatform.TestcontainersConfiguration;
import com.eylul.ticketplatform.identity.Role;
import com.eylul.ticketplatform.identity.internal.user.AppUser;
import com.eylul.ticketplatform.identity.internal.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AdminInitializerTests {

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        appUserRepository.deleteAll();
    }

    @Test
    void givenSettingsAndEmptyDatabase_whenRun_thenAdminIsCreated() {
        AdminProperties properties = new AdminProperties("  Admin@Example.com  ", "Passw0rd!x");
        AdminInitializer initializer = new AdminInitializer(properties, appUserRepository, passwordEncoder);

        initializer.run(null);

        AppUser admin = appUserRepository.findByEmail("admin@example.com").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches("Passw0rd!x", admin.getPasswordHash())).isTrue();
    }

    private void runWith(String email, String password) {
        AdminProperties properties = new AdminProperties(email, password);
        new AdminInitializer(properties, appUserRepository, passwordEncoder).run(null);
    }

    @Test
    void givenSettingsAndAdminCreatedBefore_whenRunTwice_thenStillOneAdmin() {
        runWith("admin@example.com", "Passw0rd!x");
        runWith("admin@example.com", "Passw0rd!x");

        assertThat(appUserRepository.count()).isEqualTo(1);
    }

    @Test
    void givenExistingAdmin_whenRunWithOtherSettings_thenNothingChanges() {
        appUserRepository.save(new AppUser("first.admin@example.com", "hash", Role.ADMIN));

        runWith("second.admin@example.com", "Passw0rd!x");

        assertThat(appUserRepository.count()).isEqualTo(1);
        assertThat(appUserRepository.findByEmail("second.admin@example.com")).isEmpty();
    }

    @Test
    void givenNullEmail_whenRun_thenNoAdminIsCreated() {
        runWith(null, "Passw0rd!x");

        assertThat(appUserRepository.count()).isZero();
    }

    @Test
    void givenBlankEmail_whenRun_thenNoAdminIsCreated() {
        runWith("   ", "Passw0rd!x");

        assertThat(appUserRepository.count()).isZero();
    }

    @Test
    void givenNullPassword_whenRun_thenNoAdminIsCreated() {
        runWith("admin@example.com", null);

        assertThat(appUserRepository.count()).isZero();
    }

    @Test
    void givenBlankPassword_whenRun_thenNoAdminIsCreated() {
        runWith("admin@example.com", "   ");

        assertThat(appUserRepository.count()).isZero();
    }
}