package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.dto.ResponseSummaryPeriod;
import com.hoaphat.pvt.model.dto.ResponseSummaryTimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ResponseSummaryPeriodTest {

    @Test
    void todayRunsFromMidnightThroughTheRequestTime() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 30, 13, 15, 20);

        ResponseSummaryTimeRange range = ResponseSummaryPeriod.TODAY.resolve(now);

        assertEquals(LocalDateTime.of(2026, 7, 30, 0, 0), range.getStartInclusive());
        assertTrue(range.getEndExclusive().isAfter(now));
        assertEquals(LocalDate.of(2026, 7, 30), range.getPriorityDate());
    }

    @Test
    void yesterdayUsesAClosedOpenCalendarDay() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 30, 8, 0);

        ResponseSummaryTimeRange range = ResponseSummaryPeriod.YESTERDAY.resolve(now);

        assertEquals(LocalDateTime.of(2026, 7, 29, 0, 0), range.getStartInclusive());
        assertEquals(LocalDateTime.of(2026, 7, 30, 0, 0), range.getEndExclusive());
        assertEquals(LocalDate.of(2026, 7, 29), range.getPriorityDate());
    }

    @Test
    void morningStopsAtNowBeforeNoonAndAtNoonAfterwards() {
        ResponseSummaryTimeRange beforeNoon = ResponseSummaryPeriod.MORNING.resolve(
                LocalDateTime.of(2026, 7, 30, 11, 0));
        ResponseSummaryTimeRange afterNoon = ResponseSummaryPeriod.MORNING.resolve(
                LocalDateTime.of(2026, 7, 30, 15, 0));

        assertTrue(beforeNoon.getEndExclusive().isAfter(LocalDateTime.of(2026, 7, 30, 11, 0)));
        assertEquals(LocalDateTime.of(2026, 7, 30, 12, 0), afterNoon.getEndExclusive());
    }

    @Test
    void afternoonStartsAtNoonAndIsEmptyBeforeNoon() {
        ResponseSummaryTimeRange beforeNoon = ResponseSummaryPeriod.AFTERNOON.resolve(
                LocalDateTime.of(2026, 7, 30, 11, 0));
        ResponseSummaryTimeRange afterNoon = ResponseSummaryPeriod.AFTERNOON.resolve(
                LocalDateTime.of(2026, 7, 30, 15, 0));

        assertTrue(beforeNoon.isEmpty());
        assertEquals(LocalDateTime.of(2026, 7, 30, 12, 0), afterNoon.getStartInclusive());
        assertTrue(afterNoon.getEndExclusive().isAfter(LocalDateTime.of(2026, 7, 30, 15, 0)));
    }

    @Test
    void invalidPeriodFallsBackToToday() {
        assertEquals(ResponseSummaryPeriod.TODAY, ResponseSummaryPeriod.fromRequestValue(null));
        assertEquals(ResponseSummaryPeriod.TODAY, ResponseSummaryPeriod.fromRequestValue(""));
        assertEquals(ResponseSummaryPeriod.TODAY, ResponseSummaryPeriod.fromRequestValue("unknown"));
        assertEquals(ResponseSummaryPeriod.YESTERDAY,
                ResponseSummaryPeriod.fromRequestValue("yesterday"));
    }
}
