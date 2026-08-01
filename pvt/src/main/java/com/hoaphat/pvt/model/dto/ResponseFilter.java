package com.hoaphat.pvt.model.dto;

import com.hoaphat.pvt.model.event.MonthEvent;

import java.util.List;

public class ResponseFilter {
    private List<MonthEvent> monthEventList;
    private String nameFilter;
    private ResponseSummaryPreview responseSummary;

    public ResponseFilter() {
    }

    public ResponseFilter(List<MonthEvent> monthEventList, String nameFilter) {
        this.monthEventList = monthEventList;
        this.nameFilter = nameFilter;
    }

    public ResponseFilter(List<MonthEvent> monthEventList, String nameFilter,
                          ResponseSummaryPreview responseSummary) {
        this.monthEventList = monthEventList;
        this.nameFilter = nameFilter;
        this.responseSummary = responseSummary;
    }

    public List<MonthEvent> getMonthEventList() {
        return monthEventList;
    }

    public void setMonthEventList(List<MonthEvent> monthEventList) {
        this.monthEventList = monthEventList;
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    public ResponseSummaryPreview getResponseSummary() {
        return responseSummary;
    }

    public void setResponseSummary(ResponseSummaryPreview responseSummary) {
        this.responseSummary = responseSummary;
    }
}
