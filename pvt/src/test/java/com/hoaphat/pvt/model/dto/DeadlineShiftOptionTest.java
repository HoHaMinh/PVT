package com.hoaphat.pvt.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeadlineShiftOptionTest {
    @Test
    void calendarDayOptionsPreserveTime() {
        LocalDateTime source = LocalDateTime.of(2026, 7, 29, 11, 25);

        assertEquals(LocalDateTime.of(2026, 7, 30, 11, 25),
                DeadlineShiftOption.ONE_DAY.apply(source));
        assertEquals(LocalDateTime.of(2026, 7, 31, 11, 25),
                DeadlineShiftOption.TWO_DAYS.apply(source));
        assertEquals(LocalDateTime.of(2026, 8, 1, 11, 25),
                DeadlineShiftOption.THREE_DAYS.apply(source));
    }

    @Test
    void oneSessionMovesMorningToSameDayAtSixteen() {
        assertEquals(LocalDateTime.of(2026, 7, 29, 16, 0),
                DeadlineShiftOption.ONE_SESSION.apply(
                        LocalDateTime.of(2026, 7, 29, 11, 59)));
    }

    @Test
    void oneSessionMovesNoonOrAfternoonToNextDayAtEleven() {
        assertEquals(LocalDateTime.of(2026, 7, 30, 11, 0),
                DeadlineShiftOption.ONE_SESSION.apply(
                        LocalDateTime.of(2026, 7, 29, 12, 0)));
        assertEquals(LocalDateTime.of(2026, 7, 30, 11, 0),
                DeadlineShiftOption.ONE_SESSION.apply(
                        LocalDateTime.of(2026, 7, 29, 16, 0)));
    }
}
