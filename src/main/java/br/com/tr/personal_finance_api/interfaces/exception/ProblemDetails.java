package br.com.tr.personal_finance_api.interfaces.exception;

import java.time.OffsetDateTime;

public record ProblemDetails(

        String type,
        String title,
        int status,
        String detail,
        OffsetDateTime timestamp

) {}