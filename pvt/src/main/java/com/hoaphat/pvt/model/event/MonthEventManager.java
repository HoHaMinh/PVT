package com.hoaphat.pvt.model.event;

import java.util.ArrayList;
import java.util.List;

public class MonthEventManager {
    private ArrayList<MonthEvent> monthEvents;

    public MonthEventManager() {
    }

    public MonthEventManager(ArrayList<MonthEvent> monthEvents) {
        this.monthEvents = monthEvents;
    }

    public ArrayList<MonthEvent> getMonthEvents() {
        return monthEvents;
    }

    public void setMonthEvents(ArrayList<MonthEvent> monthEvents) {
        this.monthEvents = monthEvents;
    }
}
