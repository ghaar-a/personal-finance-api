package br.com.tr.personal_finance_api.domain.repository;

import br.com.tr.personal_finance_api.domain.entity.AuditLog;
import br.com.tr.personal_finance_api.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    }
}