package br.com.tr.personal_finance_api.integration;

import br.com.tr.personal_finance_api.application.command.TransferCommand;
import br.com.tr.personal_finance_api.application.service.TransferService;
import br.com.tr.personal_finance_api.domain.entity.Account;
import br.com.tr.personal_finance_api.domain.entity.AccountStatus;
import br.com.tr.personal_finance_api.domain.entity.Role;
import br.com.tr.personal_finance_api.domain.entity.Transaction;
import br.com.tr.personal_finance_api.domain.entity.User;
import br.com.tr.personal_finance_api.domain.repository.AccountRepository;
import br.com.tr.personal_finance_api.domain.repository.TransactionRepository;
import br.com.tr.personal_finance_api.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferTransactionIntegrityIntegrationTest extends BaseIntegrationTest {

    private static final BigDecimal INITIAL_SOURCE_BALANCE =
            new BigDecimal("1000.00");

    private static final BigDecimal INITIAL_DESTINATION_BALANCE =
            new BigDecimal("200.00");

    private static final String FAILURE_SEQUENCE =
            "test_transfer_transaction_insert_seq";

    private static final String FAILURE_FUNCTION =
            "test_fail_on_second_transaction_insert";

    private static final String FAILURE_TRIGGER =
            "test_fail_on_second_transaction_insert";

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;

    private Account sourceAccount;

    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        dropFailureTrigger();

        jdbcTemplate.update("DELETE FROM transfer_idempotency");
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        owner = createUser("transfer-integrity-owner");

        sourceAccount = createAccount(
                "INT-SOURCE",
                INITIAL_SOURCE_BALANCE,
                owner
        );

        destinationAccount = createAccount(
                "INT-DEST",
                INITIAL_DESTINATION_BALANCE,
                owner
        );
    }

    @AfterEach
    void tearDown() {
        dropFailureTrigger();
    }

    @Test
    @DisplayName(
            "Falha no segundo lançamento deve reverter toda a transferência"
    )
    void shouldRollbackJpaAndJdbcOperationsWhenSecondTransactionFails() {

        installFailureTrigger();

        TransferCommand command = command(
                sourceAccount,
                destinationAccount,
                "100.00",
                "rollback-integrity-key"
        );

        assertThrows(
                RuntimeException.class,
                () -> transferService.transfer(command)
        );

        assertEquals(
                2L,
                jdbcTemplate.queryForObject(
                        "SELECT last_value FROM " + FAILURE_SEQUENCE,
                        Long.class
                ),
                "O trigger deveria ter sido executado nos dois lançamentos"
        );

        assertBalance(sourceAccount, "1000.00");
        assertBalance(destinationAccount, "200.00");

        assertEquals(
                0L,
                transactionRepository.count(),
                "O primeiro lançamento também deve ser revertido"
        );

        assertEquals(
                0L,
                countIdempotencyRecords(),
                "A reserva JDBC da chave também deve ser revertida"
        );
    }

    @Test
    @DisplayName(
            "Duas transferências concorrentes com a mesma chave devem executar apenas uma vez"
    )
    void shouldExecuteOnlyOneTransferForConcurrentRequestsWithSameKey()
            throws Exception {

        TransferCommand firstCommand = command(
                sourceAccount,
                destinationAccount,
                "100.00",
                "concurrent-same-key"
        );

        TransferCommand secondCommand = command(
                sourceAccount,
                destinationAccount,
                "100.00",
                "concurrent-same-key"
        );

        List<TransferOutcome> outcomes = executeConcurrently(
                firstCommand,
                secondCommand
        );

        assertEquals(
                List.of(TransferOutcome.SUCCESS, TransferOutcome.SUCCESS),
                outcomes
        );

        assertBalance(sourceAccount, "900.00");
        assertBalance(destinationAccount, "300.00");

        assertEquals(2L, transactionRepository.count());
        assertEquals(1L, countIdempotencyRecords());
    }

    @Test
    @DisplayName(
            "Bloqueio pessimista deve impedir saldo negativo em transferências concorrentes"
    )
    void shouldPreventOverdraftDuringConcurrentTransfers()
            throws Exception {

        sourceAccount.setBalance(new BigDecimal("100.00"));
        destinationAccount.setBalance(BigDecimal.ZERO);

        sourceAccount = accountRepository.save(sourceAccount);
        destinationAccount = accountRepository.save(destinationAccount);

        TransferCommand firstCommand = command(
                sourceAccount,
                destinationAccount,
                "80.00",
                "concurrent-overdraft-1"
        );

        TransferCommand secondCommand = command(
                sourceAccount,
                destinationAccount,
                "80.00",
                "concurrent-overdraft-2"
        );

        List<TransferOutcome> outcomes = executeConcurrently(
                firstCommand,
                secondCommand
        );

        assertEquals(
                1L,
                outcomes.stream()
                        .filter(outcome -> outcome == TransferOutcome.SUCCESS)
                        .count()
        );

        assertEquals(
                1L,
                outcomes.stream()
                        .filter(outcome -> outcome == TransferOutcome.REJECTED)
                        .count()
        );

        assertBalance(sourceAccount, "20.00");
        assertBalance(destinationAccount, "80.00");

        assertEquals(2L, transactionRepository.count());

        assertEquals(
                1L,
                countIdempotencyRecords(),
                "A reserva da operação rejeitada deve ser revertida"
        );
    }

    @Test
    @DisplayName(
            "Transferências simultâneas em sentidos opostos devem evitar deadlock"
    )
    void shouldCompleteOppositeDirectionTransfersWithoutDeadlock()
            throws Exception {

        TransferCommand forwardCommand = command(
                sourceAccount,
                destinationAccount,
                "100.00",
                "opposite-direction-forward"
        );

        TransferCommand reverseCommand = command(
                destinationAccount,
                sourceAccount,
                "100.00",
                "opposite-direction-reverse"
        );

        List<TransferOutcome> outcomes = executeConcurrently(
                forwardCommand,
                reverseCommand
        );

        assertEquals(
                List.of(TransferOutcome.SUCCESS, TransferOutcome.SUCCESS),
                outcomes
        );

        assertBalance(sourceAccount, "1000.00");
        assertBalance(destinationAccount, "200.00");

        assertEquals(4L, transactionRepository.count());
        assertEquals(2L, countIdempotencyRecords());

        BigDecimal totalBalance = currentBalance(sourceAccount)
                .add(currentBalance(destinationAccount));

        assertEquals(
                0,
                new BigDecimal("1200.00").compareTo(totalBalance),
                "O total dos saldos deve permanecer constante"
        );
    }

    private void installFailureTrigger() {
        jdbcTemplate.execute(
                "CREATE SEQUENCE " + FAILURE_SEQUENCE
                        + " START WITH 1 INCREMENT BY 1"
        );

        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION test_fail_on_second_transaction_insert()
                RETURNS TRIGGER
                LANGUAGE plpgsql
                AS $$
                BEGIN
                    IF nextval('test_transfer_transaction_insert_seq') = 2 THEN
                        RAISE EXCEPTION
                            'Simulated failure on second transaction insert';
                    END IF;

                    RETURN NEW;
                END;
                $$
                """
        );

        jdbcTemplate.execute("""
                CREATE TRIGGER test_fail_on_second_transaction_insert
                BEFORE INSERT ON transactions
                FOR EACH ROW
                EXECUTE FUNCTION test_fail_on_second_transaction_insert()
                """
        );
    }

    private void dropFailureTrigger() {
        jdbcTemplate.execute(
                "DROP TRIGGER IF EXISTS " + FAILURE_TRIGGER
                        + " ON transactions"
        );

        jdbcTemplate.execute(
                "DROP FUNCTION IF EXISTS " + FAILURE_FUNCTION + "()"
        );

        jdbcTemplate.execute(
                "DROP SEQUENCE IF EXISTS " + FAILURE_SEQUENCE
        );
    }

    private List<TransferOutcome> executeConcurrently(
            TransferCommand firstCommand,
            TransferCommand secondCommand
    ) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<TransferOutcome> first = executor.submit(
                    transferTask(firstCommand, ready, start)
            );

            Future<TransferOutcome> second = executor.submit(
                    transferTask(secondCommand, ready, start)
            );

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "As duas threads deveriam estar prontas para iniciar"
            );

            start.countDown();

            TransferOutcome firstOutcome = first.get(
                    15,
                    TimeUnit.SECONDS
            );

            TransferOutcome secondOutcome = second.get(
                    15,
                    TimeUnit.SECONDS
            );

            return List.of(firstOutcome, secondOutcome);

        } finally {
            start.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(5, TimeUnit.SECONDS),
                    "As threads de transferência deveriam ser encerradas"
            );
        }
    }

    private Callable<TransferOutcome> transferTask(
            TransferCommand command,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();

            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timeout aguardando início da transferência concorrente"
                );
            }

            try {
                transferService.transfer(command);
                return TransferOutcome.SUCCESS;

            } catch (IllegalArgumentException exception) {
                return TransferOutcome.REJECTED;
            }
        };
    }

    private TransferCommand command(
            Account source,
            Account destination,
            String amount,
            String idempotencyKey
    ) {
        return new TransferCommand(
                source.getAccountNumber(),
                destination.getAccountNumber(),
                new BigDecimal(amount),
                idempotencyKey,
                owner.getId()
        );
    }

    private User createUser(String username) {
        return userRepository.save(
                User.builder()
                        .fullName(username)
                        .email(username + "@integrity-test.com")
                        .passwordHash("integration-test-password-hash")
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build()
        );
    }

    private Account createAccount(
            String accountNumber,
            BigDecimal balance,
            User user
    ) {
        return accountRepository.save(
                Account.builder()
                        .accountNumber(accountNumber)
                        .balance(balance)
                        .currency("BRL")
                        .status(AccountStatus.ACTIVE)
                        .user(user)
                        .build()
        );
    }

    private void assertBalance(
            Account account,
            String expectedBalance
    ) {
        assertEquals(
                0,
                new BigDecimal(expectedBalance).compareTo(
                        currentBalance(account)
                ),
                "Saldo inesperado para a conta " + account.getAccountNumber()
        );
    }

    private BigDecimal currentBalance(Account account) {
        return accountRepository.findById(account.getId())
                .orElseThrow()
                .getBalance();
    }

    private Long countIdempotencyRecords() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfer_idempotency",
                Long.class
        );
    }

    private enum TransferOutcome {
        SUCCESS,
        REJECTED
    }
}