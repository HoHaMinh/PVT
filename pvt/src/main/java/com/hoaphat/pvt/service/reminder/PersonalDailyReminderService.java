package com.hoaphat.pvt.service.reminder;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.reminder.PersonalDailyReminder;
import com.hoaphat.pvt.model.reminder.PersonalReminderView;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.reminder.IPersonalDailyReminderRepository;
import com.hoaphat.pvt.service.debt.DebtAlertStoreService;
import com.hoaphat.pvt.service.price.PriceLookupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class PersonalDailyReminderService {
    private static final long RETENTION_DAYS = 90L;

    private final IPersonalDailyReminderRepository reminderRepository;
    private final IAccountRepository accountRepository;
    private final PriceLookupService priceLookupService;
    private final DebtAlertStoreService debtAlertStoreService;
    private final Clock clock;

    public PersonalDailyReminderService(IPersonalDailyReminderRepository reminderRepository,
                                        IAccountRepository accountRepository,
                                        PriceLookupService priceLookupService,
                                        DebtAlertStoreService debtAlertStoreService,
                                        Clock clock) {
        this.reminderRepository = reminderRepository;
        this.accountRepository = accountRepository;
        this.priceLookupService = priceLookupService;
        this.debtAlertStoreService = debtAlertStoreService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean isScannedToday() {
        return reminderRepository.existsByReminderDate(today());
    }

    /**
     * Chụp trạng thái đúng một lần trong ngày. Nội dung phát sinh sau thời điểm này
     * chỉ được đưa vào snapshot của ngày kế tiếp.
     */
    @Transactional
    public boolean scanToday() {
        LocalDate today = today();
        if (reminderRepository.existsByReminderDate(today)) return false;

        Set<String> priceAccounts = normalize(priceLookupService.getPendingAccountNames());
        Set<String> importAccounts = normalize(
                debtAlertStoreService.getPendingImportAlertAccountNamesToday());
        LocalDateTime scannedAt = LocalDateTime.now(clock);
        List<PersonalDailyReminder> rows = new ArrayList<>();

        for (Account account : accountRepository.findAll()) {
            if (account == null || account.getAccountName() == null
                    || account.getAccountName().trim().isEmpty()) {
                continue;
            }
            PersonalDailyReminder row = new PersonalDailyReminder();
            row.setReminderDate(today);
            row.setAccountName(account.getAccountName().trim());
            String key = normalize(account.getAccountName());
            row.setPriceLookupPending(priceAccounts.contains(key));
            row.setImportAlertPending(importAccounts.contains(key));
            row.setScannedAt(scannedAt);
            rows.add(row);
        }

        reminderRepository.saveAll(rows);
        reminderRepository.deleteByReminderDateBefore(today.minusDays(RETENTION_DAYS));
        return true;
    }

    /**
     * Chỉ đọc snapshot của chính account đăng nhập. Kiểm tra trạng thái hiện tại
     * chỉ dùng để tắt một lời nhắc đã xử lý, không được bật lời nhắc mới trong ngày.
     */
    @Transactional
    public PersonalReminderView getTodayForAccount(String accountName) {
        String normalizedAccount = accountName == null ? "" : accountName.trim();
        if (normalizedAccount.isEmpty()) return PersonalReminderView.empty();

        Optional<PersonalDailyReminder> optional = reminderRepository
                .findByReminderDateAndAccountName(today(), normalizedAccount);
        if (!optional.isPresent()) return PersonalReminderView.empty();

        PersonalDailyReminder snapshot = optional.get();
        boolean changed = false;
        if (snapshot.isPriceLookupPending()
                && !priceLookupService.hasPendingForAccount(normalizedAccount)) {
            snapshot.setPriceLookupPending(false);
            changed = true;
        }
        if (snapshot.isImportAlertPending()
                && !debtAlertStoreService.hasPendingImportAlertForAccountToday(normalizedAccount)) {
            snapshot.setImportAlertPending(false);
            changed = true;
        }
        if (changed) reminderRepository.save(snapshot);
        return new PersonalReminderView(snapshot.isPriceLookupPending(),
                snapshot.isImportAlertPending());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private Set<String> normalize(Set<String> values) {
        Set<String> result = new HashSet<>();
        if (values == null) return result;
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isEmpty()) result.add(normalized);
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
