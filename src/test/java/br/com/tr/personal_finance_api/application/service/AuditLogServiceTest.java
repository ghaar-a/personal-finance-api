
package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.AuditLog;
import br.com.tr.personal_finance_api.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void shouldPersistFailedLoginEvent() {
        auditLogService.logFailedLogin("user@example.com", "192.0.2.1");

        AuditLog auditLog = captureSavedAuditLog();

        assertEquals("AUTH", auditLog.getEntityName());
        assertNull(auditLog.getEntityId());
        assertEquals("FAILED_LOGIN", auditLog.getAction());
        assertEquals("user@example.com", auditLog.getOldValue());
        assertEquals("192.0.2.1", auditLog.getNewValue());
        assertNotNull(auditLog.getChangedAt());
    }

    @Test
    void shouldPersistBlockedLoginEvent() {
        auditLogService.logBlockedLogin("user@example.com", "192.0.2.2");

        AuditLog auditLog = captureSavedAuditLog();

        assertEquals("AUTH", auditLog.getEntityName());
        assertNull(auditLog.getEntityId());
        assertEquals("BLOCKED_LOGIN", auditLog.getAction());
        assertEquals("user@example.com", auditLog.getOldValue());
        assertEquals("192.0.2.2", auditLog.getNewValue());
        assertNotNull(auditLog.getChangedAt());
    }

    @Test
    void shouldPersistUnauthorizedAccessEvent() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        auditLogService.logUnauthorizedAccess(userId, resourceId);

        AuditLog auditLog = captureSavedAuditLog();

        assertEquals("ACCOUNT", auditLog.getEntityName());
        assertEquals(resourceId, auditLog.getEntityId());
        assertEquals("UNAUTHORIZED_ACCESS", auditLog.getAction());
        assertEquals(userId, auditLog.getChangedBy());
        assertNotNull(auditLog.getChangedAt());
    }

    @Test
    void shouldPersistCsrfAttemptEvent() {
        auditLogService.logCsrfAttempt(
                "192.0.2.3",
                "/api/csrf/transfer",
                "POST"
        );

        AuditLog auditLog = captureSavedAuditLog();

        assertEquals("SECURITY", auditLog.getEntityName());
        assertNull(auditLog.getEntityId());
        assertEquals("CSRF_ATTEMPT", auditLog.getAction());
        assertEquals("192.0.2.3", auditLog.getOldValue());
        assertEquals(
                "/api/csrf/transfer | POST",
                auditLog.getNewValue()
        );
        assertNotNull(auditLog.getChangedAt());
    }

    @Test
    void shouldNotPropagateFailedLoginPersistenceException() {
        simulatePersistenceFailure();

        assertDoesNotThrow(() ->
                auditLogService.logFailedLogin(
                        "user@example.com",
                        "192.0.2.1"
                )
        );
    }

    @Test
    void shouldNotPropagateBlockedLoginPersistenceException() {
        simulatePersistenceFailure();

        assertDoesNotThrow(() ->
                auditLogService.logBlockedLogin(
                        "user@example.com",
                        "192.0.2.1"
                )
        );
    }

    @Test
    void shouldNotPropagateUnauthorizedAccessPersistenceException() {
        simulatePersistenceFailure();

        assertDoesNotThrow(() ->
                auditLogService.logUnauthorizedAccess(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void shouldNotPropagateCsrfAttemptPersistenceException() {
        simulatePersistenceFailure();

        assertDoesNotThrow(() ->
                auditLogService.logCsrfAttempt(
                        "192.0.2.1",
                        "/api/csrf/transfer",
                        "POST"
                )
        );
    }

    private AuditLog captureSavedAuditLog() {
        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(auditLogRepository).save(captor.capture());

        return captor.getValue();
    }

    private void simulatePersistenceFailure() {
        doThrow(new RuntimeException("Database unavailable"))
                .when(auditLogRepository)
                .save(any(AuditLog.class));
    }
}
