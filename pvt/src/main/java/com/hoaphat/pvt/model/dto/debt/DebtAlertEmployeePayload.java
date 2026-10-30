package com.hoaphat.pvt.model.dto.debt;

import java.util.ArrayList;
import java.util.List;

public class DebtAlertEmployeePayload {
    private String accountName;
    private String employeeName;
    private String spreadsheetId;
    private String spreadsheetUrl;
    private String reportSheetName;
    private String reportBatch;
    private String reportMonth;
    private String status;
    private String message;
    private List<DebtAlertItemPayload> alerts = new ArrayList<>();
    private List<DebtImportAlertPayload> importAlerts = new ArrayList<>();

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    public String getSpreadsheetUrl() {
        return spreadsheetUrl;
    }

    public void setSpreadsheetUrl(String spreadsheetUrl) {
        this.spreadsheetUrl = spreadsheetUrl;
    }

    public String getReportSheetName() {
        return reportSheetName;
    }

    public void setReportSheetName(String reportSheetName) {
        this.reportSheetName = reportSheetName;
    }

    public String getReportBatch() {
        return reportBatch;
    }

    public void setReportBatch(String reportBatch) {
        this.reportBatch = reportBatch;
    }

    public String getReportMonth() {
        return reportMonth;
    }

    public void setReportMonth(String reportMonth) {
        this.reportMonth = reportMonth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<DebtAlertItemPayload> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<DebtAlertItemPayload> alerts) {
        this.alerts = alerts;
    }

    public List<DebtImportAlertPayload> getImportAlerts() { return importAlerts; }
    public void setImportAlerts(List<DebtImportAlertPayload> importAlerts) { this.importAlerts = importAlerts; }
}
