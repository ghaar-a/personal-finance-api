package br.com.tr.personal_finance_api.domain.repository;

import br.com.tr.personal_finance_api.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    // IDOR
    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    // NECESSÁRIO PARA TRANSFERÊNCIA
    Optional<Account> findByAccountNumber(String accountNumber);
}