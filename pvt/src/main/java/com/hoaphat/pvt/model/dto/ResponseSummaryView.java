package com.hoaphat.pvt.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ResponseSummaryView {
    private String accountName;
    private String employeeDisplayName;
    private ResponseSummaryPeriod period;
    private int taskCount;
    private int responseCount;
    private List<ResponseSummaryTaskView> tasks = new ArrayList<ResponseSummaryTaskView>();

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getEmployeeDisplayName() {
        return employeeDisplayName;
    }

    public void setEmployeeDisplayName(String employeeDisplayName) {
        this.employeeDisplayName = employeeDisplayName;
    }

    public ResponseSummaryPeriod getPeriod() {
        return period;
    }

    public void setPeriod(ResponseSummaryPeriod period) {
        this.period = period;
    }

    public String getTitle() {
        return "Tổng hợp phản hồi của " + employeeDisplayName + " "
                + (period == null ? ResponseSummaryPeriod.TODAY.getDisplayLabel()
                : period.getDisplayLabel());
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public int getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(int responseCount) {
        this.responseCount = responseCount;
    }

    public List<ResponseSummaryTaskView> getTasks() {
        return tasks;
    }

    public void setTasks(List<ResponseSummaryTaskView> tasks) {
        this.tasks = tasks == null ? new ArrayList<ResponseSummaryTaskView>() : tasks;
    }
}
