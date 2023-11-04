package com.hoaphat.pvt.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class ResponseEventInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRespose;
    private String eventInformationResponse;
    @ManyToOne
    @JoinColumn(name = "month_event_id", referencedColumnName = "monthEventId")
    private MonthEvent monthEvent;
    @CreatedBy
    private String createdByUser;
    @CreatedDate
    @JsonFormat(pattern = "HH:mm dd-MM-yyyy")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdByDate;

    public ResponseEventInformation() {
    }

    public ResponseEventInformation(Integer idRespose, String eventInformationResponse, MonthEvent monthEvent, String createdByUser, LocalDateTime createdByDate) {
        this.idRespose = idRespose;
        this.eventInformationResponse = eventInformationResponse;
        this.monthEvent = monthEvent;
        this.createdByUser = createdByUser;
        this.createdByDate = createdByDate;
    }

    public ResponseEventInformation(String eventInformationResponse, MonthEvent monthEvent, String createdByUser, LocalDateTime createdByDate) {
        this.eventInformationResponse = eventInformationResponse;
        this.monthEvent = monthEvent;
        this.createdByUser = createdByUser;
        this.createdByDate = createdByDate;
    }

    public ResponseEventInformation(MonthEvent monthEvent) {
        this.monthEvent = monthEvent;
    }

    public Integer getIdRespose() {
        return idRespose;
    }

    public void setIdRespose(Integer idRespose) {
        this.idRespose = idRespose;
    }

    public String getEventInformationResponse() {
        return eventInformationResponse;
    }

    public void setEventInformationResponse(String eventInformationResponse) {
        this.eventInformationResponse = eventInformationResponse;
    }

    public MonthEvent getMonthEvent() {
        return monthEvent;
    }

    public void setMonthEvent(MonthEvent monthEvent) {
        this.monthEvent = monthEvent;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public LocalDateTime getCreatedByDate() {
        return createdByDate;
    }

    public void setCreatedByDate(LocalDateTime createdByDate) {
        this.createdByDate = createdByDate;
    }

    public String getFormattedCreatedByDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy");
        return this.createdByDate.format(formatter);
    }
}
