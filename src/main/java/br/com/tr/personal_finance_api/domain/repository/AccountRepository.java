package br.com.tr.personal_finance_api.domain.repository;

import br.com.tr.personal_finance_api.domain.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account
            FROM Account account
            WHERE account.accountNumber IN :accountNumbers
            ORDER BY account.accountNumber
            """)
    List<Account> findAllByAccountNumbersForUpdate(
            @Param("accountNumbers") List<String> accountNumbers
    );
}