package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TimeDepositCalculator {
    private final PlanInterestRules rules = new PlanInterestRules();

    public void updateBalance(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            // Preserve accumulation from positive zero, as in the legacy code.
            double interest = 0;
            interest += rules.calculate(deposit);

            // Compatibility: valueOf changes half-cent results (e.g. basic, 18).
            double roundedInterest = new BigDecimal(interest)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            deposit.setBalance(deposit.getBalance() + roundedInterest);
        }
    }
}
