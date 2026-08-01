package com.hoaphat.pvt.model.dto.debt;

import java.util.ArrayList;
import java.util.List;

public class DebtAlertEmployeeView {
    private String accountName;
    private String employeeName;
    private String spreadsheetUrl;
    private String reportLabel;
    private String status;
    private String message;
    private List<DebtAlertItemView> items = new ArrayList<>();

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getSpreadsheetUrl() { return spreadsheetUrl; }
    public void setSpreadsheetUrl(String spreadsheetUrl) { this.spreadsheetUrl = spreadsheetUrl; }
    public String getReportLabel() { return reportLabel; }
    public void setReportLabel(String reportLabel) { this.reportLabel = reportLabel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<DebtAlertItemView> getItems() { return items; }
    public void setItems(List<DebtAlertItemView> items) { this.items = items; }
}
