package com.hoaphat.pvt.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class MonthEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer monthEventId;
    private String monthEventDescription;
    @JsonFormat(pattern = "HH:mm dd-MM-yyyy")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime monthEventDeadline;
    @Column(columnDefinition = "int default 0")
    private Integer monthEventStatus = 0;
    @Column(columnDefinition = "int default 0")
    private Integer responseStatus = 0;

    private Integer extendDay;

    public MonthEvent() {
    }

    public MonthEvent(Integer monthEventId, String monthEventDescription, LocalDateTime monthEventDeadline, Integer monthEventStatus, Integer extendDay) {
        this.monthEventId = monthEventId;
        this.monthEventDescription = monthEventDescription;
        this.monthEventDeadline = monthEventDeadline;
        this.monthEventStatus = monthEventStatus;
        this.extendDay = extendDay;
    }

    public MonthEvent(String monthEventDescription, LocalDateTime monthEventDeadline, Integer monthEventStatus, Integer extendDay) {
        this.monthEventDescription = monthEventDescription;
        this.monthEventDeadline = monthEventDeadline;
        this.monthEventStatus = monthEventStatus;
        this.extendDay = extendDay;
    }

    public MonthEvent(String monthEventDescription, LocalDateTime monthEventDeadline) {
        this.monthEventDescription = monthEventDescription;
        this.monthEventDeadline = monthEventDeadline;
    }

    public Integer getMonthEventId() {
        return monthEventId;
    }

    public void setMonthEventId(Integer monthEventId) {
        this.monthEventId = monthEventId;
    }

    public String getMonthEventDescription() {
        return monthEventDescription;
    }

    public void setMonthEventDescription(String monthEventDescription) {
        this.monthEventDescription = monthEventDescription;
    }

    public LocalDateTime getMonthEventDeadline() {
        return monthEventDeadline;
    }

    public String getFormattedDeadlineByFormat() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy");
        return this.monthEventDeadline.format(formatter);
    }

    public void setMonthEventDeadline(LocalDateTime monthEventDeadline) {
        this.monthEventDeadline = monthEventDeadline;
    }

    public Integer getMonthEventStatus() {
        return monthEventStatus;
    }

    public void setMonthEventStatus(Integer monthEventStatus) {
        this.monthEventStatus = monthEventStatus;
    }

    public Integer getExtendDay() {
        return extendDay;
    }

    public void setExtendDay(Integer extendDay) {
        this.extendDay = extendDay;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }
}
