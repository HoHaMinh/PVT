package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.event.ResponseEventInformation;

import java.util.List;

public interface IResponseEventInformationService {
    void addResponseEventInformation(ResponseEventInformation responseEventInformation);
    List<ResponseEventInformation> getAllResponseById(Integer id);

}
