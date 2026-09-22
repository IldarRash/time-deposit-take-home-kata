package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.DepositRepository;
import org.ikigaidigital.application.DepositView;
import org.ikigaidigital.application.Withdrawal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Repository
public class JdbcDepositRepository implements DepositRepository {
    private final JdbcTemplate jdbc;

    public JdbcDepositRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<DepositView> findAll() {
        // One statement provides a consistent view without an N+1 query per deposit.
        return jdbc.query("""
                SELECT d.id AS deposit_id, d."planType", d.balance, d.days,
                       w.id AS withdrawal_id, w.amount, w.date
                FROM "timeDeposits" d
                LEFT JOIN withdrawals w ON w."timeDepositId" = d.id
                ORDER BY d.id, w.id
                """, rs -> {
            var deposits = new LinkedHashMap<Integer, DepositView>();
            while (rs.next()) {
                int id = rs.getInt("deposit_id");
                DepositView deposit = deposits.get(id);
                if (deposit == null) {
                    deposit = new DepositView(id, rs.getString("planType"),
                            rs.getBigDecimal("balance"), rs.getInt("days"), new ArrayList<>());
                    deposits.put(id, deposit);
                }
                Integer withdrawalId = (Integer) rs.getObject("withdrawal_id");
                if (withdrawalId != null) {
                    deposit.withdrawals().add(new Withdrawal(withdrawalId,
                            rs.getBigDecimal("amount"), rs.getDate("date").toLocalDate()));
                }
            }
            return List.copyOf(deposits.values());
        });
    }

    @Override
    public List<TimeDeposit> findAllForUpdate() {
        // Ordered locks serialize overlapping batches; no endpoint inserts deposits.
        return jdbc.query("""
                SELECT id, "planType", balance, days FROM "timeDeposits"
                ORDER BY id FOR UPDATE
                """, (rs, row) -> new TimeDeposit(rs.getInt("id"), rs.getString("planType"),
                rs.getBigDecimal("balance").doubleValue(), rs.getInt("days")));
    }

    @Override
    public void saveBalances(List<TimeDeposit> deposits) {
        // NUMERIC has no forced scale: preserve fractional legacy results. This
        // adapter conversion must not replace new BigDecimal(interest) in the calculator.
        jdbc.batchUpdate("UPDATE \"timeDeposits\" SET balance = ? WHERE id = ?",
                deposits.stream().map(deposit -> new Object[]{
                        BigDecimal.valueOf(deposit.getBalance()), deposit.getId()}).toList());
    }
}
