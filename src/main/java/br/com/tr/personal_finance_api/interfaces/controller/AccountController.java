
package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.AccountService;
import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.interfaces.dto.AccountResponse;
import br.com.tr.personal_finance_api.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/vulnerable/{id}")
    public AccountResponse getAccountVulnerable(@PathVariable UUID id) {
        Account account = accountService.getAccountVulnerable(id);
        return AccountResponse.from(account);
    }

    @GetMapping("/secure/{id}")
    public AccountResponse getAccountSecure(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        Account account = accountService.getAccountSecure(
                id,
                userDetails.getUserId(),
                request
        );

        return AccountResponse.from(account);
    }
}
