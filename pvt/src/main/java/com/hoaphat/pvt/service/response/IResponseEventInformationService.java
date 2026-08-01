package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.event.ResponseEventInformation;

import java.util.List;

public interface IResponseEventInformationService {
    ResponseEventInformation addResponseEventInformation(ResponseEventInformation responseEventInformation);

    ResponseEventInformation addResponseEventInformation(ResponseEventInformation responseEventInformation,
                                                         boolean allowBlankContent);

    List<ResponseEventInformation> getAllResponseById(Integer id);

}
