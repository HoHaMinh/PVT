package com.hoaphat.pvt.model.debt;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "debt_alert_employee")
public class DebtAlertEmployeeSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer employeeSnapshotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sync_id", nullable = false)
    private DebtAlertSync sync;

    @Column(name = "account_name", length = 100)
    private String accountName;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "spreadsheet_id", length = 200)
    private String spreadsheetId;

    @Column(name = "spreadsheet_url", length = 500)
    private String spreadsheetUrl;

    @Column(name = "report_sheet_name")
    private String reportSheetName;

    @Column(name = "report_batch", length = 100)
    private String reportBatch;

    @Column(name = "report_month", length = 50)
    private String reportMonth;

    @Column(name = "result_status", nullable = false, length = 40)
    private String resultStatus;

    @Column(name = "result_message", length = 1000)
    private String message;

    @OneToMany(mappedBy = "employeeSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DebtAlertItem> items = new ArrayList<>();

    public Integer getEmployeeSnapshotId() {
        return employeeSnapshotId;
    }

    public void setEmployeeSnapshotId(Integer employeeSnapshotId) {
        this.employeeSnapshotId = employeeSnapshotId;
    }

    public DebtAlertSync getSync() {
        return sync;
    }

    public void setSync(DebtAlertSync sync) {
        this.sync = sync;
    }

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

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<DebtAlertItem> getItems() {
        return items;
    }

    public void setItems(List<DebtAlertItem> items) {
        this.items = items;
    }

    public void addItem(DebtAlertItem item) {
        item.setEmployeeSnapshot(this);
        items.add(item);
    }
}
