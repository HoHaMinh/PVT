package com.hoaphat.pvt.model.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

public enum DeadlineShiftOption {
    ONE_SESSION("1 buổi"),
    ONE_DAY("1 ngày"),
    TWO_DAYS("2 ngày"),
    THREE_DAYS("3 ngày");

    private final String displayLabel;

    DeadlineShiftOption(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public LocalDateTime apply(LocalDateTime currentDeadline) {
        if (currentDeadline == null) {
            throw new IllegalArgumentException("Deadline không được để trống.");
        }
        switch (this) {
            case ONE_SESSION:
                if (currentDeadline.toLocalTime().isBefore(LocalTime.NOON)) {
                    return currentDeadline.toLocalDate().atTime(16, 0);
                }
                return currentDeadline.toLocalDate().plusDays(1).atTime(11, 0);
            case TWO_DAYS:
                return currentDeadline.plusDays(2);
            case THREE_DAYS:
                return currentDeadline.plusDays(3);
            case ONE_DAY:
            default:
                return currentDeadline.plusDays(1);
        }
    }

    public static DeadlineShiftOption fromRequestValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn khoảng thời gian dời deadline.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Khoảng thời gian dời deadline không hợp lệ.");
        }
    }
}
