package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    public Account getAccountVulnerable(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
    }

    public Account getAccountSecure(UUID accountId, UUID userId, HttpServletRequest request) {

        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseGet(() -> {

                    auditLogService.logUnauthorizedAccess(userId, accountId);

                    throw new RuntimeException("Conta não encontrada ou acesso negado");
                });
    }
}