package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.UserDisplayDTO;
import com.hoaphat.pvt.model.dto.price.PriceLookupBatchConfirmationResult;
import com.hoaphat.pvt.model.dto.price.PriceLookupConfirmationRow;
import com.hoaphat.pvt.model.price.PriceLookupCandidate;
import com.hoaphat.pvt.model.price.PriceLookupItem;
import com.hoaphat.pvt.model.price.PriceLookupResult;
import com.hoaphat.pvt.model.price.PriceLookupStatus;
import com.hoaphat.pvt.service.account.AccountService;
import com.hoaphat.pvt.service.price.PriceLookupService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.site-name=Phòng vật tư Test",
        "app.price-lookup.enabled=true"
})
@AutoConfigureMockMvc
class PriceLookupPageIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PriceLookupService priceLookupService;
    @MockBean private AccountService accountService;

    @Test
    void employeeOnlyLoadsOwnPendingItems() throws Exception {
        when(priceLookupService.getPending("employee.one", false, "employee.one"))
                .thenReturn(Collections.emptyList());
        when(accountService.getUserDisplayList()).thenReturn(Collections.singletonList(
                new UserDisplayDTO("employee.one", "Nhân viên Một")));

        mockMvc.perform(get("/home/employee/price-lookups")
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("priceLookupPending"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tra giá/xác nhận")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Cung cấp hạng mục cần tra giá</button>"))));

        verify(priceLookupService).getPending("employee.one", false, "employee.one");
    }

    @Test
    void employeeCannotCreateManagerBatch() throws Exception {
        mockMvc.perform(post("/home/manager/price-lookups")
                        .with(user("employee.one").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("requestLines", "Vật tư\temployee.one"))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotUseFilterToReadAnotherEmployeesResults() throws Exception {
        when(priceLookupService.searchResults("employee.one", false,
                "employee.one", "", 0)).thenReturn(Page.empty());
        when(accountService.getUserDisplayList()).thenReturn(Collections.singletonList(
                new UserDisplayDTO("employee.one", "Nhân viên Một")));

        mockMvc.perform(get("/home/employee/price-lookup-results")
                        .param("selectedValue", "employee.two")
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("priceLookupResults"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kết quả tra giá")));

        verify(priceLookupService).searchResults("employee.one", false,
                "employee.one", "", 0);
    }

    @Test
    void resultPageUsesDisplayNameAndCompactPriceLabels() throws Exception {
        PriceLookupItem item = new PriceLookupItem();
        item.setItemId(51);
        item.setDisplayName("Lõi lọc sợi quấn AQUA - CPP 5 micromet 20 inch");
        item.setKeyword("aqua*cpp");
        item.setAssignedAccountName("dminhhh");
        item.setAssignedDisplayName("Hà Minh");
        item.setStatus(PriceLookupStatus.COMPLETED);
        item.setCompletedAt(LocalDateTime.of(2026, 10, 12, 18, 23));

        PriceLookupResult result = new PriceLookupResult();
        result.setGroupNumber(2);
        result.setGroupName("Miền Trung (Đà Nẵng)");
        result.setFound(true);
        result.setMaterialName("Tên vật tư trả về từ cache");
        result.setUnitName("Đà Nẵng");
        result.setPurchaseDate(LocalDate.of(2026, 8, 20));
        result.setPurchasePrice(new BigDecimal("47000"));
        result.setSupplierName("Công ty Thiết Bị Lọc");
        PriceLookupResult noPrice = new PriceLookupResult();
        noPrice.setGroupNumber(3);
        noPrice.setGroupName("Miền Nam");
        noPrice.setFound(false);
        item.setResults(Arrays.asList(result, noPrice));

        when(priceLookupService.searchResults("manager", true, "", "", 0))
                .thenReturn(new PageImpl<>(Collections.singletonList(item)));
        when(accountService.getUserDisplayList()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/home/employee/price-lookup-results")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Lõi lọc sợi quấn AQUA - CPP 5 micromet 20 inch")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("18:23 12/10/2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-copy=\"47,000\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">Đà Nẵng</span>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Hà Minh · dminhhh"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Đơn vị:"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Nhà cung cấp:"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Hiện chưa tìm thấy giá cơ sở")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "placeholder=\"Tìm theo tên vật tư, nhà cung cấp\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "aria-label=\"Xóa kết quả tra giá\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/home/employee/price-lookup-results/51/trash")));
    }

    @Test
    void employeeCannotOpenDeletedPriceResultFolder() throws Exception {
        mockMvc.perform(get("/home/manager/price-lookup-results/trash")
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerDeletedResultPageReusesResultLayoutWithRestoreAndEmptyActions() throws Exception {
        PriceLookupItem item = new PriceLookupItem();
        item.setItemId(52);
        item.setDisplayName("Vật tư đã xóa");
        item.setAssignedAccountName("employee.one");
        item.setStatus(PriceLookupStatus.COMPLETED);
        item.setDeleted(true);
        item.setCompletedAt(LocalDateTime.of(2026, 11, 4, 8, 55));
        item.setResults(Collections.emptyList());
        when(priceLookupService.searchDeletedResults(true, "", "", 0))
                .thenReturn(new PageImpl<>(Collections.singletonList(item)));
        when(accountService.getUserDisplayList()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/home/manager/price-lookup-results/trash")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("priceLookupResults"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Kết quả tra giá đã xóa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khôi phục")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Làm sạch thư mục")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/home/manager/price-lookup-results/trash/52/restore")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("aria-label=\"Xóa kết quả tra giá\""))));
    }

    @Test
    void assignedEmployeeCanSubmitResultDeletionAndKeepsCurrentFilters() throws Exception {
        mockMvc.perform(post("/home/employee/price-lookup-results/53/trash")
                        .with(user("employee.one").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("selectedValue", "employee.one")
                        .param("search", " vòng bi ")
                        .param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/home/employee/price-lookup-results?selectedValue=employee.one&search=v%C3%B2ng+bi&page=2"));

        verify(priceLookupService).moveResultToTrash(53, "employee.one", false);
    }

    @Test
    void managerCanRestoreAndPermanentlyEmptyDeletedResultFolder() throws Exception {
        when(priceLookupService.emptyDeletedResults(true)).thenReturn(3);

        mockMvc.perform(post("/home/manager/price-lookup-results/trash/54/restore")
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/manager/price-lookup-results/trash"));
        mockMvc.perform(post("/home/manager/price-lookup-results/trash/empty")
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mess", "Đã xóa hẳn 3 kết quả tra giá."));

        verify(priceLookupService).restoreDeletedResult(54, true);
        verify(priceLookupService).emptyDeletedResults(true);
    }

    @Test
    void pendingPageHasOneBottomConfirmButtonAndNoSuitableOptionForEveryCandidateCard() throws Exception {
        PriceLookupItem withCandidates = pendingItem(11, "employee.one", "Con lăn dẫn phoi");
        PriceLookupCandidate candidate = new PriceLookupCandidate();
        candidate.setCandidateId(101);
        candidate.setMaterialName("Tên đề xuất thứ nhất");
        withCandidates.addCandidate(candidate);
        PriceLookupItem withoutCandidates = pendingItem(12, "employee.one", "Bơm tưới nguội");
        withoutCandidates.setStatusMessage("Không tìm thấy tên vật tư phù hợp, nên thử với keyword khác");

        when(priceLookupService.getPending("employee.one", false, "employee.one"))
                .thenReturn(Arrays.asList(withCandidates, withoutCandidates));
        when(accountService.getUserDisplayList()).thenReturn(Collections.singletonList(
                new UserDisplayDTO("employee.one", "Nhân viên Một")));

        mockMvc.perform(get("/home/employee/price-lookups")
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Không có tên đề xuất nào phù hợp với tên hàng cần mua")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Hiện chưa tìm thấy giá cơ sở")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Xác nhận và tra giá"))))
                .andExpect(result -> assertEquals(1,
                        occurrences(result.getResponse().getContentAsString(), ">Xác nhận</button>")));
    }

    @Test
    void batchConfirmationBindsSelectionsFromDifferentCards() throws Exception {
        PriceLookupBatchConfirmationResult serviceResult = new PriceLookupBatchConfirmationResult();
        serviceResult.completed();
        serviceResult.completed();
        when(priceLookupService.confirmBatch(anyList(), eq("employee.one"), eq(false)))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/home/employee/price-lookups/confirm-batch")
                        .with(user("employee.one").roles("EMPLOYEE"))
                        .with(csrf())
                        .param("selectedValue", "employee.one")
                        .param("items[0].itemId", "11")
                        .param("items[0].selectedCandidateIds", "101", "102")
                        .param("items[1].itemId", "12")
                        .param("items[1].noSuitableCandidate", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/employee/price-lookups?selectedValue=employee.one"))
                .andExpect(flash().attribute("mess", "Đã xác nhận 2 hạng mục và lưu kết quả tra giá."));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<PriceLookupConfirmationRow>> captor =
                ArgumentCaptor.forClass(java.util.List.class);
        verify(priceLookupService).confirmBatch(captor.capture(), eq("employee.one"), eq(false));
        assertEquals(Arrays.asList(101, 102), captor.getValue().get(0).getSelectedCandidateIds());
        assertTrue(captor.getValue().get(1).isNoSuitableCandidate());
    }

    private PriceLookupItem pendingItem(int itemId, String accountName, String displayName) {
        PriceLookupItem item = new PriceLookupItem();
        item.setItemId(itemId);
        item.setKeyword(displayName);
        item.setDisplayName(displayName);
        item.setAssignedAccountName(accountName);
        item.setAssignedDisplayName("Nhân viên Một");
        item.setCreatedAt(LocalDateTime.of(2026, 10, 15, 11, 39));
        item.setStatus(PriceLookupStatus.PENDING_CONFIRMATION);
        return item;
    }

    private int occurrences(String text, String needle) {
        int count = 0;
        int position = 0;
        while ((position = text.indexOf(needle, position)) >= 0) {
            count++;
            position += needle.length();
        }
        return count;
    }
}
