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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AccountIdorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID accountId;
    private String jwt;

    @BeforeEach
    void setup() throws Exception {

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

        User attacker = userRepository.save(
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

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("attacker@email.com", "123456")
                        )))
                .andReturn()
                .getResponse()
                .getContentAsString();

        jwt = objectMapper.readValue(response, AuthResponse.class).token();
    }

    @Test
    @DisplayName("Endpoint seguro deve impedir acesso à conta de outro usuário")
    void secureEndpointShouldBlockIdor() throws Exception {

        mockMvc.perform(get("/api/accounts/secure/" + accountId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Endpoint vulnerável demonstra o comportamento IDOR para estudos")
    void vulnerableEndpointShouldExposeIdorScenario() throws Exception {

        mockMvc.perform(get("/api/accounts/vulnerable/" + accountId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }
}