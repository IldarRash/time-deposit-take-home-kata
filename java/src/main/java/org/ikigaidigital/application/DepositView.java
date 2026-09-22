package org.ikigaidigital.application;

import java.math.BigDecimal;
import java.util.List;

/** Query result separate from the shared mutable calculation model. */
public record DepositView(int id, String planType, BigDecimal balance, int days,
                          List<Withdrawal> withdrawals) { }
