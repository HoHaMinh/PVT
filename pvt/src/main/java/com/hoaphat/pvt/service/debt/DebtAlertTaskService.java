package com.hoaphat.pvt.service.debt;

import com.hoaphat.pvt.config.DebtAlertProperties;
import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.debt.DebtAlertApiResponse;
import com.hoaphat.pvt.model.dto.debt.DebtAlertEmployeePayload;
import com.hoaphat.pvt.model.dto.debt.DebtAlertItemPayload;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DebtAlertTaskService {
    static final String AUTOMATION_SOURCE = "DEBT_ALERT";

    private final IMonthEventRepository monthEventRepository;
    private final IAccountRepository accountRepository;
    private final DebtAlertProperties properties;
    private final Clock clock;

    public DebtAlertTaskService(IMonthEventRepository monthEventRepository,
                                IAccountRepository accountRepository,
                                DebtAlertProperties properties,
                                Clock clock) {
        this.monthEventRepository = monthEventRepository;
        this.accountRepository = accountRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Đồng bộ đúng một công việc cá nhân/account/ngày. Quét lại chỉ cập nhật
     * công việc đã sinh, không tạo thêm bản trùng.
     */
    @Transactional
    public int synchronizeDailyTasks(DebtAlertApiResponse response) {
        if (response == null || response.getEmployees() == null) {
            return 0;
        }

        LocalDate taskDate = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        LocalDateTime deadline = deadlineFor(taskDate, now);
        int changed = 0;

        for (DebtAlertEmployeePayload employee : response.getEmployees()) {
            if (employee == null || employee.getAccountName() == null
                    || employee.getAccountName().trim().isEmpty()) {
                continue;
            }
            String resultStatus = text(employee.getStatus());
            if (!resultStatus.isEmpty() && !"OK".equalsIgnoreCase(resultStatus)) {
                // Không xóa/cập nhật lịch cũ khi file Google đang lỗi, bị trùng
                // report hoặc không đọc được. Chỉ dữ liệu quét OK mới là nguồn tin cậy.
                continue;
            }
            String accountName = employee.getAccountName().trim();
            Account account = accountRepository.findByAccountName(accountName);
            if (account == null) {
                continue;
            }

            List<MonthEvent> existingTasks = monthEventRepository
                    .findActiveAutomationTasksForAccount(accountName, AUTOMATION_SOURCE);
            MonthEvent existing = existingTasks.isEmpty() ? null : existingTasks.get(0);
            for (int i = 1; i < existingTasks.size(); i++) {
                MonthEvent duplicate = existingTasks.get(i);
                duplicate.setDeleted(true);
                monthEventRepository.save(duplicate);
                changed++;
            }
            String description = buildDescription(employee.getAlerts(), taskDate);

            if (description.isEmpty()) {
                if (existing != null && !Boolean.TRUE.equals(existing.getDeleted())) {
                    existing.setDeleted(true);
                    monthEventRepository.save(existing);
                    changed++;
                }
                continue;
            }

            if (existing != null) {
                MonthEvent event = existing;
                // Manager đã chủ động xóa mềm thì tôn trọng quyết định đó; không
                // tự phục hồi khi bấm quét lại trong cùng ngày.
                if (Boolean.TRUE.equals(event.getDeleted())) {
                    continue;
                }
                event.setMonthEventDescription(description);
                event.setMonthEventDeadline(deadline);
                event.setAutomationDate(taskDate);
                event.setRegisteredDay(now);
                event.setHidden(false);
                monthEventRepository.save(event);
                changed++;
                continue;
            }

            MonthEvent event = new MonthEvent();
            event.setMonthEventDescription(description);
            event.setMonthEventDeadline(deadline);
            event.setMonthEventStatus(0);
            event.setResponseStatus(0);
            event.setExtendDay(null);
            event.setLastTimeResponse(LocalDateTime.of(2023, 1, 1, 0, 0));
            event.setLastPersonResponse(null);
            event.setRegisteredDay(now);
            event.setAccount(account);
            event.setAll(false);
            event.setHidden(false);
            event.setDeleted(false);
            event.setAutomationSource(AUTOMATION_SOURCE);
            event.setAutomationDate(taskDate);
            monthEventRepository.save(event);
            changed++;
        }
        return changed;
    }

    LocalDateTime deadlineFor(LocalDate taskDate, LocalDateTime now) {
        int deadlineHour = Math.max(0, Math.min(23, properties.getTaskDeadlineHour()));
        if (taskDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return taskDate.plusDays(1).atTime(deadlineHour, 0);
        }
        if (taskDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
            LocalDateTime saturdayDeadline = taskDate.atTime(11, 0);
            if (now != null && !now.isBefore(saturdayDeadline)) {
                return taskDate.plusDays(2).atTime(deadlineHour, 0);
            }
            return saturdayDeadline;
        }
        return taskDate.atTime(deadlineHour, 0);
    }

    String buildDescription(List<DebtAlertItemPayload> alerts, LocalDate today) {
        if (alerts == null || alerts.isEmpty()) {
            return "";
        }

        // Key là số ngày từ hôm nay đến hạn: 4 => T+4; -2 => quá hạn 2 ngày.
        SortedMap<Integer, List<String>> grouped = new TreeMap<>();
        for (DebtAlertItemPayload alert : alerts) {
            if (alert == null) {
                continue;
            }
            Integer daysUntilDue = daysUntilDue(alert, today);
            if (daysUntilDue == null) {
                continue;
            }
            grouped.computeIfAbsent(daysUntilDue, ignored -> new ArrayList<>())
                    .add(invoiceLabel(alert));
        }
        if (grouped.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder("Thanh toán ");
        boolean firstGroup = true;
        for (Map.Entry<Integer, List<String>> entry : grouped.entrySet()) {
            if (!firstGroup) {
                result.append(", ");
            }
            int days = entry.getKey();
            if (days < 0) {
                result.append("Quá hạn ").append(Math.abs(days)).append(" ngày");
            } else {
                result.append("T+").append(days);
            }
            result.append(": ").append(String.join(", ", entry.getValue()));
            firstGroup = false;
        }
        return result.toString();
    }

    private Integer daysUntilDue(DebtAlertItemPayload alert, LocalDate today) {
        if (alert.getDueDate() != null && !alert.getDueDate().trim().isEmpty()) {
            try {
                LocalDate dueDate = LocalDate.parse(alert.getDueDate().trim());
                return (int) ChronoUnit.DAYS.between(today, dueDate);
            } catch (DateTimeParseException ignored) {
                // Dùng tPlus từ Apps Script làm fallback bên dưới.
            }
        }
        return alert.getTPlus() == null ? null : -alert.getTPlus();
    }

    private String invoiceLabel(DebtAlertItemPayload alert) {
        String supplier = text(alert.getSupplier());
        String invoice = text(alert.getInvoiceNumber());
        if (supplier.isEmpty()) {
            supplier = "Không rõ nhà cung cấp";
        }
        return invoice.isEmpty() ? supplier : supplier + " (" + invoice + ")";
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
