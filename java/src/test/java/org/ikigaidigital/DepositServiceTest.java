package org.ikigaidigital;

import org.ikigaidigital.application.DepositRepository;
import org.ikigaidigital.application.DepositService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepositServiceTest {
    @Test
    void onlyChangedBalancesAreSentBackToPersistence() {
        var repository = mock(DepositRepository.class);
        var unknown = new TimeDeposit(1, "unknown", 0.1, 60);
        var waiting = new TimeDeposit(2, "basic", 0.1, 30);
        var earning = new TimeDeposit(3, "basic", 1200.0, 31);
        when(repository.findAllForUpdate()).thenReturn(List.of(unknown, waiting, earning));

        new DepositService(repository, new TimeDepositCalculator()).updateBalances();

        // Rewriting unchanged Double values can discard original NUMERIC precision.
        verify(repository).saveBalances(List.of(earning));
    }
}
