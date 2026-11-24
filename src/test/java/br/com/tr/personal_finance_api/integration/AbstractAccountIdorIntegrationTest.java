
package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.entity.AccountStatus;
import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import br.com.tr.personal_finance_api.interfaces.dto.AuthResponse;
import br.com.tr.personal_finance_api.interfaces.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class AbstractAccountIdorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected ObjectMapper objectMapper;

    protected UUID accountId;
    protected String jwt;

    @BeforeEach
    void setupAccountIdorScenario() throws Exception {

        accountRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(
                User.builder()
                        .fullName("Owner")
                        .email("owner@email.com")
                        .passwordHash(passwordEncoder.encode("123456"))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );

        userRepository.save(
                User.builder()
                        .fullName("Attacker")
                        .email("attacker@email.com")
                        .passwordHash(passwordEncoder.encode("123456"))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );

        Account account = accountRepository.save(
                Account.builder()
                        .accountNumber("ACC-100")
                        .balance(new BigDecimal("1000"))
                        .currency("BRL")
                        .status(AccountStatus.ACTIVE)
                        .user(owner)
                        .build()
        );

        accountId = account.getId();

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new LoginRequest(
                                                "attacker@email.com",
                                                "123456"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();

        AuthResponse authResponse =
                objectMapper.readValue(response, AuthResponse.class);

        if (authResponse.token() == null || authResponse.token().isBlank()) {
            throw new IllegalStateException(
                    "Login succeeded but did not return a valid JWT."
            );
        }

        jwt = authResponse.token();
    }
}
