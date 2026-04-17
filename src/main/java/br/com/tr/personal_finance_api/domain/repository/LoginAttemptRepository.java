package br.com.tr.personal_finance_api.domain.repository;

import br.com.tr.personal_finance_api.domain.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    // Busca tentativas recentes por email
    List<LoginAttempt> findByEmailAndAttemptTimeAfter(String email, OffsetDateTime time);

    // Contagem direta (mais eficiente)
    long countByEmailAndSuccessFalseAndAttemptTimeAfter(String email, OffsetDateTime time);
}