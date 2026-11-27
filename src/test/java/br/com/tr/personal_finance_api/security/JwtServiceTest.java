
package br.com.tr.personal_finance_api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String VALID_SECRET =
            "personal-finance-api-test-secret-256-bits";

    @Test
    @DisplayName("Deve gerar e validar token JWT")
    void shouldGenerateAndExtractUserId() {
        JwtService jwtService = createService(VALID_SECRET, 3600000L);
        jwtService.init();

        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, "user@example.com");

        assertEquals(userId, jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Deve rejeitar segredo JWT nulo")
    void shouldRejectNullSecret() {
        JwtService jwtService = createService(null, 3600000L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                jwtService::init
        );

        assertTrue(exception.getMessage().contains("null or blank"));
    }

    @Test
    @DisplayName("Deve rejeitar segredo JWT menor que 32 bytes")
    void shouldRejectSecretShorterThan32Bytes() {
        JwtService jwtService = createService("short-secret", 3600000L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                jwtService::init
        );

        assertTrue(exception.getMessage().contains("32 bytes"));
    }

    @Test
    @DisplayName("Deve rejeitar segredo JWT vazio")
    void shouldRejectBlankSecret() {
        JwtService jwtService = createService("   ", 3600000L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                jwtService::init
        );

        assertTrue(exception.getMessage().contains("blank"));
    }

    @Test
    @DisplayName("Deve rejeitar expiração JWT menor ou igual a zero")
    void shouldRejectNonPositiveExpiration() {
        JwtService jwtService = createService(VALID_SECRET, 0L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                jwtService::init
        );

        assertTrue(exception.getMessage().contains("greater than zero"));
    }

    private JwtService createService(String secret, long expiration) {
        JwtService jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", expiration);

        return jwtService;
    }
}
