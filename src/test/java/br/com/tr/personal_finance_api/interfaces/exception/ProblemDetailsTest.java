
package br.com.tr.personal_finance_api.interfaces.exception;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProblemDetailsTest {

    @Test
    void shouldExposeAllRecordComponents() {
        OffsetDateTime timestamp = OffsetDateTime.parse(
                "2026-11-27T10:00:00-03:00"
        );

        ProblemDetails problem = new ProblemDetails(
                "about:blank",
                "Not Found",
                404,
                "Conta não encontrada",
                timestamp
        );

        assertEquals("about:blank", problem.type());
        assertEquals("Not Found", problem.title());
        assertEquals(404, problem.status());
        assertEquals("Conta não encontrada", problem.detail());
        assertEquals(timestamp, problem.timestamp());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        OffsetDateTime timestamp = OffsetDateTime.parse(
                "2026-11-27T10:00:00-03:00"
        );

        ProblemDetails first = new ProblemDetails(
                "about:blank",
                "Not Found",
                404,
                "Conta não encontrada",
                timestamp
        );

        ProblemDetails second = new ProblemDetails(
                "about:blank",
                "Not Found",
                404,
                "Conta não encontrada",
                timestamp
        );

        ProblemDetails different = new ProblemDetails(
                "about:blank",
                "Forbidden",
                403,
                "Acesso negado",
                timestamp
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
        assertNotEquals(first, null);
        assertNotEquals(first, new Object());
    }

    @Test
    void shouldGenerateStringRepresentation() {
        OffsetDateTime timestamp = OffsetDateTime.parse(
                "2026-11-27T10:00:00-03:00"
        );

        ProblemDetails problem = new ProblemDetails(
                "about:blank",
                "Not Found",
                404,
                "Conta não encontrada",
                timestamp
        );

        String representation = problem.toString();

        assertTrue(representation.startsWith("ProblemDetails["));
        assertTrue(representation.contains("type=about:blank"));
        assertTrue(representation.contains("title=Not Found"));
        assertTrue(representation.contains("status=404"));
        assertTrue(representation.contains("detail=Conta não encontrada"));
        assertTrue(representation.contains("timestamp=" + timestamp));
    }
}
