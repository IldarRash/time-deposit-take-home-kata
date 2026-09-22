package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TimeDepositCalculatorTest {
    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0} at day {1}: 1200 becomes {2}")
    @CsvSource({
            "basic,0,1200", "basic,29,1200", "basic,30,1200", "basic,31,1201",
            "basic,45,1201", "basic,365,1201", "basic,366,1201",
            "student,0,1200", "student,29,1200", "student,30,1200", "student,31,1203",
            "student,364,1203", "student,365,1203", "student,366,1200", "student,367,1200",
            "premium,0,1200", "premium,29,1200", "premium,30,1200", "premium,31,1200",
            "premium,44,1200", "premium,45,1200", "premium,46,1205", "premium,366,1205"
    })
    void updatesBalanceOnlyWithinTheExistingPlanWindow(String plan, int days, double expected) {
        TimeDeposit deposit = new TimeDeposit(7, plan, 1200.0, days);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(expected);
        assertThat(deposit.getId()).isEqualTo(7);
        assertThat(deposit.getPlanType()).isEqualTo(plan);
        assertThat(deposit.getDays()).isEqualTo(days);
    }

    // Fixed outputs captured from the unchanged upstream calculator, not
    // recomputed by a second implementation inside the assertions.
    @ParameterizedTest(name = "{0} balance {1} becomes {2}")
    @CsvSource({
            "basic,0,0", "basic,6,6.01", "basic,18,18.01", "basic,30,30.02",
            "basic,54,54.05", "basic,1000.123,1000.9530000000001",
            "basic,1234567,1235595.81", "basic,-18,-18.01",
            "student,0,0", "student,6,6.01", "student,18,18.05", "student,30,30.07",
            "student,54,54.13", "student,1000.123,1002.623",
            "student,1234567,1237653.42", "student,-18,-18.05",
            "premium,0,0", "premium,6,6.03", "premium,18,18.07", "premium,30,30.13",
            "premium,54,54.23", "premium,1000.123,1004.293",
            "premium,1234567,1239711.03", "premium,-18,-18.07"
    })
    void preservesLegacyRoundingAndFinalBalancePrecision(String plan, double balance, double expected) {
        TimeDeposit deposit = new TimeDeposit(1, plan, balance, 46);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "BASIC", "Basic", " basic", "", "premium "})
    void leavesUnrecognizedPlanStringsUnchanged(String plan) {
        TimeDeposit deposit = new TimeDeposit(1, plan, 1200.0, 46);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(1200.0);
    }

    @Test
    void processesEveryDepositIncludingThoseAfterAnUnknownOrIneligiblePlan() {
        List<TimeDeposit> deposits = new ArrayList<>(List.of(
                new TimeDeposit(1, "unknown", 1200.0, 46),
                new TimeDeposit(2, "premium", 1200.0, 45),
                new TimeDeposit(3, "basic", 1200.0, 31),
                new TimeDeposit(4, "student", 1200.0, 365),
                new TimeDeposit(5, "premium", 1200.0, 46)));
        List<TimeDeposit> originalObjects = List.copyOf(deposits);

        calculator.updateBalance(deposits);

        assertThat(deposits).containsExactlyElementsOf(originalObjects);
        assertThat(deposits).extracting(TimeDeposit::getBalance)
                .containsExactly(1200.0, 1200.0, 1201.0, 1203.0, 1205.0);
    }

    @Test
    void anEmptyListIsANoOp() {
        assertThatCode(() -> calculator.updateBalance(List.of())).doesNotThrowAnyException();
    }

    @Test
    void eachCallAccruesAgainWithoutAdvancingDays() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1200.0, 31);

        calculator.updateBalance(List.of(deposit));
        assertThat(deposit.getBalance()).isEqualTo(1201.0);
        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(1202.0);
        assertThat(deposit.getDays()).isEqualTo(31);
    }

    @Test
    void aRepeatedObjectReferenceIsUpdatedForEachListEntry() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1200.0, 31);

        calculator.updateBalance(List.of(deposit, deposit));

        assertThat(deposit.getBalance()).isEqualTo(1202.0);
    }
}
