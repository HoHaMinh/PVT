package com.hoaphat.pvt.model.dto.debt;

import java.util.ArrayList;
import java.util.List;

public class DebtAlertApiResponse {
    private boolean success;
    private boolean skipped;
    private String scanDate;
    private String generatedAt;
    private Integer daysOffset;
    private String message;
    private String skipReason;
    private List<DebtAlertEmployeePayload> employees = new ArrayList<>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public String getScanDate() {
        return scanDate;
    }

    public void setScanDate(String scanDate) {
        this.scanDate = scanDate;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
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

    public String getSkipReason() {
        return skipReason;
    }

    public void setSkipReason(String skipReason) {
        this.skipReason = skipReason;
    }

    public List<DebtAlertEmployeePayload> getEmployees() {
        return employees;
    }

    public void setEmployees(List<DebtAlertEmployeePayload> employees) {
        this.employees = employees;
    }
}
