package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ResponseEventInformationService implements IResponseEventInformationService{
    @Autowired
    private IResponseEventInformationRepository responseEventInformationRepository;

    @Autowired
    private IMonthEventRepository monthEventRepository;

    @Override
    public void addResponseEventInformation(com.hoaphat.pvt.model.event.ResponseEventInformation responseEventInformation) {
        if (Objects.equals(responseEventInformation.getCreatedByUser(), "dminhhh") || Objects.equals(responseEventInformation.getCreatedByUser(), "dmont") ) {
            monthEventRepository.updateResponseStatus1(responseEventInformation.getMonthEvent().getMonthEventId());
        } else {
            monthEventRepository.updateResponseStatus2(responseEventInformation.getMonthEvent().getMonthEventId());
        }
        responseEventInformationRepository.save(responseEventInformation);
    }

    @Override
    public List<com.hoaphat.pvt.model.event.ResponseEventInformation> getAllResponseById(Integer idMonthEvent) {
        return responseEventInformationRepository.findResponseById(idMonthEvent);
    }
}
