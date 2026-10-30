package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.LoginAttemptRepository;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AuthBruteForceTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        loginAttemptRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(
                User.builder()
                        .fullName("Brute Force Target")
                        .email("bruteforce@email.com")
                        .passwordHash(passwordEncoder.encode("correctPassword"))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );
    }

    @Test
    @DisplayName("Deve retornar 401 nas primeiras 10 tentativas e 429 na 11ª por força bruta")
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        String loginJson = """
                {
                    "email": "bruteforce@email.com",
                    "password": "wrongPassword123"
                }
                """;

        for (int i = 1; i <= 10; i++) {
            String clientIp = "192.0.2." + i;

            mockMvc.perform(post("/api/auth/login")
                            .with(request -> {
                                request.setRemoteAddr(clientIp);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.11");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isTooManyRequests());
    }
}