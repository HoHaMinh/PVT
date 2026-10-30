package com.hoaphat.pvt.model.dto.price;

public class PriceLookupBatchConfirmationResult {
    private int completedCount;
    private int failedCount;
    private int skippedCount;

    public void completed() {
        completedCount++;
    }

    public void failed() {
        failedCount++;
    }

    public void skipped() {
        skippedCount++;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public int getProcessedCount() {
        return completedCount + failedCount;
    }
}
