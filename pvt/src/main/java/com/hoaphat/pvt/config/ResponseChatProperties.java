package com.hoaphat.pvt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.response-chat")
public class ResponseChatProperties {
    /**
     * Tắt mặc định để mỗi máy chủ có thể quay lại giao diện bảng cũ chỉ bằng
     * application.properties, không phải sửa database hay thay JAR.
     */
    private boolean enabled = false;

    /**
     * Thư mục lưu ảnh đính kèm phản hồi. Mặc định đi theo thư mục chạy JAR.
     * Máy chủ thật có thể cấu hình rõ hơn, ví dụ D:/PVT/response-attachments.
     */
    private String attachmentFolderPath = "./response-attachments";

    /**
     * Giới hạn mềm để tránh người dùng bấm nhầm quá nhiều ảnh trong một lần gửi.
     */
    private int maxImagesPerMessage = 5;

    private int maxAttachmentsPerMessage = 8;

    /**
     * 10 MB/ảnh là đủ thoải mái cho ảnh chụp màn hình nội bộ, nhưng vẫn tránh
     * làm đầy ổ cứng quá nhanh.
     */
    private long maxImageSizeBytes = 10L * 1024L * 1024L;

    private long maxAttachmentSizeBytes = 25L * 1024L * 1024L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAttachmentFolderPath() {
        return attachmentFolderPath;
    }

    public void setAttachmentFolderPath(String attachmentFolderPath) {
        this.attachmentFolderPath = attachmentFolderPath;
    }

    public int getMaxImagesPerMessage() {
        return maxImagesPerMessage;
    }

    public void setMaxImagesPerMessage(int maxImagesPerMessage) {
        this.maxImagesPerMessage = maxImagesPerMessage;
    }

    public int getMaxAttachmentsPerMessage() {
        return maxAttachmentsPerMessage;
    }

    public void setMaxAttachmentsPerMessage(int maxAttachmentsPerMessage) {
        this.maxAttachmentsPerMessage = maxAttachmentsPerMessage;
    }

    public long getMaxImageSizeBytes() {
        return maxImageSizeBytes;
    }

    public void setMaxImageSizeBytes(long maxImageSizeBytes) {
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    public long getMaxAttachmentSizeBytes() {
        return maxAttachmentSizeBytes;
    }

    public void setMaxAttachmentSizeBytes(long maxAttachmentSizeBytes) {
        this.maxAttachmentSizeBytes = maxAttachmentSizeBytes;
    }
}
