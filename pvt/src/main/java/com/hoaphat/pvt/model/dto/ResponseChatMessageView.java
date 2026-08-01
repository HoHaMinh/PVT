package com.hoaphat.pvt.model.dto;

import com.hoaphat.pvt.model.event.ResponseEventInformation;

import java.util.ArrayList;
import java.util.List;

public class ResponseChatMessageView {
    private Integer idResponse;
    private String eventInformationResponse;
    private String createdByUser;
    private String createdByDate;
    private List<ResponseAttachmentView> attachments = new ArrayList<ResponseAttachmentView>();

    public ResponseChatMessageView() {
    }

    public ResponseChatMessageView(ResponseEventInformation response) {
        this.idResponse = response.getIdResponse();
        this.eventInformationResponse = response.getEventInformationResponse();
        this.createdByUser = response.getCreatedByUser();
        this.createdByDate = response.getFormattedCreatedByDate();
    }

    public Integer getIdResponse() {
        return idResponse;
    }

    public void setIdResponse(Integer idResponse) {
        this.idResponse = idResponse;
    }

    public String getEventInformationResponse() {
        return eventInformationResponse;
    }

    public void setEventInformationResponse(String eventInformationResponse) {
        this.eventInformationResponse = eventInformationResponse;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getCreatedByDate() {
        return createdByDate;
    }

    public void setCreatedByDate(String createdByDate) {
        this.createdByDate = createdByDate;
    }

    public List<ResponseAttachmentView> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<ResponseAttachmentView> attachments) {
        this.attachments = attachments;
    }
}
