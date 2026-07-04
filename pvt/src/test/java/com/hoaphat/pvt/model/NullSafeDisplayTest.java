package com.hoaphat.pvt.model;

import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NullSafeDisplayTest {

    @Test
    void monthEventDateLabelsAreSafeWhenLegacyDataIsNull() {
        MonthEvent event = new MonthEvent();
        event.setMonthEventDeadline(null);
        event.setLastTimeResponse(null);
        event.setRegisteredDay(null);

        assertEquals("Chưa có deadline", event.getFormattedDeadlineByFormat());
        assertEquals("Chưa có phản hồi", event.getFormattedLastTimeResponse());
        assertEquals("Không rõ ngày giao", event.getFormattedRegisteredDay());
    }

    @Test
    void responseDateLabelIsSafeWhenLegacyDataIsNull() {
        ResponseEventInformation response = new ResponseEventInformation();
        response.setCreatedByDate(null);

        assertEquals("Không rõ thời gian", response.getFormattedCreatedByDate());
    }
}
