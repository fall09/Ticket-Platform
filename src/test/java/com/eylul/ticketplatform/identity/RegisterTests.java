package com.eylul.ticketplatform.identity;

import com.eylul.ticketplatform.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegisterTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void registersNewUserWithRoleUser() {
        // given: ...
        // when: ...
        // then: ...
    }
}