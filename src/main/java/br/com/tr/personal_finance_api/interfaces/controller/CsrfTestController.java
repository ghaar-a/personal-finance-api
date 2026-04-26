package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/csrf")
@RequiredArgsConstructor
public class CsrfTestController {

    // Simulação de operação sensível
    // NÃO valida origem → alvo de CSRF

    @PostMapping("/transfer")
    public String transfer(
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return "Transferência simulada realizada pelo usuário: "
                + user.getUserId()
                + " no valor de "
                + amount;
    }
}