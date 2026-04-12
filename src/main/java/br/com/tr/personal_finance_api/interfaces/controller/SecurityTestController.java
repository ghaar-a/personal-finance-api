package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.security.mode.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/security-test")
@RequiredArgsConstructor
public class SecurityTestController {

    private final VulnerableAccountService vulnerableService;
    private final SecureAccountService secureService;

    @Value("${app.security-mode}")
    private SecurityMode mode;

    @GetMapping
    public List<Map<String, Object>> test(@RequestParam String accountNumber) {

        if (mode == SecurityMode.VULNERABLE) { // alterna entre os modos (SECURE) para SQL injection
            return vulnerableService.findAccountByNumber(accountNumber);
        }

        return secureService.findAccountByNumber(accountNumber);
    }
}