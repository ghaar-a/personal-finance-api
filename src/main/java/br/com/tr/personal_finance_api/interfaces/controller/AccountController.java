package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.AccountService;
import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // Endpoint vulnerável
    @GetMapping("/vulnerable/{id}")
    public Account getAccountVulnerable(@PathVariable UUID id) {
        return accountService.getAccountVulnerable(id);
    }

    // Endpoint seguro + auditoria
    @GetMapping("/secure/{id}")
    public Account getAccountSecure(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        return accountService.getAccountSecure(
                id,
                userDetails.getUserId(),
                request
        );
    }
}