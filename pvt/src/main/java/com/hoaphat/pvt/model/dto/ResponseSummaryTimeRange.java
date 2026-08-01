package com.hoaphat.pvt.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ResponseSummaryTimeRange {
    private final LocalDateTime startInclusive;
    private final LocalDateTime endExclusive;
    private final LocalDate priorityDate;

    public ResponseSummaryTimeRange(LocalDateTime startInclusive,
                                    LocalDateTime endExclusive,
                                    LocalDate priorityDate) {
        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
        this.priorityDate = priorityDate;
    }

    public LocalDateTime getStartInclusive() {
        return startInclusive;
    }

    public LocalDateTime getEndExclusive() {
        return endExclusive;
    }

    public LocalDate getPriorityDate() {
        return priorityDate;
    }

    public boolean isEmpty() {
        return startInclusive == null || endExclusive == null
                || !startInclusive.isBefore(endExclusive);
    }
}
