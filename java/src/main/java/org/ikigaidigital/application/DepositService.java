package org.ikigaidigital.application;

import org.ikigaidigital.TimeDepositCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
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
        var originalBalances = new HashMap<Integer, Double>();
        deposits.forEach(deposit -> originalBalances.put(deposit.getId(), deposit.getBalance()));
        calculator.updateBalance(deposits);
        // Keep the original database decimal when the legacy calculation is a no-op.
        // A Double round trip alone can lose digits, even when no interest is earned.
        var changed = deposits.stream()
                .filter(deposit -> !deposit.getBalance().equals(originalBalances.get(deposit.getId())))
                .toList();
        repository.saveBalances(changed);
    }
}
