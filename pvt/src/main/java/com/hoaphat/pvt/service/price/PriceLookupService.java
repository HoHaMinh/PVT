package com.hoaphat.pvt.service.price;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.price.*;
import com.hoaphat.pvt.model.price.*;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.price.IPriceLookupBatchRepository;
import com.hoaphat.pvt.repository.price.IPriceLookupItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PriceLookupService {
    private static final int MAX_ITEMS_PER_SUBMISSION = 100;
    private static final int API_BATCH_SIZE = 20;
    private static final int MAX_SELECTED_NAMES = 12;
    private static final int MAX_BATCH_CONFIRMATION_ITEMS = 500;
    private static final Pattern INPUT_LINE = Pattern.compile(
            "^(.*)(?:\\t+| {3,})(.*?)(?:\\t+| {3,})([A-Za-z0-9._-]+)\\s*$");
    private static final List<PriceLookupStatus> PENDING_STATUSES = Arrays.asList(
            PriceLookupStatus.PENDING_CONFIRMATION, PriceLookupStatus.LOOKUP_FAILED);
    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PriceLookupAppsScriptClient appsScriptClient;
    private final IPriceLookupBatchRepository batchRepository;
    private final IPriceLookupItemRepository itemRepository;
    private final IAccountRepository accountRepository;

    public PriceLookupService(PriceLookupAppsScriptClient appsScriptClient,
                              IPriceLookupBatchRepository batchRepository,
                              IPriceLookupItemRepository itemRepository,
                              IAccountRepository accountRepository) {
        this.appsScriptClient = appsScriptClient;
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.accountRepository = accountRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void repairPreviouslyParsedDisplayNames() {
        List<PriceLookupItem> changed = new ArrayList<>();
        for (PriceLookupItem item : itemRepository.findAll()) {
            ParsedLine parsed = parseLine(item.getOriginalLine(), "repair");
            if (parsed != null && !parsed.displayName.equals(item.getDisplayName())) {
                item.setDisplayName(parsed.displayName);
                changed.add(item);
            }
        }
        if (!changed.isEmpty()) itemRepository.saveAll(changed);
    }

    @Transactional
    public int createBatch(String rawInput, String createdBy) {
        List<ParsedLine> lines = parseLines(rawInput);
        Map<String, Account> accounts = loadAccounts();
        List<String> missingAccounts = new ArrayList<>();
        for (ParsedLine line : lines) {
            if (!accounts.containsKey(normalizeAccountKey(line.accountName))) {
                missingAccounts.add(line.accountName);
            }
        }
        if (!missingAccounts.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy ID đăng nhập: "
                    + String.join(", ", new LinkedHashSet<>(missingAccounts)) + ".");
        }

        Map<String, PriceSearchResultPayload> resultById = new LinkedHashMap<>();
        for (int start = 0; start < lines.size(); start += API_BATCH_SIZE) {
            int end = Math.min(start + API_BATCH_SIZE, lines.size());
            List<PriceSearchQuery> queries = new ArrayList<>();
            for (int index = start; index < end; index++) {
                ParsedLine line = lines.get(index);
                queries.add(new PriceSearchQuery(line.externalId, line.keyword));
            }
            PriceSearchApiResponse response = appsScriptClient.search(queries);
            if (response.getResults() != null) {
                for (PriceSearchResultPayload result : response.getResults()) {
                    resultById.put(result.getId(), result);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        PriceLookupBatch batch = new PriceLookupBatch();
        batch.setCreatedBy(createdBy);
        batch.setCreatedAt(now);
        batch.setItemCount(lines.size());

        for (ParsedLine line : lines) {
            Account account = accounts.get(normalizeAccountKey(line.accountName));
            PriceLookupItem item = new PriceLookupItem();
            item.setDisplayName(line.displayName);
            item.setKeyword(line.keyword);
            item.setOriginalLine(line.originalLine);
            item.setAssignedAccountName(account.getAccountName());
            item.setAssignedDisplayName(account.getDisplayName());
            item.setStatus(PriceLookupStatus.PENDING_CONFIRMATION);
            item.setCreatedAt(now);

            PriceSearchResultPayload searchResult = resultById.get(line.externalId);
            Set<String> addedNames = new LinkedHashSet<>();
            if (searchResult != null && searchResult.getCandidates() != null) {
                for (PriceCandidatePayload payload : searchResult.getCandidates()) {
                    String name = trim(payload.getName());
                    if (name.isEmpty() || !addedNames.add(name)) continue;
                    PriceLookupCandidate candidate = new PriceLookupCandidate();
                    candidate.setMaterialName(name);
                    candidate.setStartRow(valueOrZero(payload.getStartRow()));
                    candidate.setRowCount(valueOrZero(payload.getRowCount()));
                    item.addCandidate(candidate);
                }
            }
            if (item.getCandidates().isEmpty()) {
                item.setStatusMessage("Không tìm thấy tên vật tư phù hợp, nên thử với keyword khác");
            }
            batch.addItem(item);
        }
        batchRepository.save(batch);
        return lines.size();
    }

    @Transactional(readOnly = true)
    public List<PriceLookupItem> getPending(String currentAccount, boolean manager, String selectedAccount) {
        String filter = resolveAccountFilter(currentAccount, manager, selectedAccount);
        return filter.isEmpty()
                ? itemRepository.findByStatusInOrderByCreatedAtAsc(PENDING_STATUSES)
                : itemRepository.findByStatusInAndAssignedAccountNameOrderByCreatedAtAsc(PENDING_STATUSES, filter);
    }

    @Transactional(readOnly = true)
    public Set<String> getPendingAccountNames() {
        return new LinkedHashSet<>(
                itemRepository.findDistinctAssignedAccountNamesByStatusIn(PENDING_STATUSES));
    }

    @Transactional(readOnly = true)
    public boolean hasPendingForAccount(String accountName) {
        String normalized = accountName == null ? "" : accountName.trim();
        return !normalized.isEmpty()
                && itemRepository.existsByStatusInAndAssignedAccountName(
                PENDING_STATUSES, normalized);
    }

    @Transactional
    public String confirm(Integer itemId, List<Integer> selectedCandidateIds,
                          String currentAccount, boolean manager) {
        PriceLookupItem item = loadConfirmableItem(itemId, currentAccount, manager);
        return confirmLoadedItem(item, selectedCandidateIds, currentAccount);
    }

    @Transactional
    public PriceLookupBatchConfirmationResult confirmBatch(List<PriceLookupConfirmationRow> rows,
                                                            String currentAccount,
                                                            boolean manager) {
        PriceLookupBatchConfirmationResult result = new PriceLookupBatchConfirmationResult();
        if (rows == null || rows.isEmpty()) return result;
        if (rows.size() > MAX_BATCH_CONFIRMATION_ITEMS) {
            throw new IllegalArgumentException("Mỗi lần chỉ được xác nhận tối đa "
                    + MAX_BATCH_CONFIRMATION_ITEMS + " hạng mục.");
        }

        Set<Integer> handledItemIds = new LinkedHashSet<>();
        for (PriceLookupConfirmationRow row : rows) {
            if (row == null || row.getItemId() == null) {
                result.skipped();
                continue;
            }
            if (!handledItemIds.add(row.getItemId())) {
                throw new IllegalArgumentException("Danh sách xác nhận có hạng mục bị lặp lại.");
            }

            PriceLookupItem item = loadConfirmableItem(row.getItemId(), currentAccount, manager);
            List<Integer> selectedIds = row.getSelectedCandidateIds() == null
                    ? Collections.emptyList() : row.getSelectedCandidateIds();
            if (row.isNoSuitableCandidate() && !selectedIds.isEmpty()) {
                throw new IllegalArgumentException("Không thể vừa chọn tên vật tư vừa chọn "
                        + "không có tên đề xuất nào phù hợp.");
            }

            if (item.getCandidates().isEmpty()) {
                if (!selectedIds.isEmpty()) {
                    throw new IllegalArgumentException("Hạng mục không có tên vật tư đề xuất để lựa chọn.");
                }
                completeWithoutPrice(item, currentAccount);
                result.completed();
            } else if (row.isNoSuitableCandidate()) {
                completeWithoutPrice(item, currentAccount);
                result.completed();
            } else if (selectedIds.isEmpty()) {
                result.skipped();
            } else {
                String error = confirmLoadedItem(item, selectedIds, currentAccount);
                if (error.isEmpty()) result.completed();
                else result.failed();
            }
        }
        return result;
    }

    private String confirmLoadedItem(PriceLookupItem item, List<Integer> selectedCandidateIds,
                                     String currentAccount) {
        Integer itemId = item.getItemId();

        Set<Integer> selectedIds = selectedCandidateIds == null
                ? Collections.emptySet() : new LinkedHashSet<>(selectedCandidateIds);
        if (selectedIds.isEmpty()) throw new IllegalArgumentException("Hãy chọn ít nhất một tên vật tư.");
        if (selectedIds.size() > MAX_SELECTED_NAMES) {
            throw new IllegalArgumentException("Mỗi hạng mục chỉ được chọn tối đa " + MAX_SELECTED_NAMES + " tên vật tư.");
        }

        List<PriceSelectedCandidate> selected = new ArrayList<>();
        for (PriceLookupCandidate candidate : item.getCandidates()) {
            boolean chosen = selectedIds.contains(candidate.getCandidateId());
            candidate.setSelected(chosen);
            if (chosen) {
                selected.add(new PriceSelectedCandidate(candidate.getMaterialName(),
                        candidate.getStartRow(), candidate.getRowCount()));
            }
        }
        if (selected.size() != selectedIds.size()) {
            throw new IllegalArgumentException("Danh sách lựa chọn có tên vật tư không thuộc hạng mục này.");
        }

        PriceLookupQuery query = new PriceLookupQuery("item-" + itemId, item.getKeyword(), selected);
        try {
            PriceLookupApiResponse response = appsScriptClient.lookup(Collections.singletonList(query));
            PriceLookupResultPayload payload = findLookupResult(response, query.getId());
            item.getResults().clear();
            List<PriceResultGroupPayload> groups = payload.getGroups() == null
                    ? Collections.emptyList() : payload.getGroups();
            for (int index = 0; index < groups.size(); index++) {
                PriceResultGroupPayload group = groups.get(index);
                PriceLookupResult result = new PriceLookupResult();
                result.setGroupNumber(index + 1);
                result.setGroupName(trim(group.getGroup()));
                result.setFound(group.isFound());
                result.setMaterialName(group.isFound() ? trim(group.getMaterial()) : null);
                result.setUnitName(group.isFound() ? trim(group.getUnit()) : null);
                result.setPurchaseDate(group.isFound() ? parseApiDate(group.getDate()) : null);
                result.setPurchasePrice(group.isFound() ? group.getPrice() : null);
                result.setSupplierName(group.isFound() ? trim(group.getSupplier()) : null);
                item.addResult(result);
            }
            item.setStatus(PriceLookupStatus.COMPLETED);
            item.setStatusMessage(null);
            item.setConfirmedAt(LocalDateTime.now());
            item.setCompletedAt(LocalDateTime.now());
            item.setConfirmedBy(currentAccount);
            itemRepository.save(item);
            return "";
        } catch (RuntimeException e) {
            item.setStatus(PriceLookupStatus.LOOKUP_FAILED);
            item.setStatusMessage(shortMessage(e));
            itemRepository.save(item);
            return item.getStatusMessage();
        }
    }

    @Transactional
    public void dismissNoMatch(Integer itemId, String currentAccount, boolean manager) {
        PriceLookupItem item = loadConfirmableItem(itemId, currentAccount, manager);
        if (!item.getCandidates().isEmpty()) {
            throw new IllegalArgumentException("Hạng mục này có tên vật tư để xác nhận, không thể bỏ qua theo cách này.");
        }
        completeWithoutPrice(item, currentAccount);
    }

    private PriceLookupItem loadConfirmableItem(Integer itemId, String currentAccount, boolean manager) {
        PriceLookupItem item = itemRepository.findWithDetailsByItemId(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Hạng mục cần xác nhận không còn tồn tại."));
        if (!manager && !Objects.equals(item.getAssignedAccountName(), currentAccount)) {
            throw new SecurityException("Bạn không có quyền xác nhận hạng mục của nhân viên khác.");
        }
        if (!PENDING_STATUSES.contains(item.getStatus())) {
            throw new IllegalArgumentException("Hạng mục này không còn ở trạng thái chờ xác nhận.");
        }
        return item;
    }

    private void completeWithoutPrice(PriceLookupItem item, String currentAccount) {
        for (PriceLookupCandidate candidate : item.getCandidates()) {
            candidate.setSelected(false);
        }
        item.getResults().clear();
        PriceLookupResult result = new PriceLookupResult();
        result.setGroupNumber(1);
        result.setGroupName("Kết quả tra giá");
        result.setFound(false);
        item.addResult(result);
        LocalDateTime now = LocalDateTime.now();
        item.setStatus(PriceLookupStatus.COMPLETED);
        item.setStatusMessage(null);
        item.setConfirmedAt(now);
        item.setCompletedAt(now);
        item.setConfirmedBy(currentAccount);
        itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public Page<PriceLookupItem> searchResults(String currentAccount, boolean manager,
                                               String selectedAccount, String search, int page) {
        String filter = resolveAccountFilter(currentAccount, manager, selectedAccount);
        String normalizedSearch = search == null ? "" : search.trim();
        return itemRepository.searchCompleted(PriceLookupStatus.COMPLETED, filter,
                normalizedSearch, PageRequest.of(Math.max(0, page), 20));
    }

    @Transactional(readOnly = true)
    public Page<PriceLookupItem> searchDeletedResults(boolean manager,
                                                      String selectedAccount,
                                                      String search, int page) {
        requireManager(manager);
        String filter = selectedAccount == null ? "" : selectedAccount.trim();
        String normalizedSearch = search == null ? "" : search.trim();
        return itemRepository.searchDeletedCompleted(PriceLookupStatus.COMPLETED, filter,
                normalizedSearch, PageRequest.of(Math.max(0, page), 20));
    }

    @Transactional
    public void moveResultToTrash(Integer itemId, String currentAccount, boolean manager) {
        PriceLookupItem item = loadCompletedItem(itemId);
        if (!manager && !Objects.equals(item.getAssignedAccountName(), currentAccount)) {
            throw new SecurityException("Bạn không có quyền xóa kết quả tra giá của nhân viên khác.");
        }
        if (item.isDeleted()) {
            throw new IllegalArgumentException("Kết quả tra giá đã nằm trong thư mục đã xóa.");
        }
        item.setDeleted(true);
        item.setDeletedAt(LocalDateTime.now());
        item.setDeletedBy(currentAccount == null ? "" : currentAccount.trim());
        itemRepository.save(item);
    }

    @Transactional
    public void restoreDeletedResult(Integer itemId, boolean manager) {
        requireManager(manager);
        PriceLookupItem item = loadCompletedItem(itemId);
        if (!item.isDeleted()) {
            throw new IllegalArgumentException("Kết quả tra giá không nằm trong thư mục đã xóa.");
        }
        item.setDeleted(false);
        item.setDeletedAt(null);
        item.setDeletedBy(null);
        itemRepository.save(item);
    }

    @Transactional
    public int emptyDeletedResults(boolean manager) {
        requireManager(manager);
        List<PriceLookupItem> deletedItems = itemRepository.findDeletedForUpdate();
        for (PriceLookupItem item : deletedItems) {
            itemRepository.delete(item);
        }
        return deletedItems.size();
    }

    private PriceLookupItem loadCompletedItem(Integer itemId) {
        if (itemId == null || itemId <= 0) {
            throw new IllegalArgumentException("Mã kết quả tra giá không hợp lệ.");
        }
        PriceLookupItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Kết quả tra giá không còn tồn tại."));
        if (item.getStatus() != PriceLookupStatus.COMPLETED) {
            throw new IllegalArgumentException("Hạng mục này chưa có kết quả tra giá hoàn chỉnh.");
        }
        return item;
    }

    private void requireManager(boolean manager) {
        if (!manager) {
            throw new SecurityException("Chỉ quản lý được sử dụng thư mục kết quả tra giá đã xóa.");
        }
    }

    private List<ParsedLine> parseLines(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Hãy nhập ít nhất một hạng mục cần tra giá.");
        }
        String[] rawLines = rawInput.split("\\r?\\n");
        List<ParsedLine> result = new ArrayList<>();
        List<Integer> invalidLines = new ArrayList<>();
        for (int index = 0; index < rawLines.length; index++) {
            String original = rawLines[index].trim();
            if (original.isEmpty()) continue;
            ParsedLine parsed = parseLine(original, "line-" + (index + 1));
            if (parsed == null) {
                invalidLines.add(index + 1);
                continue;
            }
            if (parsed.displayName.length() > 1000 || parsed.keyword.length() > 1000
                    || original.length() > 2000) {
                throw new IllegalArgumentException("Dòng " + (index + 1) + " quá dài.");
            }
            result.add(parsed);
        }
        if (!invalidLines.isEmpty()) {
            throw new IllegalArgumentException("Sai định dạng tại dòng " + joinNumbers(invalidLines)
                    + ". Mỗi dòng phải gồm: tên hiển thị, keyword tra giá, ID nhân viên; "
                    + "ngăn cách bằng phím Tab hoặc ít nhất 3 dấu cách.");
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Không có hạng mục hợp lệ để tra giá.");
        if (result.size() > MAX_ITEMS_PER_SUBMISSION) {
            throw new IllegalArgumentException("Mỗi lần cung cấp tối đa " + MAX_ITEMS_PER_SUBMISSION + " hạng mục.");
        }
        return result;
    }

    private ParsedLine parseLine(String originalLine, String externalId) {
        String original = trim(originalLine);
        if (original.isEmpty()) return null;
        Matcher matcher = INPUT_LINE.matcher(original);
        if (!matcher.matches()) return null;
        String displayName = matcher.group(1).trim();
        String keyword = matcher.group(2).trim();
        String accountName = matcher.group(3).trim();
        if (displayName.isEmpty() || keyword.isEmpty() || accountName.isEmpty()) return null;
        return new ParsedLine(externalId, displayName, keyword, accountName, original);
    }

    private Map<String, Account> loadAccounts() {
        Map<String, Account> result = new LinkedHashMap<>();
        for (Account account : accountRepository.findAll()) {
            result.put(normalizeAccountKey(account.getAccountName()), account);
        }
        return result;
    }

    private String normalizeAccountKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveAccountFilter(String currentAccount, boolean manager, String selectedAccount) {
        if (!manager) return currentAccount == null ? "" : currentAccount;
        return selectedAccount == null ? "" : selectedAccount.trim();
    }

    private PriceLookupResultPayload findLookupResult(PriceLookupApiResponse response, String id) {
        if (response.getResults() != null) {
            for (PriceLookupResultPayload result : response.getResults()) {
                if (id.equals(result.getId())) return result;
            }
        }
        throw new IllegalStateException("Apps Script không trả kết quả cho hạng mục đã xác nhận.");
    }

    private LocalDate parseApiDate(String value) {
        String text = trim(value);
        if (text.isEmpty() || "-".equals(text)) return null;
        try { return LocalDate.parse(text, API_DATE); }
        catch (DateTimeParseException e) { return null; }
    }

    private int valueOrZero(Integer value) { return value == null ? 0 : value; }
    private String trim(String value) { return value == null ? "" : value.trim(); }
    private String shortMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) message = "Không thể tra giá lúc này.";
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
    private String joinNumbers(List<Integer> values) {
        List<String> strings = new ArrayList<>();
        for (Integer value : values) strings.add(String.valueOf(value));
        return String.join(", ", strings);
    }

    private static class ParsedLine {
        private final String externalId;
        private final String displayName;
        private final String keyword;
        private final String accountName;
        private final String originalLine;

        private ParsedLine(String externalId, String displayName, String keyword,
                           String accountName, String originalLine) {
            this.externalId = externalId;
            this.displayName = displayName;
            this.keyword = keyword;
            this.accountName = accountName;
            this.originalLine = originalLine;
        }
    }
}
