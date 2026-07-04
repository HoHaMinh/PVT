package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.account.IAccountRoleRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseEventInformationServiceTest {
    @Mock
    private IResponseEventInformationRepository responseRepository;

    @Mock
    private IMonthEventRepository monthEventRepository;

    @Mock
    private IAccountRoleRepository accountRoleRepository;

    @InjectMocks
    private ResponseEventInformationService responseService;

    @Test
    void addResponseRejectsDeletedOrMissingEventBeforeWriting() {
        MonthEvent submittedEvent = new MonthEvent();
        submittedEvent.setMonthEventId(200);
        ResponseEventInformation response = new ResponseEventInformation(submittedEvent);
        response.setCreatedByUser("employee.test");
        response.setEventInformationResponse("Đã thực hiện");
        when(monthEventRepository.findByIdForUpdate(200)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> responseService.addResponseEventInformation(response));

        verify(responseRepository, never()).save(any(ResponseEventInformation.class));
        verify(monthEventRepository, never()).updateResponseStatus1(200);
        verify(monthEventRepository, never()).updateResponseStatus2(200);
        verify(monthEventRepository, never()).updateMonthEventResponse(any(), any(), any());
    }

    @Test
    void addResponseUsesTheManagedActiveEvent() {
        MonthEvent submittedEvent = new MonthEvent();
        submittedEvent.setMonthEventId(201);
        MonthEvent activeEvent = new MonthEvent();
        activeEvent.setMonthEventId(201);
        ResponseEventInformation response = new ResponseEventInformation(submittedEvent);
        response.setCreatedByUser("manager.test");
        response.setEventInformationResponse("Đã kiểm tra");
        when(monthEventRepository.findByIdForUpdate(201)).thenReturn(Optional.of(activeEvent));
        when(accountRoleRepository.findAllRoleByUser("manager.test"))
                .thenReturn(Collections.singletonList("ROLE_MANAGER"));

        responseService.addResponseEventInformation(response);

        assertSame(activeEvent, response.getMonthEvent());
        verify(monthEventRepository).updateResponseStatus1(201);
        verify(responseRepository).save(response);
    }
}
