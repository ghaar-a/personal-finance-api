package br.com.tr.personal_finance_api.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank
        String fromAccount,

        @NotBlank
        String toAccount,

        @NotNull
        BigDecimal amount,

        String idempotencyKey

) {}