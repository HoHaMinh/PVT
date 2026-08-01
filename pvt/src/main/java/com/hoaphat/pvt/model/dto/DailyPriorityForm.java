package com.hoaphat.pvt.model.dto;

import java.util.ArrayList;
import java.util.List;

public class DailyPriorityForm {
    private String accountName;
    private boolean alreadySubmitted;
    private List<PriorityTaskRow> tasks = new ArrayList<>();

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public boolean isAlreadySubmitted() {
        return alreadySubmitted;
    }

    public void setAlreadySubmitted(boolean alreadySubmitted) {
        this.alreadySubmitted = alreadySubmitted;
    }

    public List<PriorityTaskRow> getTasks() {
        return tasks;
    }

    public void setTasks(List<PriorityTaskRow> tasks) {
        this.tasks = tasks;
    }
}
