package com.hoaphat.pvt.service.price;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.price.*;
import com.hoaphat.pvt.model.price.PriceLookupBatch;
import com.hoaphat.pvt.model.price.PriceLookupCandidate;
import com.hoaphat.pvt.model.price.PriceLookupItem;
import com.hoaphat.pvt.model.price.PriceLookupResult;
import com.hoaphat.pvt.model.price.PriceLookupStatus;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.price.IPriceLookupBatchRepository;
import com.hoaphat.pvt.repository.price.IPriceLookupItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class PriceLookupServiceTest {
    private PriceLookupAppsScriptClient client;
    private IPriceLookupBatchRepository batchRepository;
    private IAccountRepository accountRepository;
    private IPriceLookupItemRepository itemRepository;
    private PriceLookupService service;

    @BeforeEach
    void setUp() {
        client = mock(PriceLookupAppsScriptClient.class);
        batchRepository = mock(IPriceLookupBatchRepository.class);
        accountRepository = mock(IAccountRepository.class);
        itemRepository = mock(IPriceLookupItemRepository.class);
        service = new PriceLookupService(client, batchRepository,
                itemRepository, accountRepository);
    }

    @Test
    void parsesTabAndWideSpaceThenRoutesEachItemToItsAccount() {
        when(accountRepository.findAll()).thenReturn(Arrays.asList(
                new Account("dtramtt", "x", "Đặng Trần Minh Tâm"),
                new Account("dtrongtv.", "x", "Đỗ Trọng TV")));
        when(client.search(anyList())).thenAnswer(invocation -> {
            List<PriceSearchQuery> queries = invocation.getArgument(0);
            PriceSearchApiResponse response = new PriceSearchApiResponse();
            response.setSuccess(true);
            List<PriceSearchResultPayload> results = new ArrayList<>();
            for (PriceSearchQuery query : queries) {
                PriceSearchResultPayload result = new PriceSearchResultPayload();
                result.setId(query.getId());
                result.setKeyword(query.getKeyword());
                PriceCandidatePayload candidate = new PriceCandidatePayload();
                candidate.setName(query.getKeyword() + " - đề xuất");
                candidate.setStartRow(2);
                candidate.setRowCount(1);
                result.setCandidates(Collections.singletonList(candidate));
                results.add(result);
            }
            response.setResults(results);
            return response;
        });

        int count = service.createBatch(
                "Tăng đơ cáp vải bản rộng\ttang*do*cap\tDTRAMTT\n"
                        + "Găng tay da hàn   gang*da*han   DTRONGTV.", "manager");

        assertEquals(2, count);
        ArgumentCaptor<PriceLookupBatch> captor = ArgumentCaptor.forClass(PriceLookupBatch.class);
        verify(batchRepository).save(captor.capture());
        assertEquals("Tăng đơ cáp vải bản rộng", captor.getValue().getItems().get(0).getDisplayName());
        assertEquals("tang*do*cap", captor.getValue().getItems().get(0).getKeyword());
        assertEquals("dtramtt", captor.getValue().getItems().get(0).getAssignedAccountName());
        assertEquals("dtrongtv.", captor.getValue().getItems().get(1).getAssignedAccountName());
        assertEquals(1, captor.getValue().getItems().get(0).getCandidates().size());
    }

    @Test
    void rejectsUnknownEmployeeBeforeCallingAppsScript() {
        when(accountRepository.findAll()).thenReturn(Collections.emptyList());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createBatch("Tăng đơ cáp\ttang*do\tKHONGCO", "manager"));
        assertTrue(error.getMessage().contains("KHONGCO"));
        verifyNoInteractions(client);
    }

    @Test
    void rejectsLegacyTwoPartInputToAvoidUsingKeywordAsDisplayName() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createBatch("aqua*cpp  DMINHHH", "manager"));

        assertTrue(error.getMessage().contains("tên hiển thị, keyword tra giá, ID nhân viên"));
        verifyNoInteractions(client, batchRepository, accountRepository);
    }

    @Test
    void keepsRepeatedSpacesInsideDisplayNameByParsingFromTheRight() {
        when(accountRepository.findAll()).thenReturn(Collections.singletonList(
                new Account("dtramtt", "x", "Thanh Trâm")));
        PriceSearchApiResponse response = new PriceSearchApiResponse();
        response.setSuccess(true);
        response.setResults(Collections.emptyList());
        when(client.search(anyList())).thenReturn(response);

        service.createBatch("Xích cẩu  thùng phuy 1000kg, Kawasaki, Trung Quốc\txích*kawa   DTRAMTT",
                "manager");

        ArgumentCaptor<PriceLookupBatch> captor = ArgumentCaptor.forClass(PriceLookupBatch.class);
        verify(batchRepository).save(captor.capture());
        PriceLookupItem item = captor.getValue().getItems().get(0);
        assertEquals("Xích cẩu  thùng phuy 1000kg, Kawasaki, Trung Quốc", item.getDisplayName());
        assertEquals("xích*kawa", item.getKeyword());
        assertEquals("Không tìm thấy tên vật tư phù hợp, nên thử với keyword khác",
                item.getStatusMessage());
    }

    @Test
    void legacyNoMatchActionCreatesVisibleNoPriceResult() {
        PriceLookupItem item = new PriceLookupItem();
        item.setAssignedAccountName("dtramtt");
        item.setStatus(PriceLookupStatus.PENDING_CONFIRMATION);
        when(itemRepository.findWithDetailsByItemId(7)).thenReturn(Optional.of(item));

        service.dismissNoMatch(7, "dtramtt", false);

        assertEquals(PriceLookupStatus.COMPLETED, item.getStatus());
        assertNull(item.getStatusMessage());
        assertEquals(1, item.getResults().size());
        assertEquals("Kết quả tra giá", item.getResults().get(0).getGroupName());
        assertFalse(item.getResults().get(0).isFound());
        verify(itemRepository).save(item);
    }

    @Test
    void batchConfirmProcessesSelectedAndNoCandidateItemsButKeepsUntouchedCards() {
        PriceLookupItem selectedItem = pendingItem(11, "dtramtt");
        selectedItem.addCandidate(candidate(101, "Vật tư đúng"));
        PriceLookupItem untouchedItem = pendingItem(12, "dtramtt");
        untouchedItem.addCandidate(candidate(102, "Chưa được chọn"));
        PriceLookupItem noCandidateItem = pendingItem(13, "dtramtt");

        when(itemRepository.findWithDetailsByItemId(11)).thenReturn(Optional.of(selectedItem));
        when(itemRepository.findWithDetailsByItemId(12)).thenReturn(Optional.of(untouchedItem));
        when(itemRepository.findWithDetailsByItemId(13)).thenReturn(Optional.of(noCandidateItem));
        when(client.lookup(anyList())).thenAnswer(invocation -> successfulLookup(invocation.getArgument(0)));

        List<PriceLookupConfirmationRow> rows = Arrays.asList(
                confirmation(11, Collections.singletonList(101), false),
                confirmation(12, Collections.emptyList(), false),
                confirmation(13, Collections.emptyList(), false));

        PriceLookupBatchConfirmationResult result = service.confirmBatch(rows, "dtramtt", false);

        assertEquals(2, result.getCompletedCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(PriceLookupStatus.COMPLETED, selectedItem.getStatus());
        assertTrue(selectedItem.getCandidates().get(0).isSelected());
        assertEquals(PriceLookupStatus.PENDING_CONFIRMATION, untouchedItem.getStatus());
        assertEquals(PriceLookupStatus.COMPLETED, noCandidateItem.getStatus());
        assertEquals(1, noCandidateItem.getResults().size());
        assertFalse(noCandidateItem.getResults().get(0).isFound());
        verify(client, times(1)).lookup(anyList());
    }

    @Test
    void noSuitableCandidateCompletesWithNoPriceResultWithoutCallingAppsScript() {
        PriceLookupItem item = pendingItem(21, "dtramtt");
        item.addCandidate(candidate(201, "Tên đề xuất không đúng"));
        when(itemRepository.findWithDetailsByItemId(21)).thenReturn(Optional.of(item));

        PriceLookupBatchConfirmationResult result = service.confirmBatch(
                Collections.singletonList(confirmation(21, Collections.emptyList(), true)),
                "dtramtt", false);

        assertEquals(1, result.getCompletedCount());
        assertEquals(PriceLookupStatus.COMPLETED, item.getStatus());
        assertFalse(item.getCandidates().get(0).isSelected());
        assertEquals(1, item.getResults().size());
        PriceLookupResult noPrice = item.getResults().get(0);
        assertFalse(noPrice.isFound());
        assertEquals("Kết quả tra giá", noPrice.getGroupName());
        verifyNoInteractions(client);
    }

    @Test
    void batchConfirmRejectsSelectingCandidateAndNoSuitableOptionTogether() {
        PriceLookupItem item = pendingItem(31, "dtramtt");
        item.addCandidate(candidate(301, "Tên đề xuất"));
        when(itemRepository.findWithDetailsByItemId(31)).thenReturn(Optional.of(item));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.confirmBatch(Collections.singletonList(
                                confirmation(31, Collections.singletonList(301), true)),
                        "dtramtt", false));

        assertTrue(error.getMessage().contains("không có tên đề xuất"));
        assertEquals(PriceLookupStatus.PENDING_CONFIRMATION, item.getStatus());
        verifyNoInteractions(client);
    }

    @Test
    void assignedEmployeeCanMoveOwnCompletedResultToTrash() {
        PriceLookupItem item = completedItem(41, "employee.one");
        when(itemRepository.findById(41)).thenReturn(Optional.of(item));

        service.moveResultToTrash(41, "employee.one", false);

        assertTrue(item.isDeleted());
        assertNotNull(item.getDeletedAt());
        assertEquals("employee.one", item.getDeletedBy());
        verify(itemRepository).save(item);
    }

    @Test
    void employeeCannotMoveAnotherEmployeesResultToTrash() {
        PriceLookupItem item = completedItem(42, "employee.two");
        when(itemRepository.findById(42)).thenReturn(Optional.of(item));

        SecurityException error = assertThrows(SecurityException.class,
                () -> service.moveResultToTrash(42, "employee.one", false));

        assertTrue(error.getMessage().contains("nhân viên khác"));
        assertFalse(item.isDeleted());
        verify(itemRepository, never()).save(item);
    }

    @Test
    void managerCanRestoreDeletedResultAndClearDeletionMetadata() {
        PriceLookupItem item = completedItem(43, "employee.two");
        item.setDeleted(true);
        item.setDeletedAt(java.time.LocalDateTime.of(2026, 11, 4, 9, 0));
        item.setDeletedBy("employee.two");
        when(itemRepository.findById(43)).thenReturn(Optional.of(item));

        service.restoreDeletedResult(43, true);

        assertFalse(item.isDeleted());
        assertNull(item.getDeletedAt());
        assertNull(item.getDeletedBy());
        verify(itemRepository).save(item);
    }

    @Test
    void employeeCannotOpenOrRestoreDeletedResultFolder() {
        assertThrows(SecurityException.class,
                () -> service.searchDeletedResults(false, "employee.one", "", 0));
        assertThrows(SecurityException.class,
                () -> service.restoreDeletedResult(44, false));

        verifyNoInteractions(itemRepository);
    }

    @Test
    void managerCanPermanentlyEmptyDeletedResultFolder() {
        PriceLookupItem first = completedItem(45, "employee.one");
        PriceLookupItem second = completedItem(46, "employee.two");
        first.setDeleted(true);
        second.setDeleted(true);
        when(itemRepository.findDeletedForUpdate()).thenReturn(Arrays.asList(first, second));

        int deletedCount = service.emptyDeletedResults(true);

        assertEquals(2, deletedCount);
        verify(itemRepository).delete(first);
        verify(itemRepository).delete(second);
    }

    private PriceLookupItem pendingItem(int id, String accountName) {
        PriceLookupItem item = new PriceLookupItem();
        item.setItemId(id);
        item.setKeyword("keyword-" + id);
        item.setAssignedAccountName(accountName);
        item.setStatus(PriceLookupStatus.PENDING_CONFIRMATION);
        return item;
    }

    private PriceLookupItem completedItem(int id, String accountName) {
        PriceLookupItem item = new PriceLookupItem();
        item.setItemId(id);
        item.setAssignedAccountName(accountName);
        item.setStatus(PriceLookupStatus.COMPLETED);
        return item;
    }

    private PriceLookupCandidate candidate(int id, String name) {
        PriceLookupCandidate candidate = new PriceLookupCandidate();
        candidate.setCandidateId(id);
        candidate.setMaterialName(name);
        candidate.setStartRow(2);
        candidate.setRowCount(1);
        return candidate;
    }

    private PriceLookupConfirmationRow confirmation(int itemId, List<Integer> selectedIds,
                                                     boolean noSuitableCandidate) {
        PriceLookupConfirmationRow row = new PriceLookupConfirmationRow();
        row.setItemId(itemId);
        row.setSelectedCandidateIds(selectedIds);
        row.setNoSuitableCandidate(noSuitableCandidate);
        return row;
    }

    private PriceLookupApiResponse successfulLookup(List<PriceLookupQuery> queries) {
        PriceLookupApiResponse response = new PriceLookupApiResponse();
        response.setSuccess(true);
        List<PriceLookupResultPayload> payloads = new ArrayList<>();
        for (PriceLookupQuery query : queries) {
            PriceResultGroupPayload group = new PriceResultGroupPayload();
            group.setGroup("Miền Trung");
            group.setFound(false);
            PriceLookupResultPayload payload = new PriceLookupResultPayload();
            payload.setId(query.getId());
            payload.setGroups(Collections.singletonList(group));
            payloads.add(payload);
        }
        response.setResults(payloads);
        return response;
    }
}
