package br.com.tr.personal_finance_api.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank
        String fromAccount,

        @NotBlank
        String toAccount,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @Size(max = 100)
        @Pattern(regexp = ".*\\S.*")
        String idempotencyKey
) {
}