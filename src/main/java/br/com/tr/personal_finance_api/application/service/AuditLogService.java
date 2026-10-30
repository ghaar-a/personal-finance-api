package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.AuditLog;
import br.com.tr.personal_finance_api.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    // =========================
    // FAILED LOGIN
    // =========================
    public void logFailedLogin(String email, String ip) {
        try {
            log.warn("SECURITY_EVENT failed_login email={} ip={}", email, ip);

            AuditLog auditLog = AuditLog.builder()
                    .entityName("AUTH")
                    .entityId(null)
                    .action("FAILED_LOGIN")
                    .oldValue(email)
                    .newValue(ip)
                    .changedAt(OffsetDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Erro ao salvar audit log (FAILED_LOGIN)", e);
        }
    }

    // =========================
    // BLOCKED LOGIN
    // =========================
    public void logBlockedLogin(String email, String ip) {
        try {
            log.error("SECURITY_EVENT blocked_login email={} ip={}", email, ip);

            AuditLog auditLog = AuditLog.builder()
                    .entityName("AUTH")
                    .entityId(null)
                    .action("BLOCKED_LOGIN")
                    .oldValue(email)
                    .newValue(ip)
                    .changedAt(OffsetDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Erro ao salvar audit log (BLOCKED_LOGIN)", e);
        }
    }

    // =========================
    // UNAUTHORIZED ACCESS
    // =========================
    public void logUnauthorizedAccess(UUID userId, UUID resourceId) {
        try {
            log.warn("SECURITY_EVENT unauthorized_access userId={} resourceId={}", userId, resourceId);

            AuditLog auditLog = AuditLog.builder()
                    .entityName("ACCOUNT")
                    .entityId(resourceId)
                    .action("UNAUTHORIZED_ACCESS")
                    .changedBy(userId)
                    .changedAt(OffsetDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Erro ao salvar audit log (UNAUTHORIZED_ACCESS)", e);
        }
    }

    // =========================
    // CSRF ATTEMPT
    // =========================
    public void logCsrfAttempt(String ip, String path, String method) {
        try {
            log.warn("SECURITY_EVENT csrf_attempt ip={} path={} method={}", ip, path, method);

            AuditLog auditLog = AuditLog.builder()
                    .entityName("SECURITY")
                    .entityId(null)
                    .action("CSRF_ATTEMPT")
                    .oldValue(ip)
                    .newValue(path + " | " + method)
                    .changedAt(OffsetDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Erro ao salvar audit log (CSRF_ATTEMPT)", e);
        }
    }
}