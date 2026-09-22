package org.ikigaidigital.application;

import org.ikigaidigital.TimeDepositCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DepositService {
    private final DepositRepository repository;
    private final TimeDepositCalculator calculator;

    public DepositService(DepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    public List<DepositView> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void updateBalances() {
        // One call is one accrual cycle; stored days and withdrawals are unchanged.
        // Row locks are held across calculation and all writes by this transaction.
        var deposits = repository.findAllForUpdate();
        calculator.updateBalance(deposits);
        repository.saveBalances(deposits);
    }
}
