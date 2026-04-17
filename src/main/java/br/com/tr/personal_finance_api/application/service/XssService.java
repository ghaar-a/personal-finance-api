package br.com.tr.personal_finance_api.application.service;

import org.springframework.stereotype.Service;

@Service
public class XssService {

    // VULNERÁVEL (refletido)
    public String reflectVulnerable(String input) {
        return input;
    }

    // SEGURO (refletido)
    public String reflectSecure(String input) {
        return sanitize(input);
    }

    // STORED (simulação simples)
    public String storeVulnerable(String input) {
        return input;
    }

    public String storeSecure(String input) {
        return sanitize(input);
    }

    // SANITIZAÇÃO BÁSICA (produção usaria lib como OWASP Java HTML Sanitizer)
    private String sanitize(String input) {
        if (input == null) return null;

        return input
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;");
    }
}