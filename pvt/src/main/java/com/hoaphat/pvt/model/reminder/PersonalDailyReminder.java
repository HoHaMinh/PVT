package com.hoaphat.pvt.model.reminder;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_daily_reminder",
        uniqueConstraints = @UniqueConstraint(name = "uk_personal_reminder_date_account",
                columnNames = {"reminder_date", "account_name"}),
        indexes = @Index(name = "idx_personal_reminder_account_date",
                columnList = "account_name,reminder_date"))
public class PersonalDailyReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reminderId;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "price_lookup_pending", nullable = false)
    private boolean priceLookupPending;

    @Column(name = "import_alert_pending", nullable = false)
    private boolean importAlertPending;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    public Integer getReminderId() { return reminderId; }
    public void setReminderId(Integer reminderId) { this.reminderId = reminderId; }
    public LocalDate getReminderDate() { return reminderDate; }
    public void setReminderDate(LocalDate reminderDate) { this.reminderDate = reminderDate; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public boolean isPriceLookupPending() { return priceLookupPending; }
    public void setPriceLookupPending(boolean priceLookupPending) {
        this.priceLookupPending = priceLookupPending;
    }
    public boolean isImportAlertPending() { return importAlertPending; }
    public void setImportAlertPending(boolean importAlertPending) {
        this.importAlertPending = importAlertPending;
    }
    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }
}
