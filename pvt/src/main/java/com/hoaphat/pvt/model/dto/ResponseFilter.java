package com.hoaphat.pvt.model.dto;

import com.hoaphat.pvt.model.event.MonthEvent;

import java.util.List;

public class ResponseFilter {
    private List<MonthEvent> monthEventList;
    private String nameFilter;

    public ResponseFilter() {
    }

    public ResponseFilter(List<MonthEvent> monthEventList, String nameFilter) {
        this.monthEventList = monthEventList;
        this.nameFilter = nameFilter;
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
}
