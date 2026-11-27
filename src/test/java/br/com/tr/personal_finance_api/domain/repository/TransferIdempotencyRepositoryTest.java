
package br.com.tr.personal_finance_api.domain.repository;

import br.com.tr.personal_finance_api.domain.repository.TransferIdempotencyRepository.ReservationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TransferIdempotencyRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private TransferIdempotencyRepository repository;

    private UUID userId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private static final String IDEMPOTENCY_KEY = "transfer-test-123";
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");
    private static final String CURRENCY = "BRL";

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        repository = new TransferIdempotencyRepository(jdbcTemplate);

        userId = UUID.randomUUID();
        sourceAccountId = UUID.randomUUID();
        destinationAccountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve reservar uma nova chave de idempotência")
    void shouldReserveNewIdempotencyKey() {
        when(jdbcTemplate.update(
                anyString(), any(), any(), any(), any(), any(), any()
        )).thenReturn(1);

        ReservationResult result = reserve();

        assertEquals(ReservationResult.NEW_REQUEST, result);

        verify(jdbcTemplate, never()).queryForObject(
                anyString(), any(RowMapper.class), any(), any()
        );
    }

    @Test
    @DisplayName("Deve reconhecer uma requisição idempotente")
    void shouldRecognizeIdenticalExistingPayload() throws Exception {
        stubExistingSnapshot(
                sourceAccountId,
                destinationAccountId,
                AMOUNT,
                CURRENCY
        );

        ReservationResult result = reserve();

        assertEquals(ReservationResult.IDEMPOTENT_REPLAY, result);
    }

    @Test
    @DisplayName("Deve rejeitar payload com conta de origem diferente")
    void shouldRejectDifferentSourceAccount() throws Exception {
        stubExistingSnapshot(
                UUID.randomUUID(),
                destinationAccountId,
                AMOUNT,
                CURRENCY
        );

        assertEquals(ReservationResult.PAYLOAD_CONFLICT, reserve());
    }

    @Test
    @DisplayName("Deve rejeitar payload com conta de destino diferente")
    void shouldRejectDifferentDestinationAccount() throws Exception {
        stubExistingSnapshot(
                sourceAccountId,
                UUID.randomUUID(),
                AMOUNT,
                CURRENCY
        );

        assertEquals(ReservationResult.PAYLOAD_CONFLICT, reserve());
    }

    @Test
    @DisplayName("Deve rejeitar payload com valor diferente")
    void shouldRejectDifferentAmount() throws Exception {
        stubExistingSnapshot(
                sourceAccountId,
                destinationAccountId,
                new BigDecimal("300.00"),
                CURRENCY
        );

        assertEquals(ReservationResult.PAYLOAD_CONFLICT, reserve());
    }

    @Test
    @DisplayName("Deve rejeitar payload com moeda diferente")
    void shouldRejectDifferentCurrency() throws Exception {
        stubExistingSnapshot(
                sourceAccountId,
                destinationAccountId,
                AMOUNT,
                "USD"
        );

        assertEquals(ReservationResult.PAYLOAD_CONFLICT, reserve());
    }

    @Test
    @DisplayName("Deve rejeitar reserva existente sem snapshot")
    void shouldRejectMissingExistingSnapshot() {
        when(jdbcTemplate.update(
                anyString(), any(), any(), any(), any(), any(), any()
        )).thenReturn(0);

        when(jdbcTemplate.queryForObject(
                anyString(), any(RowMapper.class), any(), any()
        )).thenReturn(null);

        assertEquals(ReservationResult.PAYLOAD_CONFLICT, reserve());
    }

    private ReservationResult reserve() {
        return repository.reserveOrValidate(
                userId,
                IDEMPOTENCY_KEY,
                sourceAccountId,
                destinationAccountId,
                AMOUNT,
                CURRENCY
        );
    }

    private void stubExistingSnapshot(
            UUID existingSourceAccountId,
            UUID existingDestinationAccountId,
            BigDecimal existingAmount,
            String existingCurrency
    ) throws Exception {
        when(jdbcTemplate.update(
                anyString(), any(), any(), any(), any(), any(), any()
        )).thenReturn(0);

        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.getObject("source_account_id", UUID.class))
                .thenReturn(existingSourceAccountId);
        when(resultSet.getObject("destination_account_id", UUID.class))
                .thenReturn(existingDestinationAccountId);
        when(resultSet.getBigDecimal("amount"))
                .thenReturn(existingAmount);
        when(resultSet.getString("currency"))
                .thenReturn(existingCurrency);

        when(jdbcTemplate.queryForObject(
                anyString(), any(RowMapper.class), any(), any()
        )).thenAnswer(invocation -> {
            RowMapper<?> rowMapper = invocation.getArgument(1);
            return rowMapper.mapRow(resultSet, 0);
        });
    }
}
