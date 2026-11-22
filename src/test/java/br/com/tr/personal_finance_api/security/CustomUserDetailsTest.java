package br.com.tr.personal_finance_api.security;

import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    void shouldBeEnabledWhenUserIsEnabled() {
        CustomUserDetails userDetails = new CustomUserDetails(createUser(true));

        assertTrue(userDetails.isEnabled());
    }

    @Test
    void shouldBeDisabledWhenUserIsDisabled() {
        CustomUserDetails userDetails = new CustomUserDetails(createUser(false));

        assertFalse(userDetails.isEnabled());
    }

    @Test
    void shouldBeDisabledWhenUserEnabledStateIsNull() {
        CustomUserDetails userDetails = new CustomUserDetails(createUser(null));

        assertFalse(userDetails.isEnabled());
    }

    private User createUser(Boolean enabled) {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("user@example.com")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .enabled(enabled)
                .build();
    }
}