package com.hoaphat.pvt.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ResponseSummaryMessageView {
    private Integer idResponse;
    private String content;
    private String createdAt;
    private List<ResponseAttachmentView> attachments = new ArrayList<ResponseAttachmentView>();

    public Integer getIdResponse() {
        return idResponse;
    }

    public void setIdResponse(Integer idResponse) {
        this.idResponse = idResponse;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<ResponseAttachmentView> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<ResponseAttachmentView> attachments) {
        this.attachments = attachments == null
                ? new ArrayList<ResponseAttachmentView>() : attachments;
    }
}
