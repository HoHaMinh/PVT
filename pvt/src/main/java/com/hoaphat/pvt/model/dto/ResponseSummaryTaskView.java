package com.hoaphat.pvt.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ResponseSummaryTaskView {
    private Integer monthEventId;
    private String description;
    private String deadline;
    private String deadlineIso;
    private Integer dailyPriority;
    private boolean allTask;
    private boolean selectable;
    private List<ResponseSummaryMessageView> messages = new ArrayList<ResponseSummaryMessageView>();

    public Integer getMonthEventId() {
        return monthEventId;
    }

    public void setMonthEventId(Integer monthEventId) {
        this.monthEventId = monthEventId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getDeadlineIso() {
        return deadlineIso;
    }

    public void setDeadlineIso(String deadlineIso) {
        this.deadlineIso = deadlineIso;
    }

    public Integer getDailyPriority() {
        return dailyPriority;
    }

    public void setDailyPriority(Integer dailyPriority) {
        this.dailyPriority = dailyPriority;
    }

    public boolean isAllTask() {
        return allTask;
    }

    public void setAllTask(boolean allTask) {
        this.allTask = allTask;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public List<ResponseSummaryMessageView> getMessages() {
        return messages;
    }

    public void setMessages(List<ResponseSummaryMessageView> messages) {
        this.messages = messages == null
                ? new ArrayList<ResponseSummaryMessageView>() : messages;
    }
}
