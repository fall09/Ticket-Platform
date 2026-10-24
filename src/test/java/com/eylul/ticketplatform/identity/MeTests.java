package com.eylul.ticketplatform.identity;

import com.eylul.ticketplatform.TestcontainersConfiguration;
import com.eylul.ticketplatform.identity.internal.user.AppUser;
import com.eylul.ticketplatform.identity.internal.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import com.eylul.ticketplatform.identity.internal.security.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MeTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        appUserRepository.deleteAll();
    }


    @Test
    @DisplayName("Protected endpoint without a token returns 401 (OWASP A01)")
    void givenNoToken_whenGetMe_thenReturn401 () throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected endpoint with a malformed token returns 401 (OWASP A01)")
    void givenMalformedToken_whenGetMe_thenReturn401 () throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenValidToken_whenGetMe_thenReturn200WithUserInfo() throws Exception {
        AppUser user = appUserRepository.save(
                new AppUser("test.user@example.com", "hash", Role.USER));
        String token = jwtService.createToken(user);

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
