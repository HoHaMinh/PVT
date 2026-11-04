package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.service.NotificationService;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.site-name=Phòng vật tư Test")
@AutoConfigureMockMvc
class TrashSearchPageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IMonthEventService monthEventService;

    @MockBean
    private NotificationService notificationService;

    @Test
    void managerCanSearchDeletedTasksByAssignmentContent() throws Exception {
        MonthEvent event = new MonthEvent();
        event.setMonthEventId(81);
        event.setMonthEventDescription("Báo cáo khảo sát sửa lắp điều hòa");
        event.setMonthEventDeadline(LocalDateTime.of(2026, 5, 4, 11, 0));
        when(monthEventService.searchDeletedMonthEvents(
                org.mockito.ArgumentMatchers.any(Pageable.class),
                org.mockito.ArgumentMatchers.eq("điều hòa")))
                .thenReturn(new PageImpl<>(Collections.singletonList(event)));

        mockMvc.perform(get("/home/manager/trash")
                        .param("search", "  điều hòa  ")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("trash"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "placeholder=\"Tìm theo nội dung giao việc\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "value=\"điều hòa\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Báo cáo khảo sát sửa lắp điều hòa")));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(monthEventService).searchDeletedMonthEvents(pageableCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("điều hòa"));
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(11, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void restoreKeepsTrashSearchAndPageState() throws Exception {
        mockMvc.perform(post("/home/manager/trash/restore/82")
                        .param("search", " báo cáo ")
                        .param("page", "3")
                        .param("size", "20")
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/home/manager/trash?search=b%C3%A1o+c%C3%A1o&page=3&size=20"));

        verify(monthEventService).restoreFromTrash(82);
    }
}
