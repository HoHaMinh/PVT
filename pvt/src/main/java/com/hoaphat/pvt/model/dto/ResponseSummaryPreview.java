package com.hoaphat.pvt.model.dto;

public class ResponseSummaryPreview {
    private final String accountName;
    private final String employeeDisplayName;
    private final String title;
    private final int taskCount;
    private final int responseCount;

    public ResponseSummaryPreview(String accountName, String employeeDisplayName,
                                  String title, int taskCount, int responseCount) {
        this.accountName = accountName;
        this.employeeDisplayName = employeeDisplayName;
        this.title = title;
        this.taskCount = taskCount;
        this.responseCount = responseCount;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getEmployeeDisplayName() {
        return employeeDisplayName;
    }

    public String getTitle() {
        return title;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public int getResponseCount() {
        return responseCount;
    }
}
