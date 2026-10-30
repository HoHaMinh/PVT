package com.hoaphat.pvt.model.dto.debt;

import java.util.ArrayList;
import java.util.List;

public class DebtAlertDashboard {
    private boolean enabled;
    private boolean configured;
    private boolean running;
    private String status;
    private String scanDate;
    private String lastAttemptAt;
    private String lastSuccessAt;
    private String message;
    private int alertCount;
    private int importAlertCount;
    private List<DebtAlertEmployeeView> employees = new ArrayList<>();
    private List<DebtImportAlertView> importAlerts = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getScanDate() { return scanDate; }
    public void setScanDate(String scanDate) { this.scanDate = scanDate; }
    public String getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(String lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public String getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(String lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getAlertCount() { return alertCount; }
    public void setAlertCount(int alertCount) { this.alertCount = alertCount; }
    public int getImportAlertCount() { return importAlertCount; }
    public void setImportAlertCount(int importAlertCount) { this.importAlertCount = importAlertCount; }
    public List<DebtAlertEmployeeView> getEmployees() { return employees; }
    public void setEmployees(List<DebtAlertEmployeeView> employees) { this.employees = employees; }
    public List<DebtImportAlertView> getImportAlerts() { return importAlerts; }
    public void setImportAlerts(List<DebtImportAlertView> importAlerts) { this.importAlerts = importAlerts; }
}
