
package br.com.tr.personal_finance_api.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserLifecycleTest {

    @Test
    void shouldUpdateTimestampBeforeEntityUpdate() {
        User user = new User();

        OffsetDateTime previousTimestamp =
                OffsetDateTime.parse("2000-01-01T00:00:00Z");

        user.setUpdatedAt(previousTimestamp);

        user.preUpdate();

        assertNotNull(user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(previousTimestamp));
    }
}
