package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import java.util.List;

public interface DepositRepository {
    List<DepositView> findAll();
    List<TimeDeposit> findAllForUpdate();
    void saveBalances(List<TimeDeposit> deposits);
}
