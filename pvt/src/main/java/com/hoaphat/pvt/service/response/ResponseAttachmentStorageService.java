package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.config.ResponseChatProperties;
import com.hoaphat.pvt.model.dto.ResponseAttachmentView;
import com.hoaphat.pvt.model.dto.ResponseChatMessageView;
import com.hoaphat.pvt.model.event.ResponseAttachment;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.event.IResponseAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ResponseAttachmentStorageService {
    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp",
                    "pdf", "doc", "docx", "rtf", "odt",
                    "xls", "xlsx", "xlsm", "xlsb", "csv", "ods");

    private static final Map<String, String> EXTENSION_CONTENT_TYPES =
            new LinkedHashMap<String, String>();

    static {
        EXTENSION_CONTENT_TYPES.put("jpg", "image/jpeg");
        EXTENSION_CONTENT_TYPES.put("jpeg", "image/jpeg");
        EXTENSION_CONTENT_TYPES.put("png", "image/png");
        EXTENSION_CONTENT_TYPES.put("gif", "image/gif");
        EXTENSION_CONTENT_TYPES.put("webp", "image/webp");
        EXTENSION_CONTENT_TYPES.put("bmp", "image/bmp");
        EXTENSION_CONTENT_TYPES.put("pdf", "application/pdf");
        EXTENSION_CONTENT_TYPES.put("doc", "application/msword");
        EXTENSION_CONTENT_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        EXTENSION_CONTENT_TYPES.put("rtf", "application/rtf");
        EXTENSION_CONTENT_TYPES.put("odt", "application/vnd.oasis.opendocument.text");
        EXTENSION_CONTENT_TYPES.put("xls", "application/vnd.ms-excel");
        EXTENSION_CONTENT_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        EXTENSION_CONTENT_TYPES.put("xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12");
        EXTENSION_CONTENT_TYPES.put("xlsb", "application/vnd.ms-excel.sheet.binary.macroEnabled.12");
        EXTENSION_CONTENT_TYPES.put("csv", "text/csv");
        EXTENSION_CONTENT_TYPES.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
    }

    @Autowired
    private ResponseChatProperties responseChatProperties;

    @Autowired
    private IResponseEventInformationService responseEventInformationService;

    @Autowired
    private IResponseAttachmentRepository responseAttachmentRepository;

    public boolean hasUsableFiles(MultipartFile[] files) {
        return !usableFiles(files).isEmpty();
    }

    @Transactional
    public ResponseEventInformation addResponseWithAttachments(ResponseEventInformation response,
                                                               MultipartFile[] files) {
        List<MultipartFile> usableFiles = usableFiles(files);
        validateFiles(usableFiles);

        ResponseEventInformation savedResponse = responseEventInformationService
                .addResponseEventInformation(response, !usableFiles.isEmpty());

        List<Path> storedPaths = new ArrayList<Path>();
        try {
            for (MultipartFile file : usableFiles) {
                ResponseAttachment attachment = storeFile(savedResponse, file, storedPaths);
                responseAttachmentRepository.save(attachment);
            }
            return savedResponse;
        } catch (RuntimeException ex) {
            deleteQuietly(storedPaths);
            throw ex;
        }
    }

    public List<ResponseChatMessageView> toChatMessageViews(List<ResponseEventInformation> responses) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> responseIds = new ArrayList<Integer>();
        for (ResponseEventInformation response : responses) {
            if (response != null && response.getIdResponse() != null) {
                responseIds.add(response.getIdResponse());
            }
        }

        Map<Integer, List<ResponseAttachmentView>> attachmentMap =
                new HashMap<Integer, List<ResponseAttachmentView>>();
        if (!responseIds.isEmpty()) {
            List<ResponseAttachment> attachments =
                    responseAttachmentRepository.findByResponseIds(responseIds);
            for (ResponseAttachment attachment : attachments) {
                if (attachment == null || attachment.getResponse() == null
                        || attachment.getResponse().getIdResponse() == null) {
                    continue;
                }
                Integer responseId = attachment.getResponse().getIdResponse();
                if (!attachmentMap.containsKey(responseId)) {
                    attachmentMap.put(responseId, new ArrayList<ResponseAttachmentView>());
                }
                attachmentMap.get(responseId).add(new ResponseAttachmentView(attachment));
            }
        }

        List<ResponseChatMessageView> result = new ArrayList<ResponseChatMessageView>();
        for (ResponseEventInformation response : responses) {
            if (response == null) {
                continue;
            }
            ResponseChatMessageView view = new ResponseChatMessageView(response);
            List<ResponseAttachmentView> attachments = attachmentMap.get(response.getIdResponse());
            if (attachments != null) {
                view.setAttachments(attachments);
            }
            result.add(view);
        }
        return result;
    }

    public AttachmentFile loadAttachment(Integer idAttachment) {
        if (idAttachment == null || idAttachment <= 0) {
            throw new IllegalArgumentException("Không xác định được file cần mở.");
        }
        ResponseAttachment attachment = responseAttachmentRepository.findActiveAttachment(idAttachment)
                .orElseThrow(() -> new IllegalArgumentException("File không còn tồn tại."));
        Path basePath = basePath();
        Path filePath = basePath.resolve(attachment.getStoredRelativePath()).normalize();
        if (!filePath.startsWith(basePath) || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("File không còn tồn tại trên máy chủ.");
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            return new AttachmentFile(attachment, resource);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Không thể mở file.", e);
        }
    }

    public void deleteFilesByMonthEventId(Integer monthEventId) {
        if (monthEventId == null) {
            return;
        }
        List<ResponseAttachment> attachments =
                responseAttachmentRepository.findByMonthEventId(monthEventId);
        Path basePath = basePath();
        for (ResponseAttachment attachment : attachments) {
            if (attachment == null || attachment.getStoredRelativePath() == null) {
                continue;
            }
            Path filePath = basePath.resolve(attachment.getStoredRelativePath()).normalize();
            if (!filePath.startsWith(basePath)) {
                continue;
            }
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
                // Nếu file đang bị khóa, vẫn để DB xóa theo luồng cũ; lần sau có thể dọn tay.
            }
        }
    }

    private ResponseAttachment storeFile(ResponseEventInformation response,
                                         MultipartFile file,
                                         Collection<Path> storedPaths) {
        Integer monthEventId = response.getMonthEvent().getMonthEventId();
        Integer responseId = response.getIdResponse();
        if (responseId == null) {
            throw new IllegalArgumentException("Không xác định được phản hồi để gắn file.");
        }

        Path basePath = basePath();
        Path responseFolder = basePath
                .resolve(String.valueOf(monthEventId))
                .resolve(String.valueOf(responseId))
                .normalize();
        if (!responseFolder.startsWith(basePath)) {
            throw new IllegalArgumentException("Đường dẫn lưu file không hợp lệ.");
        }

        try {
            Files.createDirectories(responseFolder);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể tạo thư mục lưu file phản hồi.", e);
        }

        String originalFileName = safeOriginalFileName(file.getOriginalFilename());
        String extension = extensionOf(originalFileName);
        String storedFileName = UUID.randomUUID().toString() + "." + extension;
        Path targetPath = responseFolder.resolve(storedFileName).normalize();
        if (!targetPath.startsWith(responseFolder)) {
            throw new IllegalArgumentException("Tên file không hợp lệ.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            storedPaths.add(targetPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể lưu file phản hồi.", e);
        }

        ResponseAttachment attachment = new ResponseAttachment();
        attachment.setResponse(response);
        attachment.setOriginalFileName(originalFileName);
        attachment.setStoredRelativePath(
                basePath.relativize(targetPath).toString().replace('\\', '/'));
        attachment.setContentType(contentTypeFor(file, extension));
        attachment.setFileSize(file.getSize());
        attachment.setCreatedByUser(response.getCreatedByUser());
        attachment.setCreatedByDate(LocalDateTime.now());
        return attachment;
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files.isEmpty()) {
            return;
        }
        int maxAttachments = Math.max(1, responseChatProperties.getMaxAttachmentsPerMessage());
        if (files.size() > maxAttachments) {
            throw new IllegalArgumentException("Mỗi lần gửi chỉ được đính kèm tối đa "
                    + maxAttachments + " file.");
        }
        long maxImageSize = Math.max(1L, responseChatProperties.getMaxImageSizeBytes());
        long maxAttachmentSize = Math.max(maxImageSize,
                responseChatProperties.getMaxAttachmentSizeBytes());
        for (MultipartFile file : files) {
            String originalFileName = safeOriginalFileName(file.getOriginalFilename());
            String extension = extensionOf(originalFileName);
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException(
                        "Chỉ được gửi ảnh, PDF, Word hoặc Excel/CSV.");
            }
            long limit = isImageExtension(extension) ? maxImageSize : maxAttachmentSize;
            if (file.getSize() > limit) {
                throw new IllegalArgumentException("File '" + originalFileName
                        + "' vượt quá dung lượng cho phép.");
            }
        }
    }

    private List<MultipartFile> usableFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<MultipartFile> result = new ArrayList<MultipartFile>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                result.add(file);
            }
        }
        return result;
    }

    private Path basePath() {
        String configuredPath = responseChatProperties.getAttachmentFolderPath();
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            configuredPath = "./response-attachments";
        }
        return Paths.get(configuredPath).toAbsolutePath().normalize();
    }

    private String safeOriginalFileName(String originalFileName) {
        String cleaned = StringUtils.cleanPath(originalFileName == null ? "attachment" : originalFileName);
        cleaned = cleaned.replace('\\', '/');
        int slashIndex = cleaned.lastIndexOf('/');
        if (slashIndex >= 0) {
            cleaned = cleaned.substring(slashIndex + 1);
        }
        if (cleaned.trim().isEmpty()) {
            cleaned = "attachment";
        }
        return cleaned;
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isImageExtension(String extension) {
        String contentType = EXTENSION_CONTENT_TYPES.get(extension);
        return contentType != null && contentType.startsWith("image/");
    }

    private String contentTypeFor(MultipartFile file, String extension) {
        String mapped = EXTENSION_CONTENT_TYPES.get(extension);
        if (mapped != null) {
            return mapped;
        }
        String detected = file.getContentType();
        return detected == null || detected.trim().isEmpty()
                ? "application/octet-stream"
                : detected;
    }

    private void deleteQuietly(List<Path> storedPaths) {
        for (Path path : storedPaths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }

    public static class AttachmentFile {
        private final ResponseAttachment attachment;
        private final Resource resource;

        public AttachmentFile(ResponseAttachment attachment, Resource resource) {
            this.attachment = attachment;
            this.resource = resource;
        }

        public ResponseAttachment getAttachment() {
            return attachment;
        }

        public Resource getResource() {
            return resource;
        }
    }
}
