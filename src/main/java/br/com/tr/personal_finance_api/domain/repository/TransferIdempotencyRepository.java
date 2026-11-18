
package br.com.tr.personal_finance_api.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TransferIdempotencyRepository {

    private final JdbcTemplate jdbcTemplate;

    public enum ReservationResult {
        NEW_REQUEST,
        IDEMPOTENT_REPLAY,
        PAYLOAD_CONFLICT
    }

    /**
     * Reserves a new idempotency key or checks an existing reservation.
     *
     * Returns NEW_REQUEST when the key is newly reserved.
     * Returns IDEMPOTENT_REPLAY when the existing payload is identical.
     * Returns PAYLOAD_CONFLICT when the key was used with a different payload.
     */
    public ReservationResult reserveOrValidate(
            UUID userId,
            String idempotencyKey,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency
    ) {
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO transfer_idempotency (
                    user_id,
                    idempotency_key,
                    source_account_id,
                    destination_account_id,
                    amount,
                    currency
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, idempotency_key) DO NOTHING
                """,
                userId,
                idempotencyKey,
                sourceAccountId,
                destinationAccountId,
                amount,
                currency
        );

        if (inserted == 1) {
            return ReservationResult.NEW_REQUEST;
        }

        TransferSnapshot existing = jdbcTemplate.queryForObject(
                """
                SELECT
                    source_account_id,
                    destination_account_id,
                    amount,
                    currency
                FROM transfer_idempotency
                WHERE user_id = ?
                  AND idempotency_key = ?
                """,
                (resultSet, rowNumber) -> mapSnapshot(resultSet),
                userId,
                idempotencyKey
        );

        if (existing == null
                || !existing.sourceAccountId().equals(sourceAccountId)
                || !existing.destinationAccountId().equals(destinationAccountId)
                || existing.amount().compareTo(amount) != 0
                || !existing.currency().equals(currency)) {

            return ReservationResult.PAYLOAD_CONFLICT;
        }

        return ReservationResult.IDEMPOTENT_REPLAY;
    }

    private TransferSnapshot mapSnapshot(ResultSet resultSet)
            throws SQLException {

        return new TransferSnapshot(
                resultSet.getObject("source_account_id", UUID.class),
                resultSet.getObject("destination_account_id", UUID.class),
                resultSet.getBigDecimal("amount"),
                resultSet.getString("currency")
        );
    }

    private record TransferSnapshot(
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency
    ) {
    }
}
