package br.com.tr.personal_finance_api.interfaces.controller;

import br.com.tr.personal_finance_api.application.service.XssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/xss")
@RequiredArgsConstructor
public class XssTestController {

    private final XssService xssService;

    // REFLECTED XSS

    @GetMapping("/reflect/vulnerable")
    public String reflectVulnerable(@RequestParam String input) {
        return xssService.reflectVulnerable(input);
    }

    @GetMapping("/reflect/secure")
    public String reflectSecure(@RequestParam String input) {
        return xssService.reflectSecure(input);
    }

    // STORED XSS (simulado)

    @PostMapping("/store/vulnerable")
    public String storeVulnerable(@RequestBody String input) {
        return xssService.storeVulnerable(input);
    }

    @PostMapping("/store/secure")
    public String storeSecure(@RequestBody String input) {
        return xssService.storeSecure(input);
    }
}