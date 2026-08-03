package com.hoaphat.pvt.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

public enum ResponseSummaryPeriod {
    TODAY("ngày hôm nay"),
    YESTERDAY("ngày hôm qua"),
    TWO_DAYS_AGO("ngày hôm kia"),
    MORNING("từ sáng đến giờ"),
    AFTERNOON("từ chiều đến giờ");

    private final String displayLabel;

    ResponseSummaryPeriod(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public ResponseSummaryTimeRange resolve(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("Không xác định được thời điểm tổng hợp.");
        }
        LocalDate today = now.toLocalDate();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime noon = today.atTime(LocalTime.NOON);
        LocalDateTime justAfterNow = now.plusNanos(1);

        switch (this) {
            case YESTERDAY:
                return new ResponseSummaryTimeRange(
                        today.minusDays(1).atStartOfDay(),
                        todayStart,
                        today.minusDays(1));
            case TWO_DAYS_AGO:
                LocalDate twoDaysAgo = today.minusDays(2);
                return new ResponseSummaryTimeRange(
                        twoDaysAgo.atStartOfDay(),
                        today.minusDays(1).atStartOfDay(),
                        twoDaysAgo);
            case MORNING:
                return new ResponseSummaryTimeRange(
                        todayStart,
                        now.isBefore(noon) ? justAfterNow : noon,
                        today);
            case AFTERNOON:
                return new ResponseSummaryTimeRange(
                        noon,
                        now.isBefore(noon) ? noon : justAfterNow,
                        today);
            case TODAY:
            default:
                return new ResponseSummaryTimeRange(todayStart, justAfterNow, today);
        }
    }

    public static ResponseSummaryPeriod fromRequestValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return TODAY;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return TODAY;
        }
    }
}
