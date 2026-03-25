package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;

@Service
public class ResponseEventInformationService implements IResponseEventInformationService{
    @Autowired
    private IResponseEventInformationRepository responseEventInformationRepository;

    @Autowired
    private IMonthEventRepository monthEventRepository;

    @Override
    @Transactional
    public void addResponseEventInformation(ResponseEventInformation responseEventInformation) {

        Integer monthEventId = responseEventInformation.getMonthEvent().getMonthEventId();

        // 🔥 BƯỚC 1: XÓA log "Báo cáo lại" cũ
        if (responseEventInformation.getEventInformationResponse() != null &&
                responseEventInformation.getEventInformationResponse().startsWith("Báo cáo lại")) {

            responseEventInformationRepository
                    .deleteByMonthEvent_MonthEventIdAndEventInformationResponseStartingWith(
                            monthEventId,
                            "Báo cáo lại"
                    );
        }

        // 🔥 BƯỚC 2: logic cũ của bạn (GIỮ NGUYÊN)
        if (Objects.equals(responseEventInformation.getCreatedByUser(), "dminhhh")
                || Objects.equals(responseEventInformation.getCreatedByUser(), "dmont") ) {

            monthEventRepository.updateResponseStatus1(monthEventId);

        } else {

            monthEventRepository.updateResponseStatus2(monthEventId);
        }

        // 🔥 BƯỚC 3: save
        responseEventInformationRepository.save(responseEventInformation);

        // 🔥 BƯỚC 4: update event
        monthEventRepository.updateMonthEventResponse(
                responseEventInformation.getCreatedByDate(),
                responseEventInformation.getCreatedByUser(),
                monthEventId
        );
    }

    @Override
    public List<com.hoaphat.pvt.model.event.ResponseEventInformation> getAllResponseById(Integer idMonthEvent) {
        return responseEventInformationRepository.findResponseById(idMonthEvent);
    }
}
