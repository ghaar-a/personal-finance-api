package br.com.tr.personal_finance_api.interfaces.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetails> handleRuntime(RuntimeException ex) {

        ProblemDetails problem = new ProblemDetails(
                "about:blank",
                "Business Error",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                OffsetDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problem);
    }
}