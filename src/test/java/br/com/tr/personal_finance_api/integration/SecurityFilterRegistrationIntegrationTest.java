package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.security.CorrelationIdFilter;
import br.com.tr.personal_finance_api.security.CsrfAttackLoggingFilter;
import br.com.tr.personal_finance_api.security.JwtAuthFilter;
import br.com.tr.personal_finance_api.security.RateLimitFilter;
import br.com.tr.personal_finance_api.security.SecurityHeadersFilter;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityFilterRegistrationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    @DisplayName("Filtros não devem ser registrados automaticamente no servlet container")
    void shouldDisableServletContainerRegistrationForSecurityFilters() {
        assertRegistrationDisabled("correlationIdFilterRegistration");
        assertRegistrationDisabled("securityHeadersFilterRegistration");
        assertRegistrationDisabled("rateLimitFilterRegistration");
        assertRegistrationDisabled("jwtAuthFilterRegistration");
        assertRegistrationDisabled("csrfAttackLoggingFilterRegistration");
    }

    @Test
    @DisplayName("Filtros comuns devem estar na SecurityFilterChain em ordem determinística")
    void shouldRegisterCommonFiltersInExpectedOrder() {
        List<Filter> filters = securityFilterChain.getFilters();

        int correlationIdIndex = filterIndex(filters, CorrelationIdFilter.class);
        int securityHeadersIndex = filterIndex(filters, SecurityHeadersFilter.class);
        int rateLimitIndex = filterIndex(filters, RateLimitFilter.class);
        int jwtAuthIndex = filterIndex(filters, JwtAuthFilter.class);

        assertTrue(correlationIdIndex >= 0, "CorrelationIdFilter não encontrado");
        assertTrue(securityHeadersIndex >= 0, "SecurityHeadersFilter não encontrado");
        assertTrue(rateLimitIndex >= 0, "RateLimitFilter não encontrado");
        assertTrue(jwtAuthIndex >= 0, "JwtAuthFilter não encontrado");

        assertTrue(
                correlationIdIndex < securityHeadersIndex,
                "CorrelationIdFilter deve executar antes de SecurityHeadersFilter"
        );

        assertTrue(
                securityHeadersIndex < rateLimitIndex,
                "SecurityHeadersFilter deve executar antes de RateLimitFilter"
        );

        assertTrue(
                rateLimitIndex < jwtAuthIndex,
                "RateLimitFilter deve executar antes de JwtAuthFilter"
        );

        // BaseIntegrationTest configura o modo SECURE.
        assertEquals(
                -1,
                filterIndex(filters, CsrfAttackLoggingFilter.class),
                "CsrfAttackLoggingFilter não deve integrar a cadeia no modo SECURE"
        );
    }

    private void assertRegistrationDisabled(String beanName) {
        FilterRegistrationBean<?> registration =
                applicationContext.getBean(
                        beanName,
                        FilterRegistrationBean.class
                );

        assertFalse(
                registration.isEnabled(),
                beanName + " não deveria estar habilitado no servlet container"
        );
    }

    private int filterIndex(
            List<Filter> filters,
            Class<? extends Filter> filterType
    ) {
        for (int i = 0; i < filters.size(); i++) {
            if (filterType.isInstance(filters.get(i))) {
                return i;
            }
        }

        return -1;
    }
}