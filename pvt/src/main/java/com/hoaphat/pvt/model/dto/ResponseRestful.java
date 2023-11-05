package com.hoaphat.pvt.model.dto;

import com.hoaphat.pvt.model.event.ResponseEventInformation;

import java.util.List;

public class ResponseRestful {
    private ResponseEventInformation responseEventInformation;
    private List<ResponseEventInformation> responseEventInformationList;

    public ResponseRestful() {
    }

    public ResponseRestful(ResponseEventInformation responseEventInformation, List<ResponseEventInformation> responseEventInformationList) {
        this.responseEventInformation = responseEventInformation;
        this.responseEventInformationList = responseEventInformationList;
    }

    public ResponseEventInformation getResponseEventInformation() {
        return responseEventInformation;
    }

    public void setResponseEventInformation(ResponseEventInformation responseEventInformation) {
        this.responseEventInformation = responseEventInformation;
    }

    public List<ResponseEventInformation> getResponseEventInformationList() {
        return responseEventInformationList;
    }

    public void setResponseEventInformationList(List<ResponseEventInformation> responseEventInformationList) {
        this.responseEventInformationList = responseEventInformationList;
    }
}
