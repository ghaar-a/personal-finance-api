package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.domain.repository.AccountRepository; // 1. Import adicionado
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthLoggingTest extends BaseIntegrationTest {

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
                        .fullName("Test User")
                        .email("logging@email.com")
                        .passwordHash(passwordEncoder.encode("123456"))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );
    }

    @Test
    @DisplayName("Login com credenciais válidas deve retornar 200 e token JWT")
    void shouldLoginSuccessfully() throws Exception {
        String loginJson = """
                {
                    "email": "logging@email.com",
                    "password": "123456"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.101");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}