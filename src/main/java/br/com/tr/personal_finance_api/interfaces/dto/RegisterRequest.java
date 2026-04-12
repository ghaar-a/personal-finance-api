package br.com.tr.personal_finance_api.interfaces.dto;

public record RegisterRequest(
        String fullName,
        String email,
        String password
) {}