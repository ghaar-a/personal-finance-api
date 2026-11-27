
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
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = "app.security-mode=VULNERABLE")
class SecurityFilterVulnerableModeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    @DisplayName("Modo vulnerável deve incluir o filtro de auditoria CSRF")
    void shouldRegisterCsrfAttackLoggingFilter() {
        List<Filter> filters = securityFilterChain.getFilters();

        assertTrue(
                filterIndex(filters, CsrfAttackLoggingFilter.class) >= 0,
                "CsrfAttackLoggingFilter deve estar presente no modo VULNERABLE"
        );
    }

    @Test
    @DisplayName("Filtros devem respeitar a ordem definida no modo vulnerável")
    void shouldRegisterFiltersInExpectedOrder() {
        List<Filter> filters = securityFilterChain.getFilters();

        int correlationIdIndex =
                filterIndex(filters, CorrelationIdFilter.class);

        int securityHeadersIndex =
                filterIndex(filters, SecurityHeadersFilter.class);

        int csrfLoggingIndex =
                filterIndex(filters, CsrfAttackLoggingFilter.class);

        int rateLimitIndex =
                filterIndex(filters, RateLimitFilter.class);

        int jwtAuthIndex =
                filterIndex(filters, JwtAuthFilter.class);

        assertTrue(correlationIdIndex >= 0);
        assertTrue(securityHeadersIndex >= 0);
        assertTrue(csrfLoggingIndex >= 0);
        assertTrue(rateLimitIndex >= 0);
        assertTrue(jwtAuthIndex >= 0);

        assertTrue(correlationIdIndex < securityHeadersIndex);
        assertTrue(securityHeadersIndex < csrfLoggingIndex);
        assertTrue(csrfLoggingIndex < rateLimitIndex);
        assertTrue(rateLimitIndex < jwtAuthIndex);
    }

    @Test
    @DisplayName("Filtros não devem ser registrados automaticamente no servlet container")
    void shouldDisableServletContainerRegistration() {
        assertRegistrationDisabled("correlationIdFilterRegistration");
        assertRegistrationDisabled("securityHeadersFilterRegistration");
        assertRegistrationDisabled("rateLimitFilterRegistration");
        assertRegistrationDisabled("jwtAuthFilterRegistration");
        assertRegistrationDisabled("csrfAttackLoggingFilterRegistration");
    }

    private void assertRegistrationDisabled(String beanName) {
        FilterRegistrationBean<?> registration =
                applicationContext.getBean(
                        beanName,
                        FilterRegistrationBean.class
                );

        assertFalse(
                registration.isEnabled(),
                beanName + " não deveria estar habilitado"
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
