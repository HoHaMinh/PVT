package com.hoaphat.pvt.model.debt;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "debt_alert_sync",
        uniqueConstraints = @UniqueConstraint(name = "uk_debt_alert_scan_date", columnNames = "scan_date"))
public class DebtAlertSync {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer syncId;

    @Column(name = "scan_date", nullable = false)
    private LocalDate scanDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    private DebtAlertSyncStatus status;

    @Column(name = "last_attempt_at", nullable = false)
    private LocalDateTime lastAttemptAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "days_offset")
    private Integer daysOffset;

    @Column(name = "status_message", length = 1000)
    private String message;

    @Column(name = "total_alerts", nullable = false)
    private Integer totalAlerts = 0;

    @OneToMany(mappedBy = "sync", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DebtAlertEmployeeSnapshot> employees = new ArrayList<>();

    public Integer getSyncId() {
        return syncId;
    }

    public void setSyncId(Integer syncId) {
        this.syncId = syncId;
    }

    public LocalDate getScanDate() {
        return scanDate;
    }

    public void setScanDate(LocalDate scanDate) {
        this.scanDate = scanDate;
    }

    public DebtAlertSyncStatus getStatus() {
        return status;
    }

    public void setStatus(DebtAlertSyncStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public LocalDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(LocalDateTime lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public Integer getDaysOffset() {
        return daysOffset;
    }

    public void setDaysOffset(Integer daysOffset) {
        this.daysOffset = daysOffset;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getTotalAlerts() {
        return totalAlerts;
    }

    public void setTotalAlerts(Integer totalAlerts) {
        this.totalAlerts = totalAlerts;
    }

    public List<DebtAlertEmployeeSnapshot> getEmployees() {
        return employees;
    }

    public void setEmployees(List<DebtAlertEmployeeSnapshot> employees) {
        this.employees = employees;
    }

    public void addEmployee(DebtAlertEmployeeSnapshot employee) {
        employee.setSync(this);
        employees.add(employee);
    }
}
