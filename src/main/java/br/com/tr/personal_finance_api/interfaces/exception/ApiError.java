package br.com.tr.personal_finance_api.interfaces.exception;

import java.time.OffsetDateTime;

public class ApiError {

    private final String code;
    private final String message;
    private final int status;
    private final String path;
    private final String correlationId;
    private final OffsetDateTime timestamp;

    public ApiError(String code, String message, int status, String path, String correlationId) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.path = path;
        this.correlationId = correlationId;
        this.timestamp = OffsetDateTime.now();
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public int getStatus() { return status; }
    public String getPath() { return path; }
    public String getCorrelationId() { return correlationId; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}