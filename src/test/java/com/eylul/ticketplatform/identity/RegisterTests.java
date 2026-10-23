package com.eylul.ticketplatform.identity;

import com.eylul.ticketplatform.TestcontainersConfiguration;
import com.eylul.ticketplatform.identity.internal.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegisterTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;


    @BeforeEach
    void cleanDatabase() {
        appUserRepository.deleteAll();
    }

    @Test
    void registersNewUserWithRoleUser() throws Exception {
        // given
        String body = """
                {
                  "email": "test.user@example.com",
                  "password": "Passw0rd!x"
                }
                """;

        // when + then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void givenExistingEmail_whenRegister_thenReturn409() throws Exception {
        // given: aynı e-posta ile bir kullanıcı zaten kayıtlı
        String body = """
                {
                  "email": "test.user@example.com",
                  "password": "Passw0rd!x"
                }
                """;
        // 1. ilk kayıt: perform(...) + status 201 beklentisi
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        // when + then: aynı body ile ikinci kayıt
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("E-mail already registered"));

    }

    @Test
    @DisplayName("Same e-mail in different letter case returns 409")
    void givenExistingEmailInLowerCase_whenRegisterWithUpperCase_thenReturn409() throws Exception {
        // given: lower-case e-mail is registered
        String body = """
                {
                  "email": "test.user@example.com",
                  "password": "Passw0rd!x"
                }
                """;
        //when + then: same e-mail in upper case
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        String upperCaseBody = """
                {
                  "email": "TEST.USER@EXAMPLE.COM",
                  "password": "Passw0rd!x"
                }
                """;
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(upperCaseBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("E-mail already registered"));
    }
}
