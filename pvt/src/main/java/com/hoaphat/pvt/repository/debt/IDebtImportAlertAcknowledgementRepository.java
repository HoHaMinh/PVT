package com.hoaphat.pvt.repository.debt;

import com.hoaphat.pvt.model.debt.DebtImportAlertAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IDebtImportAlertAcknowledgementRepository
        extends JpaRepository<DebtImportAlertAcknowledgement, Integer> {
    Optional<DebtImportAlertAcknowledgement> findByAlertKey(String alertKey);
    List<DebtImportAlertAcknowledgement> findByAlertKeyIn(Collection<String> alertKeys);
}
