package br.com.tr.personal_finance_api.interfaces.mapper;

import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.entity.AccountStatus;
import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.interfaces.dto.AccountResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountMapperTest {

    private AccountMapper accountMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        accountMapper = new AccountMapperImpl();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldMapOnlyExplicitlyAllowedAccountFields() {
        UUID accountId = UUID.randomUUID();

        User owner = User.builder()
                .id(UUID.randomUUID())
                .fullName("Account Owner")
                .email("owner@example.com")
                .passwordHash("sensitive-password-hash")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .accountNumber("ACC-100")
                .balance(new BigDecimal("1500.75"))
                .currency("BRL")
                .status(AccountStatus.ACTIVE)
                .user(owner)
                .createdAt(OffsetDateTime.now())
                .build();

        AccountResponse response = accountMapper.toResponse(account);

        assertEquals(accountId, response.id());
        assertEquals("ACC-100", response.accountNumber());
        assertEquals(new BigDecimal("1500.75"), response.balance());
        assertEquals("BRL", response.currency());
        assertEquals(AccountStatus.ACTIVE, response.status());
    }

    @Test
    void shouldNotExposeInternalAccountOrUserFieldsInJson() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .fullName("Account Owner")
                .email("owner@example.com")
                .passwordHash("sensitive-password-hash")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC-100")
                .balance(new BigDecimal("1500.75"))
                .currency("BRL")
                .status(AccountStatus.ACTIVE)
                .user(owner)
                .createdAt(OffsetDateTime.now())
                .build();

        AccountResponse response = accountMapper.toResponse(account);
        JsonNode json = objectMapper.valueToTree(response);

        assertTrue(json.has("id"));
        assertTrue(json.has("accountNumber"));
        assertTrue(json.has("balance"));
        assertTrue(json.has("currency"));
        assertTrue(json.has("status"));

        assertFalse(json.has("user"));
        assertFalse(json.has("createdAt"));
        assertFalse(json.has("passwordHash"));
        assertFalse(json.has("email"));
        assertFalse(json.has("role"));
    }
}