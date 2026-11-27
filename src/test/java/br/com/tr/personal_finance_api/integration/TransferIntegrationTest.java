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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TransferIntegrationTest extends BaseIntegrationTest {

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
    private User attacker;

    private Account sourceAccount;
    private Account destinationAccount;

    private String ownerJwt;
    private String attackerJwt;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM transfer_idempotency");
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        userRepository.deleteAll();

        owner = createUser("owner");
        attacker = createUser("attacker");

        sourceAccount = createAccount(
                "TRANSFER-SOURCE",
                new BigDecimal("1000.00"),
                owner
        );

        destinationAccount = createAccount(
                "TRANSFER-DESTINATION",
                new BigDecimal("200.00"),
                owner
        );

        ownerJwt = login("owner@email.com");
        attackerJwt = login("attacker@email.com");
    }

    @Test
    @DisplayName("Flyway deve criar a tabela de idempotência")
    void shouldApplyTransferIdempotencyMigration() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfer_idempotency",
                Long.class
        );

        assertNotNull(count);
        assertEquals(0L, count);
    }

    @Test
    @DisplayName("Transferência deve atualizar saldos e registrar débito e crédito")
    void shouldTransferAmountAndPersistTransactions() throws Exception {
        performTransfer(
                ownerJwt,
                "100.00",
                "transfer-success"
        ).andExpect(status().isOk());

        assertBalance(sourceAccount, "900.00");
        assertBalance(destinationAccount, "300.00");

        assertEquals(2L, transactionRepository.count());
        assertEquals(1L, countIdempotencyRecords());
    }

    @Test
    @DisplayName("Repetir a mesma chave não deve executar a transferência novamente")
    void shouldNotDuplicateTransferWhenIdempotencyKeyIsReplayed()
            throws Exception {

        performTransfer(
                ownerJwt,
                "100.00",
                "transfer-replay"
        ).andExpect(status().isOk());

        performTransfer(
                ownerJwt,
                "100.00",
                "transfer-replay"
        ).andExpect(status().isOk());

        assertBalance(sourceAccount, "900.00");
        assertBalance(destinationAccount, "300.00");

        assertEquals(2L, transactionRepository.count());
        assertEquals(1L, countIdempotencyRecords());
    }

    @Test
    @DisplayName("Reutilizar chave com payload diferente deve retornar HTTP 400")
    void shouldRejectDifferentPayloadWithSameIdempotencyKey()
            throws Exception {

        performTransfer(
                ownerJwt,
                "100.00",
                "transfer-conflict"
        ).andExpect(status().isOk());

        performTransfer(
                ownerJwt,
                "200.00",
                "transfer-conflict"
        ).andExpect(status().isBadRequest());

        assertBalance(sourceAccount, "900.00");
        assertBalance(destinationAccount, "300.00");

        assertEquals(2L, transactionRepository.count());
        assertEquals(1L, countIdempotencyRecords());
    }

    @Test
    @DisplayName("Usuário não pode transferir de conta pertencente a outro usuário")
    void shouldRejectTransferFromAnotherUsersAccount()
            throws Exception {

        performTransfer(
                attackerJwt,
                "100.00",
                "transfer-idor"
        ).andExpect(status().isNotFound());

        assertBalance(sourceAccount, "1000.00");
        assertBalance(destinationAccount, "200.00");

        assertEquals(0L, transactionRepository.count());
        assertEquals(0L, countIdempotencyRecords());
    }

    @Test
    @DisplayName("Endpoint de transferência deve exigir autenticação")
    void shouldRejectUnauthenticatedTransfer() throws Exception {
        performTransfer(
                null,
                "100.00",
                "transfer-unauthenticated"
        ).andExpect(status().isUnauthorized());

        assertBalance(sourceAccount, "1000.00");
        assertBalance(destinationAccount, "200.00");

        assertEquals(0L, transactionRepository.count());
        assertEquals(0L, countIdempotencyRecords());
    }

    private User createUser(String username) {
        return userRepository.save(
                User.builder()
                        .fullName(username)
                        .email(username + "@email.com")
                        .passwordHash(passwordEncoder.encode("123456"))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );
    }

    private Account createAccount(
            String accountNumber,
            BigDecimal balance,
            User user
    ) {
        return accountRepository.save(
                Account.builder()
                        .accountNumber(accountNumber)
                        .balance(balance)
                        .currency("BRL")
                        .status(AccountStatus.ACTIVE)
                        .user(user)
                        .build()
        );
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(
                        post("/api/auth/login")
                                .with(request -> {
                                    request.setRemoteAddr(
                                            "198.51.100." + ThreadLocalRandom.current()
                                                    .nextInt(1, 255)
                                    );
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new LoginRequest(email, "123456")
                                ))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthResponse.class).token();
    }

    private org.springframework.test.web.servlet.ResultActions performTransfer(
            String jwt,
            String amount,
            String idempotencyKey
    ) throws Exception {

        var request = post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of(
                                "fromAccount", sourceAccount.getAccountNumber(),
                                "toAccount", destinationAccount.getAccountNumber(),
                                "amount", amount,
                                "idempotencyKey", idempotencyKey
                        )
                ));

        if (jwt != null) {
            request.header("Authorization", "Bearer " + jwt);
        }

        return mockMvc.perform(request);
    }

    private void assertBalance(Account account, String expectedBalance) {
        Account updated = accountRepository.findById(account.getId())
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal(expectedBalance).compareTo(updated.getBalance())
        );
    }

    private Long countIdempotencyRecords() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfer_idempotency",
                Long.class
        );
    }
}