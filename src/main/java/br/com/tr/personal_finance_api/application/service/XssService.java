
package br.com.tr.personal_finance_api.application.service;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

@Service
public class XssService {

    private static final PolicyFactory SANITIZER =
            Sanitizers.FORMATTING
                    .and(Sanitizers.BLOCKS)
                    .and(Sanitizers.LINKS);

    public String reflectVulnerable(String input) {
        return input;
    }

    public String reflectSecure(String input) {
        return sanitize(input);
    }

    public String storeVulnerable(String input) {
        return input;
    }

    public String storeSecure(String input) {
        return sanitize(input);
    }

    private String sanitize(String input) {
        if (input == null) {
            return null;
        }

        return SANITIZER.sanitize(input);
    }
}
