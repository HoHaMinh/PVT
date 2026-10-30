package com.hoaphat.pvt.service.debt;

import com.hoaphat.pvt.config.DebtAlertProperties;
import com.hoaphat.pvt.model.debt.*;
import com.hoaphat.pvt.model.dto.debt.*;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.debt.IDebtImportAlertAcknowledgementRepository;
import com.hoaphat.pvt.repository.debt.IDebtAlertSyncRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DebtAlertStoreService {
    private static final int MAX_EMPLOYEES = 500;
    private static final int MAX_ALERTS = 10000;
    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_DISPLAY = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final IDebtAlertSyncRepository syncRepository;
    private final IDebtImportAlertAcknowledgementRepository importAcknowledgementRepository;
    private final IAccountRepository accountRepository;
    private final DebtAlertProperties properties;
    private final Clock clock;

    public DebtAlertStoreService(IDebtAlertSyncRepository syncRepository,
                                 IDebtImportAlertAcknowledgementRepository importAcknowledgementRepository,
                                 IAccountRepository accountRepository,
                                 DebtAlertProperties properties,
                                 Clock clock) {
        this.syncRepository = syncRepository;
        this.importAcknowledgementRepository = importAcknowledgementRepository;
        this.accountRepository = accountRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean isCompletedToday() {
        Optional<DebtAlertSync> current = syncRepository.findByScanDate(today());
        return current.isPresent()
                && (current.get().getStatus() == DebtAlertSyncStatus.SUCCESS
                || current.get().getStatus() == DebtAlertSyncStatus.SKIPPED);
    }

    @Transactional
    public void markRunning() {
        DebtAlertSync sync = currentOrNew();
        sync.setStatus(DebtAlertSyncStatus.RUNNING);
        sync.setLastAttemptAt(now());
        sync.setMessage("Đang đọc dữ liệu công nợ từ Google Drive.");
        syncRepository.save(sync);
    }

    @Transactional
    public void saveFailure(String errorMessage) {
        DebtAlertSync sync = currentOrNew();
        sync.setStatus(DebtAlertSyncStatus.FAILED);
        sync.setLastAttemptAt(now());
        sync.setMessage(safe(errorMessage, 1000, "Không thể đồng bộ dữ liệu công nợ."));
        syncRepository.save(sync);
    }

    @Transactional
    public void saveResponse(DebtAlertApiResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Apps Script không trả về nội dung.");
        }
        LocalDate responseDate = parseRequiredDate(response.getScanDate(), "scanDate");
        if (!today().equals(responseDate)) {
            throw new IllegalArgumentException(
                    "Apps Script trả về ngày " + response.getScanDate() + " thay vì ngày hiện tại.");
        }

        DebtAlertSync sync = currentOrNew();
        sync.setLastAttemptAt(now());
        sync.setDaysOffset(response.getDaysOffset());

        if (response.isSkipped()) {
            sync.getEmployees().clear();
            sync.setTotalAlerts(0);
            sync.setStatus(DebtAlertSyncStatus.SKIPPED);
            sync.setLastSuccessAt(now());
            sync.setMessage(safe(firstText(response.getSkipReason(), response.getMessage()),
                    1000, "Hôm nay hệ thống công nợ không thực hiện quét."));
            syncRepository.save(sync);
            return;
        }

        List<DebtAlertEmployeePayload> employeePayloads = response.getEmployees() == null
                ? Collections.emptyList() : response.getEmployees();
        if (employeePayloads.size() > MAX_EMPLOYEES) {
            throw new IllegalArgumentException("Apps Script trả về quá nhiều nhân viên.");
        }

        int totalAlerts = 0;
        for (DebtAlertEmployeePayload employee : employeePayloads) {
            if (employee != null && employee.getAlerts() != null) {
                totalAlerts += employee.getAlerts().size();
            }
            if (employee != null && employee.getImportAlerts() != null) {
                totalAlerts += employee.getImportAlerts().size();
            }
            if (totalAlerts > MAX_ALERTS) {
                throw new IllegalArgumentException("Apps Script trả về quá nhiều dòng cảnh báo công nợ.");
            }
        }

        sync.getEmployees().clear();
        for (DebtAlertEmployeePayload payload : employeePayloads) {
            if (payload != null) {
                sync.addEmployee(toEmployeeSnapshot(payload));
            }
        }
        int ordinaryAlertCount = employeePayloads.stream()
                .filter(Objects::nonNull)
                .map(DebtAlertEmployeePayload::getAlerts)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
        sync.setTotalAlerts(ordinaryAlertCount);
        sync.setStatus(DebtAlertSyncStatus.SUCCESS);
        sync.setLastSuccessAt(now());
        sync.setMessage(safe(response.getMessage(), 1000,
                "Đã cập nhật dữ liệu công nợ từ Google Drive."));
        syncRepository.save(sync);
    }

    @Transactional(readOnly = true)
    public DebtAlertDashboard getDashboard(String accountName, boolean manager, boolean running) {
        DebtAlertDashboard dashboard = new DebtAlertDashboard();
        dashboard.setEnabled(properties.isEnabled());
        dashboard.setConfigured(properties.isConfigured());
        dashboard.setRunning(running);

        if (!properties.isEnabled()) {
            dashboard.setStatus("DISABLED");
            dashboard.setMessage("Chức năng cảnh báo công nợ chưa được bật.");
            return dashboard;
        }
        if (!properties.isConfigured()) {
            dashboard.setStatus("CONFIG_ERROR");
            dashboard.setMessage("Thiếu URL hoặc token Apps Script trong application.properties.");
            return dashboard;
        }

        Optional<DebtAlertSync> optionalSync = syncRepository.findByScanDate(today());
        if (!optionalSync.isPresent()) {
            dashboard.setStatus(running ? "RUNNING" : "WAITING");
            dashboard.setMessage(running
                    ? "Đang đọc dữ liệu công nợ từ Google Drive."
                    : "Máy chủ chưa hoàn tất quét công nợ hôm nay.");
            return dashboard;
        }

        DebtAlertSync sync = optionalSync.get();
        dashboard.setStatus(running ? "RUNNING" : sync.getStatus().name());
        dashboard.setScanDate(formatDate(sync.getScanDate()));
        dashboard.setLastAttemptAt(formatDateTime(sync.getLastAttemptAt()));
        dashboard.setLastSuccessAt(formatDateTime(sync.getLastSuccessAt()));
        dashboard.setMessage(sync.getMessage());

        String normalizedAccount = accountName == null ? "" : accountName.trim();
        List<DebtAlertEmployeeView> employeeViews = new ArrayList<>();
        List<DebtImportAlertView> importAlertViews = new ArrayList<>();
        int visibleAlerts = 0;
        for (DebtAlertEmployeeSnapshot employee : sync.getEmployees()) {
            if (manager || normalizedAccount.equals(employee.getAccountName())) {
                DebtAlertEmployeeView view = toEmployeeView(employee);
                visibleAlerts += view.getItems().size();
                employeeViews.add(view);
                for (DebtImportAlertItem item : employee.getImportAlerts()) {
                    importAlertViews.add(toImportAlertView(employee, item));
                }
            }
        }
        if (!importAlertViews.isEmpty()) {
            List<String> keys = new ArrayList<>();
            for (DebtImportAlertView view : importAlertViews) keys.add(view.getAlertKey());
            Set<String> acknowledgedKeys = new HashSet<>();
            for (DebtImportAlertAcknowledgement acknowledgement
                    : importAcknowledgementRepository.findByAlertKeyIn(keys)) {
                acknowledgedKeys.add(acknowledgement.getAlertKey());
            }
            importAlertViews.removeIf(view -> acknowledgedKeys.contains(view.getAlertKey()));
        }
        importAlertViews.sort(Comparator.comparing(DebtImportAlertView::getDaysRemaining,
                Comparator.nullsLast(Comparator.naturalOrder())));
        employeeViews.sort(Comparator.comparing(DebtAlertEmployeeView::getEmployeeName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        dashboard.setEmployees(employeeViews);
        dashboard.setAlertCount(visibleAlerts);
        dashboard.setImportAlerts(importAlertViews);
        dashboard.setImportAlertCount(importAlertViews.size());
        return dashboard;
    }

    @Transactional
    public void acknowledgeImportAlert(String rawAlertKey, String accountName, boolean manager) {
        if (!manager) {
            throw new AccessDeniedException("Chỉ quản lý được xác nhận đã báo kế toán.");
        }
        String alertKey = safe(rawAlertKey, 64, "").toLowerCase(Locale.ROOT);
        if (!alertKey.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Mã cảnh báo NK không hợp lệ.");
        }

        DebtAlertSync sync = syncRepository.findByScanDate(today())
                .orElseThrow(() -> new IllegalArgumentException("Chưa có dữ liệu cảnh báo công nợ hôm nay."));
        String normalizedAccount = safe(accountName, 100, "");
        boolean found = false;
        for (DebtAlertEmployeeSnapshot employee : sync.getEmployees()) {
            for (DebtImportAlertItem item : employee.getImportAlerts()) {
                if (alertKey.equals(item.getAlertKey())) {
                    found = true;
                    break;
                }
            }
            if (found) break;
        }
        if (!found) {
            throw new IllegalArgumentException("Cảnh báo NK không còn tồn tại trong dữ liệu hôm nay.");
        }

        if (importAcknowledgementRepository.findByAlertKey(alertKey).isPresent()) return;
        DebtImportAlertAcknowledgement acknowledgement = new DebtImportAlertAcknowledgement();
        acknowledgement.setAlertKey(alertKey);
        acknowledgement.setAcknowledgedBy(normalizedAccount.isEmpty() ? "unknown" : normalizedAccount);
        acknowledgement.setAcknowledgedAt(now());
        importAcknowledgementRepository.save(acknowledgement);
    }

    @Transactional(readOnly = true)
    public Set<String> getPendingImportAlertAccountNamesToday() {
        DebtAlertSync sync = syncRepository.findByScanDate(today()).orElse(null);
        if (sync == null || sync.getStatus() != DebtAlertSyncStatus.SUCCESS) {
            return Collections.emptySet();
        }

        Map<String, String> accountByAlertKey = new LinkedHashMap<>();
        for (DebtAlertEmployeeSnapshot employee : sync.getEmployees()) {
            String accountName = safe(employee.getAccountName(), 100, "");
            if (accountName.isEmpty()) continue;
            for (DebtImportAlertItem item : employee.getImportAlerts()) {
                if (item != null && item.getAlertKey() != null
                        && !item.getAlertKey().trim().isEmpty()) {
                    accountByAlertKey.put(item.getAlertKey(), accountName);
                }
            }
        }
        if (accountByAlertKey.isEmpty()) return Collections.emptySet();

        for (DebtImportAlertAcknowledgement acknowledgement
                : importAcknowledgementRepository.findByAlertKeyIn(accountByAlertKey.keySet())) {
            accountByAlertKey.remove(acknowledgement.getAlertKey());
        }
        return new LinkedHashSet<>(accountByAlertKey.values());
    }

    @Transactional(readOnly = true)
    public boolean hasPendingImportAlertForAccountToday(String accountName) {
        String normalized = accountName == null ? "" : accountName.trim();
        if (normalized.isEmpty()) return false;
        for (String pendingAccount : getPendingImportAlertAccountNamesToday()) {
            if (normalized.equalsIgnoreCase(pendingAccount)) return true;
        }
        return false;
    }

    private DebtAlertEmployeeSnapshot toEmployeeSnapshot(DebtAlertEmployeePayload payload) {
        DebtAlertEmployeeSnapshot snapshot = new DebtAlertEmployeeSnapshot();
        String accountName = safe(payload.getAccountName(), 100, "");
        snapshot.setAccountName(accountName);
        snapshot.setEmployeeName(safe(payload.getEmployeeName(), 255,
                accountName.isEmpty() ? "Không rõ nhân viên" : accountName));
        snapshot.setSpreadsheetId(safe(payload.getSpreadsheetId(), 200, ""));
        snapshot.setSpreadsheetUrl(safe(payload.getSpreadsheetUrl(), 500, ""));
        snapshot.setReportSheetName(safe(payload.getReportSheetName(), 255, ""));
        snapshot.setReportBatch(safe(payload.getReportBatch(), 100, ""));
        snapshot.setReportMonth(safe(payload.getReportMonth(), 50, ""));

        String status = safe(payload.getStatus(), 40, "OK").toUpperCase(Locale.ROOT);
        String message = safe(payload.getMessage(), 1000, "");
        if (accountName.isEmpty()) {
            status = "MISSING_ACCOUNT";
            message = "Thiếu Bravo ID/account_name tại sheet Danh sach nhan vien.";
        } else if (accountRepository.findByAccountName(accountName) == null) {
            status = "ACCOUNT_NOT_FOUND";
            message = "Bravo ID " + accountName + " chưa tồn tại trong database PVT.";
        }
        snapshot.setResultStatus(status);
        snapshot.setMessage(message);

        List<DebtAlertItemPayload> alerts = payload.getAlerts() == null
                ? Collections.emptyList() : payload.getAlerts();
        for (DebtAlertItemPayload alert : alerts) {
            if (alert != null) {
                snapshot.addItem(toItem(alert));
            }
        }
        List<DebtImportAlertPayload> importAlerts = payload.getImportAlerts() == null
                ? Collections.emptyList() : payload.getImportAlerts();
        for (DebtImportAlertPayload alert : importAlerts) {
            if (alert != null) snapshot.addImportAlert(toImportAlert(alert));
        }
        return snapshot;
    }

    private DebtImportAlertItem toImportAlert(DebtImportAlertPayload payload) {
        String alertKey = safe(payload.getAlertKey(), 64, "").toLowerCase(Locale.ROOT);
        if (!alertKey.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Apps Script trả về alertKey NK không hợp lệ.");
        }
        DebtImportAlertItem item = new DebtImportAlertItem();
        item.setAlertKey(alertKey);
        item.setSection(safe(payload.getSection(), 30, ""));
        item.setItemOrder(safe(payload.getItemOrder(), 50, ""));
        item.setSupplier(safe(payload.getSupplier(), 500, ""));
        item.setInvoiceNumber(safe(payload.getInvoiceNumber(), 200, ""));
        item.setRemainingAmountDisplay(safe(payload.getRemainingAmountDisplay(), 200, ""));
        item.setBillOfLadingDate(parseOptionalDate(payload.getBillOfLadingDate()));
        item.setCustomsDeclarationDate(parseOptionalDate(payload.getCustomsDeclarationDate()));
        LocalDate deadline = parseOptionalDate(payload.getRegulatoryDeadline());
        if (deadline == null) {
            throw new IllegalArgumentException("Apps Script trả về regulatoryDeadline NK không hợp lệ.");
        }
        item.setRegulatoryDeadline(deadline);
        item.setDaysRemaining((int) ChronoUnit.DAYS.between(today(), deadline));
        return item;
    }

    private DebtAlertItem toItem(DebtAlertItemPayload payload) {
        DebtAlertItem item = new DebtAlertItem();
        item.setSection(safe(payload.getSection(), 30, ""));
        item.setItemOrder(safe(payload.getItemOrder(), 50, ""));
        item.setSupplier(safe(payload.getSupplier(), 500, ""));
        item.setInvoiceNumber(safe(payload.getInvoiceNumber(), 200, ""));
        item.setAmountDisplay(safe(payload.getAmountDisplay(), 200, ""));
        LocalDate dueDate = parseOptionalDate(payload.getDueDate());
        item.setDueDate(dueDate);
        // Tự tính lại từ ngày đến hạn để không phụ thuộc cách Jackson/Apps Script
        // đặt tên field và luôn khớp đúng ngày nghiệp vụ của PVT.
        Integer tPlus = payload.getTPlus();
        if (dueDate != null) {
            tPlus = (int) ChronoUnit.DAYS.between(dueDate, today());
        }
        item.setTPlus(tPlus);
        return item;
    }

    private DebtAlertEmployeeView toEmployeeView(DebtAlertEmployeeSnapshot employee) {
        DebtAlertEmployeeView view = new DebtAlertEmployeeView();
        view.setAccountName(employee.getAccountName());
        view.setEmployeeName(employee.getEmployeeName());
        view.setSpreadsheetUrl(employee.getSpreadsheetUrl());
        view.setStatus(employee.getResultStatus());
        view.setMessage(employee.getMessage());
        String batch = safe(employee.getReportBatch(), 100, "");
        String month = safe(employee.getReportMonth(), 50, "");
        if (!month.isEmpty() && !month.toLowerCase(Locale.ROOT).startsWith("tháng")) {
            month = "Tháng " + month;
        }
        String reportLabel = (batch + " " + month).trim();
        if (reportLabel.isEmpty()) {
            reportLabel = employee.getReportSheetName();
        }
        view.setReportLabel(reportLabel);

        List<DebtAlertItemView> items = new ArrayList<>();
        for (DebtAlertItem item : employee.getItems()) {
            items.add(toItemView(item));
        }
        items.sort(Comparator.comparing(DebtAlertItemView::getTPlus,
                Comparator.nullsLast(Comparator.reverseOrder())));
        view.setItems(items);
        return view;
    }

    private DebtAlertItemView toItemView(DebtAlertItem item) {
        DebtAlertItemView view = new DebtAlertItemView();
        view.setSection(item.getSection());
        view.setItemOrder(item.getItemOrder());
        view.setSupplier(item.getSupplier());
        view.setInvoiceNumber(item.getInvoiceNumber());
        view.setAmountDisplay(item.getAmountDisplay());
        view.setDueDate(formatDate(item.getDueDate()));
        view.setTPlus(item.getTPlus());
        if (item.getTPlus() == null) {
            view.setTPlusLabel("Không rõ");
            view.setTPlusClass("text-muted");
        } else if (item.getTPlus() > 0) {
            view.setTPlusLabel("+" + item.getTPlus() + " (Trễ " + item.getTPlus() + " ngày)");
            view.setTPlusClass("text-danger");
        } else if (item.getTPlus() == 0) {
            view.setTPlusLabel("Hôm nay");
            view.setTPlusClass("text-warning");
        } else {
            view.setTPlusLabel(item.getTPlus() + " (Còn " + Math.abs(item.getTPlus()) + " ngày)");
            view.setTPlusClass("text-success");
        }
        return view;
    }

    private DebtImportAlertView toImportAlertView(DebtAlertEmployeeSnapshot employee,
                                                   DebtImportAlertItem item) {
        DebtImportAlertView view = new DebtImportAlertView();
        view.setAlertKey(item.getAlertKey());
        view.setAccountName(employee.getAccountName());
        view.setEmployeeName(employee.getEmployeeName());
        view.setSection(item.getSection());
        view.setItemOrder(item.getItemOrder());
        view.setSupplier(item.getSupplier());
        view.setInvoiceNumber(item.getInvoiceNumber());
        view.setRemainingAmountDisplay(item.getRemainingAmountDisplay());
        view.setBillOfLadingDate(formatDate(item.getBillOfLadingDate()));
        view.setCustomsDeclarationDate(formatDate(item.getCustomsDeclarationDate()));
        view.setRegulatoryDeadline(formatDate(item.getRegulatoryDeadline()));
        Integer daysRemaining = item.getRegulatoryDeadline() == null ? null
                : (int) ChronoUnit.DAYS.between(today(), item.getRegulatoryDeadline());
        view.setDaysRemaining(daysRemaining);
        if (daysRemaining == null) {
            view.setDeadlineLabel("Không xác định được hạn");
            view.setDeadlineClass("text-muted");
        } else if (daysRemaining < 0) {
            view.setDeadlineLabel("Đã quá hạn " + Math.abs(daysRemaining) + " ngày");
            view.setDeadlineClass("text-danger");
        } else if (daysRemaining == 0) {
            view.setDeadlineLabel("Hết hạn hôm nay");
            view.setDeadlineClass("text-danger");
        } else {
            view.setDeadlineLabel("Còn " + daysRemaining + " ngày");
            view.setDeadlineClass(daysRemaining <= 3 ? "text-danger" : "text-warning");
        }
        return view;
    }

    private DebtAlertSync currentOrNew() {
        return syncRepository.findByScanDate(today()).orElseGet(() -> {
            DebtAlertSync sync = new DebtAlertSync();
            sync.setScanDate(today());
            sync.setStatus(DebtAlertSyncStatus.RUNNING);
            sync.setLastAttemptAt(now());
            sync.setTotalAlerts(0);
            return sync;
        });
    }

    private LocalDate parseRequiredDate(String value, String field) {
        LocalDate parsed = parseOptionalDate(value);
        if (parsed == null) {
            throw new IllegalArgumentException("Apps Script trả về " + field + " không hợp lệ.");
        }
        return parsed;
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.format(DATE_DISPLAY);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_DISPLAY);
    }

    private String safe(String value, int maxLength, String fallback) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            normalized = fallback == null ? "" : fallback;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String firstText(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first : second;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }
}
