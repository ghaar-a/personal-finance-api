package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.entity.AccountStatus;
import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.domain.repository.LoginAttemptRepository;
import br.com.tr.personal_finance_api.domain.repository.TransactionRepository;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import br.com.tr.personal_finance_api.interfaces.dto.AuthResponse;
import br.com.tr.personal_finance_api.interfaces.dto.LoginRequest;
import br.com.tr.personal_finance_api.security.CsrfDemoCookie;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security-mode=VULNERABLE")
class CsrfTransferIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private User owner;
    private Account sourceAccount;
    private Account destinationAccount;

    private String ownerJwt;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM transfer_idempotency");
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(
                User.builder()
                        .fullName("CSRF Owner")
                        .email("csrf-owner@email.com")
                        .passwordHash(passwordEncoder.encode("123456"))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );

        sourceAccount = createAccount(
                "CSRF-SOURCE",
                new BigDecimal("1000.00")
        );

        destinationAccount = createAccount(
                "CSRF-DESTINATION",
                new BigDecimal("200.00")
        );

        ownerJwt = login();
    }

    @Test
    @DisplayName("Login no modo vulnerável deve emitir cookie JWT com atributos de segurança")
    void shouldIssueSecureHttpOnlyCookieInVulnerableMode()
            throws Exception {

        var result = mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .header("Origin", "https://localhost")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new LoginRequest(
                                                "csrf-owner@email.com",
                                                "123456"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = result.getResponse()
                .getHeader("Set-Cookie");

        assertNotNull(setCookie);
        assertTrue(setCookie.contains(CsrfDemoCookie.NAME + "="));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=None"));
        assertTrue(setCookie.contains("Path=/api/csrf"));
    }

    @Test
    @DisplayName("Cookie JWT deve permitir transferência sem token CSRF no endpoint demonstrativo")
    void shouldExecuteTransferUsingCookieWithoutCsrfToken()
            throws Exception {

        mockMvc.perform(
                        post("/api/csrf/transfer")
                                .cookie(new Cookie(
                                        CsrfDemoCookie.NAME,
                                        ownerJwt
                                ))
                                .header("Origin", "https://attacker.example")
                                .header(
                                        "Referer",
                                        "https://attacker.example/csrf.html"
                                )
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                                .param(
                                        "fromAccount",
                                        sourceAccount.getAccountNumber()
                                )
                                .param(
                                        "toAccount",
                                        destinationAccount.getAccountNumber()
                                )
                                .param("amount", "100.00")
                                .param("idempotencyKey", "csrf-demo-transfer")
                )
                .andExpect(status().isOk());

        assertBalance(sourceAccount, "900.00");
        assertBalance(destinationAccount, "300.00");

        assertEquals(2L, transactionRepository.count());
    }

    @Test
    @DisplayName("Endpoint demonstrativo deve rejeitar requisição sem cookie")
    void shouldRejectTransferWithoutCookie() throws Exception {

        mockMvc.perform(
                        post("/api/csrf/transfer")
                                .contentType(
                                        MediaType.APPLICATION_FORM_URLENCODED
                                )
                                .param(
                                        "fromAccount",
                                        sourceAccount.getAccountNumber()
                                )
                                .param(
                                        "toAccount",
                                        destinationAccount.getAccountNumber()
                                )
                                .param("amount", "100.00")
                )
                .andExpect(status().isUnauthorized());

        assertBalance(sourceAccount, "1000.00");
        assertBalance(destinationAccount, "200.00");

        assertEquals(0L, transactionRepository.count());
    }

    @Test
    @DisplayName("Cookie demonstrativo não deve autenticar outros endpoints")
    void shouldNotUseDemoCookieOutsideDemoEndpoint() throws Exception {

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "fromAccount", sourceAccount.getAccountNumber(),
                        "toAccount", destinationAccount.getAccountNumber(),
                        "amount", "100.00",
                        "idempotencyKey", "cookie-outside-demo"
                )
        );

        mockMvc.perform(
                        post("/api/transfers")
                                .with(csrf())
                                .cookie(new Cookie(
                                        CsrfDemoCookie.NAME,
                                        ownerJwt
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized());

        assertBalance(sourceAccount, "1000.00");
        assertBalance(destinationAccount, "200.00");

        assertEquals(0L, transactionRepository.count());
    }

    private String login() throws Exception {
        String response = mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .header("Origin", "https://localhost")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new LoginRequest(
                                                "csrf-owner@email.com",
                                                "123456"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(
                response,
                AuthResponse.class
        ).token();
    }

    private Account createAccount(
            String accountNumber,
            BigDecimal balance
    ) {
        return accountRepository.save(
                Account.builder()
                        .accountNumber(accountNumber)
                        .balance(balance)
                        .currency("BRL")
                        .status(AccountStatus.ACTIVE)
                        .user(owner)
                        .build()
        );
    }

    private void assertBalance(
            Account account,
            String expectedBalance
    ) {
        Account updated = accountRepository.findById(account.getId())
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal(expectedBalance)
                        .compareTo(updated.getBalance())
        );
    }
}