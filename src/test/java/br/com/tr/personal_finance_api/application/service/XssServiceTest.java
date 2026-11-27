
package br.com.tr.personal_finance_api.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class XssServiceTest {

    private final XssService xssService = new XssService();

    @Test
    @DisplayName("Deve aceitar entrada nula na sanitização de reflexão")
    void shouldReturnNullWhenReflectSecureReceivesNull() {
        assertNull(xssService.reflectSecure(null));
    }

    @Test
    @DisplayName("Deve aceitar entrada nula na sanitização de armazenamento")
    void shouldReturnNullWhenStoreSecureReceivesNull() {
        assertNull(xssService.storeSecure(null));
    }
}
