package org.ikigaidigital;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Plan-specific formulas; adding a rule does not change balance application. */
final class PlanInterestRules {
    private static final ToDoubleFunction<TimeDeposit> NO_INTEREST = deposit -> 0;
    private static final Map<String, ToDoubleFunction<TimeDeposit>> RULES = Map.of(
            "basic", deposit -> deposit.getBalance() * 0.01 / 12,
            "student", deposit -> deposit.getDays() < 366
                    ? deposit.getBalance() * 0.03 / 12 : 0,
            "premium", deposit -> deposit.getDays() > 45
                    ? deposit.getBalance() * 0.05 / 12 : 0);

    double calculate(TimeDeposit deposit) {
        // Existing plans share a 30-day waiting period. Rates retain the legacy
        // division by 12 despite the README's ambiguous "monthly interest" wording.
        if (deposit.getDays() <= 30) {
            return 0;
        }
        return RULES.getOrDefault(deposit.getPlanType(), NO_INTEREST).applyAsDouble(deposit);
    }
}
