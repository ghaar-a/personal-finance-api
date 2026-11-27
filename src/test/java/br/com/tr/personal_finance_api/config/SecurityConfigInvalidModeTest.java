
package br.com.tr.personal_finance_api.config;

import br.com.tr.personal_finance_api.interfaces.exception.SecurityExceptionHandler;
import br.com.tr.personal_finance_api.security.CorrelationIdFilter;
import br.com.tr.personal_finance_api.security.CsrfAttackLoggingFilter;
import br.com.tr.personal_finance_api.security.JwtAuthFilter;
import br.com.tr.personal_finance_api.security.RateLimitFilter;
import br.com.tr.personal_finance_api.security.SecurityHeadersFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;

class SecurityConfigInvalidModeTest {

    @Test
    void shouldFailWhenSecurityModeIsUnsupported() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(SecurityExceptionHandler.class),
                mock(CorrelationIdFilter.class),
                mock(SecurityHeadersFilter.class),
                mock(RateLimitFilter.class),
                mock(JwtAuthFilter.class),
                mock(CsrfAttackLoggingFilter.class)
        );

        ReflectionTestUtils.setField(
                securityConfig,
                "securityMode",
                "INVALID"
        );

        HttpSecurity http = mock(HttpSecurity.class, RETURNS_SELF);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> securityConfig.securityFilterChain(http)
        );

        assertEquals(
                "Unsupported app.security-mode: INVALID. Expected VULNERABLE or SECURE.",
                exception.getMessage()
        );
    }
}
