
package br.com.tr.personal_finance_api.application.service;

import br.com.tr.personal_finance_api.application.command.TransferCommand;
import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.entity.AccountStatus;
import br.com.tr.personal_finance_api.domain.entity.Transaction;
import br.com.tr.personal_finance_api.domain.entity.TransactionType;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.domain.repository.TransactionRepository;
import br.com.tr.personal_finance_api.domain.repository.TransferIdempotencyRepository;
import br.com.tr.personal_finance_api.domain.repository.TransferIdempotencyRepository.ReservationResult;
import br.com.tr.personal_finance_api.interfaces.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger log =
            LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferIdempotencyRepository transferIdempotencyRepository;

    @Transactional
    public void transfer(TransferCommand command) {

        validateCommand(command);

        if (command.fromAccount().equals(command.toAccount())) {
            throw new IllegalArgumentException(
                    "As contas de origem e destino devem ser diferentes."
            );
        }

        log.info(
                "TRANSFER_START | from={} to={} amount={} userId={}",
                command.fromAccount(),
                command.toAccount(),
                command.amount(),
                command.userId()
        );

        Map<String, Account> accounts = lockAccounts(command);

        Account source = accounts.get(command.fromAccount());
        Account destination = accounts.get(command.toAccount());

        validateOwnership(source, command);
        validateCurrency(source, destination);

        if (command.idempotencyKey() != null) {
            ReservationResult reservationResult =
                    transferIdempotencyRepository.reserveOrValidate(
                            command.userId(),
                            command.idempotencyKey(),
                            source.getId(),
                            destination.getId(),
                            command.amount(),
                            source.getCurrency()
                    );

            if (reservationResult == ReservationResult.PAYLOAD_CONFLICT) {
                throw new IllegalArgumentException(
                        "A chave de idempotência já foi utilizada com dados diferentes."
                );
            }

            if (reservationResult == ReservationResult.IDEMPOTENT_REPLAY) {
                log.info(
                        "TRANSFER_IDEMPOTENT_REPLAY | userId={} idempotencyKey={}",
                        command.userId(),
                        command.idempotencyKey()
                );
                return;
            }
        }

        validateAccountStatus(source, destination);
        validateBalance(source, command.amount());

        executeTransfer(source, destination, command.amount());
        saveTransactions(source, destination, command.amount());

        log.info(
                "TRANSFER_SUCCESS | from={} to={} amount={} userId={}",
                command.fromAccount(),
                command.toAccount(),
                command.amount(),
                command.userId()
        );
    }

    private void validateCommand(TransferCommand command) {

        if (command == null
                || command.userId() == null
                || command.fromAccount() == null
                || command.fromAccount().isBlank()
                || command.toAccount() == null
                || command.toAccount().isBlank()
                || command.amount() == null) {

            throw new IllegalArgumentException(
                    "Os dados da transferência são inválidos."
            );
        }

        BigDecimal amount = command.amount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {

            throw new IllegalArgumentException(
                    "O valor da transferência deve ser positivo e possuir no máximo duas casas decimais."
            );
        }

        if (command.idempotencyKey() != null
                && (command.idempotencyKey().isBlank()
                || command.idempotencyKey().length() > 100)) {

            throw new IllegalArgumentException(
                    "A chave de idempotência deve possuir entre 1 e 100 caracteres."
            );
        }
    }

    private Map<String, Account> lockAccounts(TransferCommand command) {

        List<Account> lockedAccounts =
                accountRepository.findAllByAccountNumbersForUpdate(
                        List.of(command.fromAccount(), command.toAccount())
                );

        Map<String, Account> accountsByNumber = lockedAccounts.stream()
                .collect(Collectors.toMap(
                        Account::getAccountNumber,
                        Function.identity()
                ));

        if (!accountsByNumber.containsKey(command.fromAccount())
                || !accountsByNumber.containsKey(command.toAccount())) {

            throw new ResourceNotFoundException(
                    "Conta de origem ou destino não encontrada."
            );
        }

        return accountsByNumber;
    }

    private void validateOwnership(
            Account source,
            TransferCommand command
    ) {
        if (source.getUser() == null
                || !source.getUser().getId().equals(command.userId())) {

            throw new ResourceNotFoundException(
                    "Conta de origem ou destino não encontrada."
            );
        }
    }

    private void validateCurrency(Account source, Account destination) {

        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new IllegalArgumentException(
                    "As contas devem possuir a mesma moeda."
            );
        }
    }

    private void validateAccountStatus(
            Account source,
            Account destination
    ) {
        if (source.getStatus() != AccountStatus.ACTIVE
                || destination.getStatus() != AccountStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "As duas contas devem estar ativas para realizar a transferência."
            );
        }
    }

    private void validateBalance(Account source, BigDecimal amount) {

        if (source.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente.");
        }
    }

    private void executeTransfer(
            Account source,
            Account destination,
            BigDecimal amount
    ) {
        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        // As entidades estão gerenciadas pelo contexto JPA.
        // O dirty checking persiste as alterações no commit da transação.
    }

    private void saveTransactions(
            Account source,
            Account destination,
            BigDecimal amount
    ) {
        Transaction debit = Transaction.builder()
                .account(source)
                .amount(amount)
                .transactionType(TransactionType.DEBIT)
                .description("Transferência enviada")
                .build();

        Transaction credit = Transaction.builder()
                .account(destination)
                .amount(amount)
                .transactionType(TransactionType.CREDIT)
                .description("Transferência recebida")
                .build();

        transactionRepository.save(debit);
        transactionRepository.save(credit);
    }
}
