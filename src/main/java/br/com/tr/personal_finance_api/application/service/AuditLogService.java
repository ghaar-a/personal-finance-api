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

    public void logUnauthorizedAccess(UUID userId, UUID resourceId) {
        log.warn("SECURITY_EVENT unauthorized_access userId={} resourceId={}", userId, resourceId);

        AuditLog auditLog = AuditLog.builder()
                .entityName("ACCOUNT")
                .entityId(resourceId)
                .action("UNAUTHORIZED_ACCESS")
                .changedBy(userId)
                .changedAt(OffsetDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    public void logFailedLogin(String email, String ip) {
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
    }

    public void logBlockedLogin(String email, String ip) {
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
    }
}