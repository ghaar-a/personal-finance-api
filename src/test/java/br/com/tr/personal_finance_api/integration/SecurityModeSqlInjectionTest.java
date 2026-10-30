package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.entity.AccountStatus;
import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityModeSqlInjectionTest extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void securityMode(DynamicPropertyRegistry registry) {
        registry.add("app.security-mode", () -> "SECURE");
    }

    @BeforeEach
    void setup() {

        accountRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(
                User.builder()
                        .fullName("Security Tester")
                        .email("security@test.com")
                        .passwordHash(passwordEncoder.encode("123456"))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );

        accountRepository.save(
                Account.builder()
                        .accountNumber("ACC-001")
                        .balance(BigDecimal.valueOf(1000))
                        .currency("BRL")
                        .status(AccountStatus.ACTIVE)
                        .user(user)
                        .build()
        );
    }

    @Test
    @DisplayName("Endpoint SQL Injection deve exigir autenticação")
    void shouldRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/security-test")
                        .param("accountNumber", "' OR '1'='1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}