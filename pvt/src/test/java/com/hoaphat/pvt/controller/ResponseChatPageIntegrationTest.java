package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.config.ResponseChatProperties;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import com.hoaphat.pvt.service.response.IResponseEventInformationService;
import com.hoaphat.pvt.service.response.ResponseAttachmentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.site-name=Phòng vật tư Ống thép Hòa Phát Test")
@AutoConfigureMockMvc
class ResponseChatPageIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @MockBean private IMonthEventService monthEventService;
    @MockBean private IResponseEventInformationService responseService;
    @MockBean private ResponseChatProperties responseChatProperties;
    @MockBean private ResponseAttachmentStorageService responseAttachmentStorageService;

    @BeforeEach
    void setUp() {
        MonthEvent event = new MonthEvent();
        event.setMonthEventId(100);
        event.setMonthEventDescription("Báo cáo tiến độ thử nghiệm");
        event.setMonthEventDeadline(LocalDateTime.of(2026, 7, 8, 16, 0));
        event.setHidden(false);
        event.setDeleted(false);
        when(monthEventService.findById(100)).thenReturn(event);
        when(responseService.getAllResponseById(100)).thenReturn(Collections.emptyList());
    }

    @Test
    void enabledFlagRendersChatWithoutLegacyAddResponseModal() throws Exception {
        when(responseChatProperties.isEnabled()).thenReturn(true);

        mockMvc.perform(get("/home/employee/private/showEventResponseForm/100")
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("eventResponseChat"))
                .andExpect(model().attribute("currentAccountName", "employee.one"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trao đổi công việc")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhập phản hồi...")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "const card = document.createElement('a');")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "scrollPageToComposerOnMobile")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"lightboxZoomOut\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"lightboxZoomIn\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"lightboxReset\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Mở ảnh ở tab mới"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("+ Thêm phản hồi"))));
    }

    @Test
    void disabledFlagKeepsOriginalTableAndModal() throws Exception {
        when(responseChatProperties.isEnabled()).thenReturn(false);

        mockMvc.perform(get("/home/employee/private/showEventResponseForm/100")
                        .with(user("employee.one").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("eventResponse"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("+ Thêm phản hồi")));
    }
}
