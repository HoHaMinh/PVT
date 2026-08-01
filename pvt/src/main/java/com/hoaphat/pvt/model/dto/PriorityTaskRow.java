package com.hoaphat.pvt.model.dto;

public class PriorityTaskRow {
    private Integer monthEventId;
    private String description;
    private String deadline;
    private boolean allTask;
    private Integer selectedPriority;
    private Integer priority;

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

    public boolean isAllTask() {
        return allTask;
    }

    public void setAllTask(boolean allTask) {
        this.allTask = allTask;
    }

    public Integer getSelectedPriority() {
        return selectedPriority;
    }

    public void setSelectedPriority(Integer selectedPriority) {
        this.selectedPriority = selectedPriority;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
