package com.hoaphat.pvt.model.dto;

import com.hoaphat.pvt.model.event.ResponseAttachment;

import java.util.Locale;

public class ResponseAttachmentView {
    private Integer idAttachment;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String url;
    private String extension;
    private boolean image;
    private boolean pdf;

    public ResponseAttachmentView() {
    }

    public ResponseAttachmentView(ResponseAttachment attachment) {
        this.idAttachment = attachment.getIdAttachment();
        this.originalFileName = attachment.getOriginalFileName();
        this.contentType = attachment.getContentType();
        this.fileSize = attachment.getFileSize();
        this.url = "/home/employee/private/response-attachment/" + attachment.getIdAttachment();
        this.extension = extensionOf(attachment.getOriginalFileName());
        String normalizedContentType = attachment.getContentType() == null
                ? "" : attachment.getContentType().toLowerCase(Locale.ROOT);
        this.image = normalizedContentType.startsWith("image/");
        this.pdf = "pdf".equals(this.extension) || normalizedContentType.equals("application/pdf");
    }

    public Integer getIdAttachment() {
        return idAttachment;
    }

    public void setIdAttachment(Integer idAttachment) {
        this.idAttachment = idAttachment;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean isImage() {
        return image;
    }

    public void setImage(boolean image) {
        this.image = image;
    }

    public boolean isPdf() {
        return pdf;
    }

    public void setPdf(boolean pdf) {
        this.pdf = pdf;
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
