
package br.com.tr.personal_finance_api.interfaces.dto;

import br.com.tr.personal_finance_api.domain.entity.AccountStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String currency,
        AccountStatus status
) {
}
