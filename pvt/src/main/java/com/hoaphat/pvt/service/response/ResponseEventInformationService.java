package com.hoaphat.pvt.service.response;

import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.account.IAccountRoleRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ResponseEventInformationService implements IResponseEventInformationService {
    @Autowired
    private IResponseEventInformationRepository responseEventInformationRepository;

    @Autowired
    private IMonthEventRepository monthEventRepository;

    @Autowired
    private IAccountRoleRepository accountRoleRepository;

    @Override
    @Transactional
    public ResponseEventInformation addResponseEventInformation(ResponseEventInformation responseEventInformation) {
        return addResponseEventInformation(responseEventInformation, false);
    }

    @Override
    @Transactional
    public ResponseEventInformation addResponseEventInformation(ResponseEventInformation responseEventInformation,
                                                               boolean allowBlankContent) {

        if (responseEventInformation == null) {
            throw new IllegalArgumentException("Không xác định được phản hồi cần lưu.");
        }
        if (responseEventInformation.getMonthEvent() == null
                || responseEventInformation.getMonthEvent().getMonthEventId() == null
                || responseEventInformation.getMonthEvent().getMonthEventId() <= 0) {
            throw new IllegalArgumentException("Không xác định được công việc cần phản hồi.");
        }
        if (responseEventInformation.getEventInformationResponse() == null
                || responseEventInformation.getEventInformationResponse().trim().isEmpty()) {
            if (!allowBlankContent) {
                throw new IllegalArgumentException("Nội dung phản hồi không được để trống.");
            }
            responseEventInformation.setEventInformationResponse("");
        }
        if (responseEventInformation.getCreatedByUser() == null
                || responseEventInformation.getCreatedByUser().trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được người gửi phản hồi.");
        }

        Integer monthEventId = responseEventInformation.getMonthEvent().getMonthEventId();
        MonthEvent activeEvent = monthEventRepository.findByIdForUpdate(monthEventId)
                .filter(event -> !Boolean.TRUE.equals(event.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Công việc không còn tồn tại hoặc đã được chuyển vào Thư mục đã xóa."));
        responseEventInformation.setMonthEvent(activeEvent);
        responseEventInformation.setEventInformationResponse(
                responseEventInformation.getEventInformationResponse().trim());
        if (responseEventInformation.getCreatedByDate() == null) {
            responseEventInformation.setCreatedByDate(LocalDateTime.now());
        }

        // Bước 1: xóa log "Báo cáo lại" cũ.
        if (responseEventInformation.getEventInformationResponse() != null
                && responseEventInformation.getEventInformationResponse().startsWith("Báo cáo lại")) {

            responseEventInformationRepository
                    .deleteByMonthEvent_MonthEventIdAndEventInformationResponseStartingWith(
                            monthEventId,
                            "Báo cáo lại"
                    );
        }

        // Bước 2: giữ nguyên logic trạng thái phản hồi cũ.
        List<String> roles = accountRoleRepository.findAllRoleByUser(
                responseEventInformation.getCreatedByUser());
        boolean isManager = (roles == null ? Collections.<String>emptyList() : roles)
                .contains("ROLE_MANAGER");

        if (isManager) {
            monthEventRepository.updateResponseStatus1(monthEventId);
        } else {
            monthEventRepository.updateResponseStatus2(monthEventId);
        }

        // Bước 3: lưu phản hồi và trả lại entity để phần ảnh lấy được idResponse.
        ResponseEventInformation savedResponse =
                responseEventInformationRepository.save(responseEventInformation);

        // Bước 4: cập nhật thông tin phản hồi mới nhất của công việc.
        monthEventRepository.updateMonthEventResponse(
                responseEventInformation.getCreatedByDate(),
                responseEventInformation.getCreatedByUser(),
                monthEventId
        );
        return savedResponse;
    }

    @Override
    public List<ResponseEventInformation> getAllResponseById(Integer idMonthEvent) {
        return responseEventInformationRepository.findResponseById(idMonthEvent);
    }
}
