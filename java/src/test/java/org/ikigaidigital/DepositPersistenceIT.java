package org.ikigaidigital;

import org.ikigaidigital.application.DepositRepository;
import org.ikigaidigital.application.DepositService;
import org.ikigaidigital.application.DepositView;
import org.ikigaidigital.application.Withdrawal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DepositPersistenceIT extends PostgresIntegrationSupport {
    @Autowired DepositService service;
    @Autowired DepositRepository repository;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void emptyDatabaseHasNoDepositsAndCanBeUpdated() {
        assertThat(service.findAll()).isEmpty();
        service.updateBalances();
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void listsOrderedDepositsWithOnlyTheirOwnWithdrawals() {
        insertDeposit(2, "student", 365, "1200.00");
        insertDeposit(1, "basic", 31, "1200.00");
        insertDeposit(3, "premium", 46, "1200.00");
        jdbc.update("""
                INSERT INTO withdrawals VALUES
                    (20, 1, 12.34, '2026-01-20'),
                    (10, 1, 3.45, '2026-01-10'),
                    (30, 2, 5.67, '2026-01-30')
                """);

        var deposits = service.findAll();

        assertThat(deposits).extracting(DepositView::id).containsExactly(1, 2, 3);
        assertThat(deposits.get(0).withdrawals()).containsExactly(
                new Withdrawal(10, new BigDecimal("3.45"), LocalDate.of(2026, 1, 10)),
                new Withdrawal(20, new BigDecimal("12.34"), LocalDate.of(2026, 1, 20)));
        assertThat(deposits.get(1).withdrawals()).extracting(Withdrawal::id).containsExactly(30);
        assertThat(deposits.get(2).withdrawals()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"basic,18,18.01", "basic,1000.123,1000.9530000000001",
            "student,1000.123,1002.623", "premium,1000.123,1004.293"})
    void decimalStorageRoundTripsLegacyResults(String plan, String initial, String expected) {
        insertDeposit(1, plan, 46, initial);

        service.updateBalances();

        assertThat(balance(1)).isEqualByComparingTo(expected);
        assertThat(service.findAll().get(0).balance().doubleValue())
                .isEqualTo(Double.parseDouble(expected));
        Double reloadedBalance = new TransactionTemplate(transactionManager).execute(status ->
                repository.findAllForUpdate().get(0).getBalance());
        assertThat(reloadedBalance).isEqualTo(Double.parseDouble(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "unknown,60,0.10000000000000000001",
            "basic,30,0.10000000000000000001",
            "student,366,0.10000000000000000001",
            "premium,45,0.10000000000000000001",
            "basic,31,0.10000000000000000001",
            "unknown,60,9007199254740993"
    })
    void unchangedLegacyBalancesRetainTheirExactStoredDecimal(String plan, int days, String initial) {
        insertDeposit(1, plan, days, initial);
        insertDeposit(2, "basic", 31, "1200");

        service.updateBalances();
        service.updateBalances();

        assertThat(balance(1)).isEqualByComparingTo(initial);
        assertThat(service.findAll().get(0).balance()).isEqualByComparingTo(initial);
        assertThat(balance(2)).isEqualByComparingTo("1202");
    }

    @Test
    void updatesAllPlansWithoutAdvancingDaysOrReapplyingWithdrawals() {
        String unknownPlan = "unknown".repeat(40);
        insertDeposit(1, unknownPlan, 46, "1200");
        insertDeposit(2, "basic", 31, "1200");
        insertDeposit(3, "student", 365, "1200");
        insertDeposit(4, "premium", 46, "1200");
        insertDeposit(5, "student", 366, "1200");
        jdbc.update("INSERT INTO withdrawals VALUES (1, 2, 25.00, '2026-01-15')");

        service.updateBalances();

        assertThat(service.findAll()).extracting(view -> view.balance().doubleValue())
                .containsExactly(1200.0, 1201.0, 1203.0, 1205.0, 1200.0);
        assertThat(service.findAll()).extracting(DepositView::days).containsExactly(46, 31, 365, 46, 366);
        assertThat(service.findAll().get(0).planType()).isEqualTo(unknownPlan);
        assertThat(service.findAll().get(1).withdrawals()).containsExactly(
                new Withdrawal(1, new BigDecimal("25.00"), LocalDate.of(2026, 1, 15)));
    }

    @Test
    void repeatedTransactionsAccrueAgainUsingThePersistedBalance() {
        insertDeposit(1, "basic", 31, "1200");
        service.updateBalances();
        assertThat(balance(1)).isEqualByComparingTo("1201");
        service.updateBalances();
        assertThat(balance(1)).isEqualByComparingTo("1202");
    }

    @Test
    void aFailedWriteRollsBackTheWholeBatch() {
        insertDeposit(1, "basic", 31, "1200");
        insertDeposit(2, "student", 31, "1200");
        jdbc.execute("ALTER TABLE \"timeDeposits\" ADD CONSTRAINT test_failure CHECK (id <> 2 OR balance <= 1200)");
        try {
            assertThatThrownBy(service::updateBalances).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(balance(1)).isEqualByComparingTo("1200");
            assertThat(balance(2)).isEqualByComparingTo("1200");
        } finally {
            jdbc.execute("ALTER TABLE \"timeDeposits\" DROP CONSTRAINT test_failure");
        }
    }

    @Test
    void schemaEnforcesRequiredValuesPrimaryKeysAndWithdrawalOwnership() {
        insertDeposit(1, "basic", 31, "1200");
        assertThatThrownBy(() -> insertDeposit(1, "basic", 31, "1200"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO \"timeDeposits\" VALUES (2, 'basic', 31, NULL)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO withdrawals VALUES (1, 99, 1.00, '2026-01-01')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO withdrawals VALUES (1, 1, 1.00, NULL)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void overlappingUpdatesSerializeWithoutLosingAnAccrual() throws Exception {
        insertDeposit(1, "basic", 31, "1200");
        var executor = Executors.newFixedThreadPool(2);
        var firstHasLock = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        repository.findAllForUpdate();
                        firstHasLock.countDown();
                        try {
                            if (!releaseFirst.await(15, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("First transaction was not released");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        service.updateBalances();
                    }));
            assertThat(firstHasLock.await(10, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(service::updateBalances);
            // Observe the database lock, rather than assuming overlap after a sleep.
            await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock'
                      AND query LIKE '%FOR UPDATE%'
                    """, Integer.class) > 0);
            releaseFirst.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            assertThat(balance(1)).isEqualByComparingTo("1202");
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
