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

    private static final Logger log =
            LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void transfer(TransferRequest request) {

        log.info("TRANSFER_START | from={} to={} amount={} idempotencyKey={}",
                request.fromAccount(),
                request.toAccount(),
                request.amount(),
                request.idempotencyKey());

        validateIdempotency(request);

        Account from = getAccountOrThrow(request.fromAccount(), "origem");
        Account to = getAccountOrThrow(request.toAccount(), "destino");

        BigDecimal amount = request.amount();

        validateBalance(from, amount);

        executeTransfer(from, to, amount);

        saveTransactions(from, to, amount, request.idempotencyKey());

        log.info("TRANSFER_SUCCESS | from={} to={} amount={}",
                request.fromAccount(),
                request.toAccount(),
                amount);
    }

    private void validateIdempotency(TransferRequest request) {
        if (request.idempotencyKey() != null) {
            transactionRepository
                    .findByIdempotencyKey(request.idempotencyKey())
                    .ifPresent(t -> {
                        log.warn("TRANSFER_DUPLICATE | idempotencyKey={}",
                                request.idempotencyKey());
                        throw new RuntimeException("Transferência já processada");
                    });
        }
    }

    private Account getAccountOrThrow(String accountNumber, String type) {
        return accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.error("ACCOUNT_NOT_FOUND | type={} account={}",
                            type, accountNumber);
                    return new RuntimeException("Conta " + type + " não encontrada");
                });
    }

    private void validateBalance(Account from, BigDecimal amount) {
        if (from.getBalance().compareTo(amount) < 0) {
            log.warn("INSUFFICIENT_BALANCE | account={} balance={} attempted={}",
                    from.getAccountNumber(),
                    from.getBalance(),
                    amount);
            throw new RuntimeException("Saldo insuficiente");
        }
    }

    private void executeTransfer(Account from, Account to, BigDecimal amount) {
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);
    }

    private void saveTransactions(Account from, Account to, BigDecimal amount, String idempotencyKey) {

        Transaction debit = Transaction.builder()
                .account(from)
                .amount(amount)
                .transactionType(TransactionType.DEBIT)
                .description("Transferência enviada")
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction credit = Transaction.builder()
                .account(to)
                .amount(amount)
                .transactionType(TransactionType.CREDIT)
                .description("Transferência recebida")
                .idempotencyKey(idempotencyKey)
                .build();

        transactionRepository.save(debit);
        transactionRepository.save(credit);
    }
}