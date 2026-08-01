package com.hoaphat.pvt.repository.debt;

import com.hoaphat.pvt.model.debt.DebtAlertSync;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface IDebtAlertSyncRepository extends JpaRepository<DebtAlertSync, Integer> {
    Optional<DebtAlertSync> findByScanDate(LocalDate scanDate);
    Optional<DebtAlertSync> findTopByOrderByScanDateDesc();
}
