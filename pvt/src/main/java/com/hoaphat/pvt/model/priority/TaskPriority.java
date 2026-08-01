package com.hoaphat.pvt.model.priority;

import com.hoaphat.pvt.model.event.MonthEvent;

import javax.persistence.*;

@Entity
@Table(name = "task_priority",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_priority_selection_event",
                columnNames = {"selection_id", "month_event_id"}))
public class TaskPriority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer taskPriorityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selection_id", nullable = false)
    private DailyPrioritySelection selection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "month_event_id", nullable = false)
    private MonthEvent monthEvent;

    @Column(name = "priority_value", nullable = false)
    private Integer priorityValue;

    public Integer getTaskPriorityId() {
        return taskPriorityId;
    }

    public void setTaskPriorityId(Integer taskPriorityId) {
        this.taskPriorityId = taskPriorityId;
    }

    public DailyPrioritySelection getSelection() {
        return selection;
    }

    public void setSelection(DailyPrioritySelection selection) {
        this.selection = selection;
    }

    public MonthEvent getMonthEvent() {
        return monthEvent;
    }

    public void setMonthEvent(MonthEvent monthEvent) {
        this.monthEvent = monthEvent;
    }

    public Integer getPriorityValue() {
        return priorityValue;
    }

    public void setPriorityValue(Integer priorityValue) {
        this.priorityValue = priorityValue;
    }
}
