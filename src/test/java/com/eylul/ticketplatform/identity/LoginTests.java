package com.eylul.ticketplatform.identity;


import com.eylul.ticketplatform.TestcontainersConfiguration;
import com.eylul.ticketplatform.identity.internal.user.AppUser;
import com.eylul.ticketplatform.identity.internal.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabaseAndAddNewUser() {
        appUserRepository.deleteAll();
        String email = "test.user@example.com";
        String password = "Passw0rd!x";
        String passwordHash = passwordEncoder.encode(password);
        AppUser newUser = new AppUser(email, passwordHash, Role.USER);
        appUserRepository.save(newUser);
    }

    @Test
    void givenCorrectCredentials_whenLogin_thenReturn200WithToken() throws Exception {
        String body = """
                {
                  "email": "test.user@example.com",
                  "password": "Passw0rd!x"
                }
                """;
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

}
