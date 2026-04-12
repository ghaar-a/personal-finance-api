package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.domain.entity.*;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.domain.repository.TransactionRepository;
import br.com.tr.personal_finance_api.interfaces.dto.TransferRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    // LOGGER (fica sempre logo após a declaração da classe)
    private static final Logger log =
            LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void transfer(TransferRequest request) {

        // LOG 1 — início da operação
        log.info("Transferência iniciada | from={} to={} amount={} idempotencyKey={}",
                request.fromAccount(),
                request.toAccount(),
                request.amount(),
                request.idempotencyKey());

        // 1. Verificar idempotência
        if (request.idempotencyKey() != null) {

            transactionRepository
                    .findByIdempotencyKey(request.idempotencyKey())
                    .ifPresent(t -> {

                        log.warn("Tentativa de duplicação detectada | idempotencyKey={}",
                                request.idempotencyKey());

                        throw new RuntimeException("Transferência já processada");
                    });
        }

        // 2. Buscar contas
        Account from = accountRepository
                .findByAccountNumber(request.fromAccount())
                .orElseThrow(() -> {

                    log.error("Conta origem não encontrada | account={}",
                            request.fromAccount());

                    return new RuntimeException("Conta origem não encontrada");
                });

        Account to = accountRepository
                .findByAccountNumber(request.toAccount())
                .orElseThrow(() -> {

                    log.error("Conta destino não encontrada | account={}",
                            request.toAccount());

                    return new RuntimeException("Conta destino não encontrada");
                });

        BigDecimal amount = request.amount();

        // 3. Validar saldo
        if (from.getBalance().compareTo(amount) < 0) {

            log.warn("Saldo insuficiente | account={} balance={} attemptedAmount={}",
                    from.getAccountNumber(),
                    from.getBalance(),
                    amount);

            throw new RuntimeException("Saldo insuficiente");
        }

        // 4. Atualizar saldos
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);

        // 5. Registrar débito
        Transaction debit = Transaction.builder()
                .account(from)
                .amount(amount)
                .transactionType(TransactionType.DEBIT)
                .description("Transferência enviada")
                .idempotencyKey(request.idempotencyKey())
                .build();

        // 6. Registrar crédito
        Transaction credit = Transaction.builder()
                .account(to)
                .amount(amount)
                .transactionType(TransactionType.CREDIT)
                .description("Transferência recebida")
                .idempotencyKey(request.idempotencyKey())
                .build();

        transactionRepository.save(debit);
        transactionRepository.save(credit);

        // LOG FINAL — operação concluída
        log.info("Transferência concluída | from={} to={} amount={}",
                request.fromAccount(),
                request.toAccount(),
                amount);
    }
}