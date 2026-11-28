package br.com.tr.personal_finance_api.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountSchemaConstraintsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @BeforeEach
    void createUser() {
        userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, full_name, email, password_hash) VALUES (?, ?, ?, ?)",
                userId, "Schema Test", userId + "@test.local", "hash"
        );
    }

    @AfterEach
    void removeData() {
        jdbcTemplate.update("DELETE FROM accounts WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    @DisplayName("Banco deve aceitar conta com balance, currency e status preenchidos")
    void shouldAcceptAccountWithAllRequiredColumns() {
        int inserted = jdbcTemplate.update(
                "INSERT INTO accounts (id, user_id, account_number, balance, currency, status) "
                        + "VALUES (?, ?, ?, 0.00, 'BRL', 'ACTIVE')",
                UUID.randomUUID(), userId, accountNumber()
        );

        assertThat(inserted).isEqualTo(1);
    }

    @Test
    @DisplayName("Banco deve rejeitar conta com balance nulo")
    void shouldRejectNullBalance() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO accounts (id, user_id, account_number, balance, currency, status) "
                        + "VALUES (?, ?, ?, NULL, 'BRL', 'ACTIVE')",
                UUID.randomUUID(), userId, accountNumber()
        ))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("not-null constraint")
                .hasMessageContaining("balance");
    }

    @Test
    @DisplayName("Banco deve rejeitar conta com currency nula")
    void shouldRejectNullCurrency() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO accounts (id, user_id, account_number, balance, currency, status) "
                        + "VALUES (?, ?, ?, 0.00, NULL, 'ACTIVE')",
                UUID.randomUUID(), userId, accountNumber()
        ))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("not-null constraint")
                .hasMessageContaining("currency");
    }

    @Test
    @DisplayName("Banco deve rejeitar conta com status nulo")
    void shouldRejectNullStatus() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO accounts (id, user_id, account_number, balance, currency, status) "
                        + "VALUES (?, ?, ?, 0.00, 'BRL', NULL)",
                UUID.randomUUID(), userId, accountNumber()
        ))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("not-null constraint")
                .hasMessageContaining("status");
    }

    private static String accountNumber() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 19);
    }
}