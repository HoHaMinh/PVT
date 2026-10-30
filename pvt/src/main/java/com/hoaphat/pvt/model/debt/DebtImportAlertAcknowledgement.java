package com.hoaphat.pvt.model.debt;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "debt_import_alert_ack",
        uniqueConstraints = @UniqueConstraint(name = "uk_debt_import_alert_key", columnNames = "alert_key"))
public class DebtImportAlertAcknowledgement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer acknowledgementId;

    @Column(name = "alert_key", nullable = false, length = 64)
    private String alertKey;

    @Column(name = "acknowledged_by", nullable = false, length = 100)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at", nullable = false)
    private LocalDateTime acknowledgedAt;

    public Integer getAcknowledgementId() { return acknowledgementId; }
    public void setAcknowledgementId(Integer acknowledgementId) { this.acknowledgementId = acknowledgementId; }
    public String getAlertKey() { return alertKey; }
    public void setAlertKey(String alertKey) { this.alertKey = alertKey; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
}
