package com.hoaphat.pvt.model.reminder;

public class PersonalReminderView {
    private final boolean priceLookupPending;
    private final boolean importAlertPending;

    public PersonalReminderView(boolean priceLookupPending, boolean importAlertPending) {
        this.priceLookupPending = priceLookupPending;
        this.importAlertPending = importAlertPending;
    }

    public static PersonalReminderView empty() {
        return new PersonalReminderView(false, false);
    }

    public boolean isPriceLookupPending() { return priceLookupPending; }
    public boolean isImportAlertPending() { return importAlertPending; }
    public boolean hasAny() { return priceLookupPending || importAlertPending; }
}
