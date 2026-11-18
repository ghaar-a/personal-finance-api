package br.com.tr.personal_finance_api.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String idempotencyKey,
        UUID userId
) {
}