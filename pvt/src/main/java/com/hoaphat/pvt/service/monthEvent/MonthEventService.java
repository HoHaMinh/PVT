package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.dto.DeadlineShiftOption;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import com.hoaphat.pvt.service.response.IResponseEventInformationService;
import com.hoaphat.pvt.service.response.ResponseAttachmentStorageService;
import com.hoaphat.pvt.service.priority.WorkPriorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MonthEventService implements IMonthEventService {
    @Autowired
    private IMonthEventRepository monthEventRepository;

    @Autowired
    private IResponseEventInformationRepository responseEventInformationRepository;
    @Autowired
    private IAccountRepository accountRepository;

    @Autowired
    private IResponseEventInformationService responseEventInformationService;
    @Autowired
    private ResponseAttachmentStorageService responseAttachmentStorageService;
    @Autowired
    private WorkPriorityService workPriorityService;
    @Override
    public List<MonthEvent> getMonthEventList() {
        return monthEventRepository.findAll();
    }

    //    *Trang private
    @Override
    public List<MonthEvent> getMonthEventListByFilter(LocalDateTime now, String accountName) {
        LocalDateTime before = now.minusDays(730);
        LocalDateTime after = now.plusDays(30);

        if (accountName == null || accountName.isEmpty()) {
            return monthEventRepository.findAllFilter(before, after);
        }

        return monthEventRepository.findByAccountFilter(before, after, accountName);
    }

    //* Trang task
    @Override
    public Page<MonthEvent> getMonthEventListWithPaging(Pageable pageable) {
        return monthEventRepository.findMonthEventsByMonthEventStatus(pageable);
    }

    //  *** flag =true là add, flag =false là edit
    @Override
    @Transactional
    public void addMonthEvent(MonthEvent monthEvent, boolean flag) {
        if (flag) {
            Integer status = monthEvent == null ? null : monthEvent.getMonthEventStatus();
            if (status == null) {
                throw new IllegalArgumentException("Loại công việc không được để trống.");
            }
            addMonthEvents(Collections.singletonList(monthEvent), status);
            return;
        }
        if (monthEvent == null) {
            throw new IllegalArgumentException("Không xác định được công việc cần lưu.");
        }
        monthEventRepository.save(monthEvent);
    }

    @Override
    @Transactional
    public void addMonthEvents(List<MonthEvent> monthEvents, int expectedStatus) {
        if (expectedStatus != 0 && expectedStatus != 2) {
            throw new IllegalArgumentException("Loại công việc không hợp lệ.");
        }
        if (monthEvents == null || monthEvents.isEmpty()) {
            throw new IllegalArgumentException("Danh sách công việc không được để trống.");
        }

        LocalDateTime registeredAt = LocalDateTime.now();
        for (int i = 0; i < monthEvents.size(); i++) {
            try {
                validateAndPrepareNewEvent(monthEvents.get(i), expectedStatus, registeredAt);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Dòng " + (i + 1) + ": " + e.getMessage());
            }
        }

        // Chỉ bắt đầu ghi sau khi toàn bộ các dòng đã hợp lệ.
        for (MonthEvent monthEvent : monthEvents) {
            monthEventRepository.save(monthEvent);
        }
    }

    @Override
    @Transactional
    public void addSelfAssignedEvent(String accountName, String description, LocalDateTime deadline) {
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được tài khoản đang đăng nhập.");
        }
        Account account = accountRepository.findByAccountName(accountName.trim());
        if (account == null) {
            throw new IllegalArgumentException("Tài khoản đang đăng nhập không còn tồn tại.");
        }

        MonthEvent event = new MonthEvent();
        event.setMonthEventDescription(description);
        event.setMonthEventDeadline(deadline);
        event.setMonthEventStatus(0);
        event.setAccount(account);
        validateAndPrepareNewEvent(event, 0, LocalDateTime.now());
        monthEventRepository.save(event);
    }

    private void validateAndPrepareNewEvent(MonthEvent monthEvent, int expectedStatus,
                                            LocalDateTime registeredAt) {
        if (monthEvent == null) {
            throw new IllegalArgumentException("Công việc không được để trống.");
        }
        if (monthEvent.getMonthEventDescription() == null
                || monthEvent.getMonthEventDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung công việc không được để trống.");
        }
        if (monthEvent.getMonthEventDeadline() == null) {
            throw new IllegalArgumentException("Deadline không được để trống.");
        }
        if (!Integer.valueOf(expectedStatus).equals(monthEvent.getMonthEventStatus())) {
            throw new IllegalArgumentException("Loại công việc không hợp lệ.");
        }

        String selectedAccountName = monthEvent.getAccount() == null
                ? null : monthEvent.getAccount().getAccountName();
        if ("ALL".equals(selectedAccountName)) {
            monthEvent.setAll(true);
            monthEvent.setAccount(null);
        } else {
            if (selectedAccountName == null || selectedAccountName.trim().isEmpty()) {
                throw new IllegalArgumentException("Người phụ trách không được để trống.");
            }
            Account account = accountRepository.findByAccountName(selectedAccountName.trim());
            if (account == null) {
                throw new IllegalArgumentException("Người phụ trách đã chọn không còn tồn tại.");
            }
            monthEvent.setAll(false);
            monthEvent.setAccount(account);
        }

        if (expectedStatus == 2) {
            if (monthEvent.getExtendDay() == null
                    || monthEvent.getExtendDay() < 1
                    || monthEvent.getExtendDay() > 365) {
                throw new IllegalArgumentException("Số ngày gia hạn phải từ 1 đến 365.");
            }
        } else {
            monthEvent.setExtendDay(null);
        }

        monthEvent.setMonthEventId(null);
        monthEvent.setMonthEventDescription(monthEvent.getMonthEventDescription().trim());
        monthEvent.setMonthEventStatus(expectedStatus);
        monthEvent.setResponseStatus(0);
        monthEvent.setLastTimeResponse(LocalDateTime.of(2023, 1, 1, 0, 0));
        monthEvent.setLastPersonResponse(null);
        monthEvent.setRegisteredDay(registeredAt);
        monthEvent.setHidden(false);
        monthEvent.setDeleted(false);
    }

    @Override
    @Transactional
    public void editMonthEvent(MonthEvent editedEvent, String editedBy) {
        if (editedEvent == null || editedEvent.getMonthEventId() == null) {
            throw new IllegalArgumentException("Không xác định được công việc cần sửa.");
        }
        if (editedEvent.getMonthEventDescription() == null
                || editedEvent.getMonthEventDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung công việc không được để trống.");
        }
        if (editedEvent.getMonthEventDeadline() == null) {
            throw new IllegalArgumentException("Deadline không được để trống.");
        }
        if (editedBy == null || editedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được người sửa công việc.");
        }

        MonthEvent existingEvent = monthEventRepository.findByIdForUpdate(editedEvent.getMonthEventId())
                .filter(event -> !Boolean.TRUE.equals(event.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Công việc không còn tồn tại."));

        boolean previousAll = Boolean.TRUE.equals(existingEvent.getAll());
        String previousAccountName = existingEvent.getAccount() == null
                ? null : existingEvent.getAccount().getAccountName();

        existingEvent.setMonthEventDescription(editedEvent.getMonthEventDescription().trim());
        existingEvent.setMonthEventDeadline(editedEvent.getMonthEventDeadline());

        String selectedAccountName = editedEvent.getAccount() == null
                ? null
                : editedEvent.getAccount().getAccountName();

        if ("ALL".equals(selectedAccountName)) {
            existingEvent.setAll(true);
            existingEvent.setAccount(null);
        } else if (selectedAccountName != null && !selectedAccountName.trim().isEmpty()) {
            Account selectedAccount = accountRepository.findByAccountName(selectedAccountName);
            if (selectedAccount == null) {
                throw new IllegalArgumentException("Người phụ trách đã chọn không còn tồn tại.");
            }
            existingEvent.setAll(false);
            existingEvent.setAccount(selectedAccount);
        }

        boolean assignmentChanged = previousAll != Boolean.TRUE.equals(existingEvent.getAll())
                || !java.util.Objects.equals(previousAccountName,
                existingEvent.getAccount() == null ? null : existingEvent.getAccount().getAccountName());
        if (assignmentChanged) {
            // Chỉ bỏ ưu tiên của ngày hiện tại. Lịch sử các ngày trước phải còn
            // để trang tổng hợp phản hồi có thể đối chiếu nhân viên đã làm theo ưu tiên nào.
            workPriorityService.deleteTodayForEvent(existingEvent.getMonthEventId());
        }

        monthEventRepository.save(existingEvent);

        addDeadlineChangeResponse(existingEvent, editedBy);
    }

    @Override
    public MonthEvent findById(Integer id) {
        return monthEventRepository.findActiveById(id).orElse(null);
    }

    @Override
    public Page<MonthEvent> getDeletedMonthEventListWithPaging(Pageable pageable) {
        return monthEventRepository.findDeletedEvents(pageable);
    }

    @Override
    @Transactional
    public void moveToTrash(Integer id) {
        validateEventId(id);
        MonthEvent event = monthEventRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Công việc không còn tồn tại."));
        if (Boolean.TRUE.equals(event.getDeleted())) {
            throw new IllegalArgumentException("Công việc đã nằm trong Thư mục đã xóa.");
        }
        event.setDeleted(true);
        monthEventRepository.save(event);
    }

    @Override
    @Transactional
    public void moveToTrash(List<Integer> ids) {
        List<MonthEvent> events = loadActivePersonalEventsForBulk(ids);
        for (MonthEvent event : events) {
            event.setDeleted(true);
            monthEventRepository.save(event);
        }
    }

    @Override
    @Transactional
    public void shiftDeadlines(List<Integer> ids, DeadlineShiftOption shiftOption, String editedBy) {
        if (shiftOption == null) {
            throw new IllegalArgumentException("Vui lòng chọn khoảng thời gian dời deadline.");
        }
        if (editedBy == null || editedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Không xác định được người sửa deadline.");
        }
        List<MonthEvent> events = loadActivePersonalEventsForBulk(ids);
        for (MonthEvent event : events) {
            if (event.getMonthEventDeadline() == null) {
                throw new IllegalArgumentException("Công việc có mã " + event.getMonthEventId()
                        + " không có deadline.");
            }
        }

        for (MonthEvent event : events) {
            event.setMonthEventDeadline(shiftOption.apply(event.getMonthEventDeadline()));
            monthEventRepository.save(event);
            addDeadlineChangeResponse(event, editedBy.trim());
        }
    }

    @Override
    @Transactional
    public void restoreFromTrash(Integer id) {
        validateEventId(id);
        MonthEvent event = findDeletedEvent(id);
        event.setDeleted(false);
        event.setHidden(false);
        monthEventRepository.save(event);
    }

    @Override
    @Transactional
    public void permanentlyDelete(Integer id) {
        validateEventId(id);
        MonthEvent event = findDeletedEvent(id);
        workPriorityService.deleteForEvent(id);
        responseAttachmentStorageService.deleteFilesByMonthEventId(id);
        responseEventInformationRepository.deleteResponseById(id);
        monthEventRepository.delete(event);
    }

    @Override
    @Transactional
    public int emptyTrash() {
        List<MonthEvent> deletedEvents = monthEventRepository.findDeletedEventsForUpdate();
        for (MonthEvent event : deletedEvents) {
            workPriorityService.deleteForEvent(event.getMonthEventId());
            responseAttachmentStorageService.deleteFilesByMonthEventId(event.getMonthEventId());
            responseEventInformationRepository.deleteResponseById(event.getMonthEventId());
            monthEventRepository.delete(event);
        }
        return deletedEvents.size();
    }

    private MonthEvent findDeletedEvent(Integer id) {
        MonthEvent event = monthEventRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Công việc không còn tồn tại."));
        if (!Boolean.TRUE.equals(event.getDeleted())) {
            throw new IllegalArgumentException("Công việc không nằm trong Thư mục đã xóa.");
        }
        return event;
    }

    private void validateEventId(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Mã công việc không hợp lệ.");
        }
    }

    private List<MonthEvent> loadActivePersonalEventsForBulk(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một công việc.");
        }
        Set<Integer> uniqueIds = new LinkedHashSet<Integer>();
        for (Integer id : ids) {
            validateEventId(id);
            uniqueIds.add(id);
        }

        List<MonthEvent> events = new ArrayList<MonthEvent>();
        for (Integer id : uniqueIds) {
            MonthEvent event = monthEventRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Công việc có mã " + id + " không còn tồn tại."));
            if (Boolean.TRUE.equals(event.getDeleted())) {
                throw new IllegalArgumentException("Công việc có mã " + id
                        + " đã nằm trong Thư mục đã xóa.");
            }
            if (Boolean.TRUE.equals(event.getAll())) {
                throw new IllegalArgumentException("Công việc Cả phòng không thể thao tác hàng loạt.");
            }
            events.add(event);
        }
        return events;
    }

    private void addDeadlineChangeResponse(MonthEvent event, String editedBy) {
        ResponseEventInformation response = new ResponseEventInformation();
        response.setCreatedByUser(editedBy);
        response.setCreatedByDate(LocalDateTime.now());
        response.setEventInformationResponse("Báo cáo lại "
                + event.getMonthEventDeadline().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        response.setMonthEvent(event);
        responseEventInformationService.addResponseEventInformation(response);
    }

    //    *Trang weekly task
    @Override
    public List<MonthEvent> getWeekEventList() {
        return monthEventRepository.findWeekEvents();
    }

    @Override
    public void checkWeekEventDeadline(LocalDateTime now) {
        List<MonthEvent> weekEvents = monthEventRepository.findWeekEvents();
        for (MonthEvent weekEvent : weekEvents) {
            // Kiểm tra nếu tháng sự kiện có deadline
            if (weekEvent.getMonthEventDeadline() != null) {
                // Kiểm tra nếu deadline đã qua và extendDay là null, gán mặc định là 0
                int extendDay = (weekEvent.getExtendDay() != null) ? weekEvent.getExtendDay() : 0;

                // Kiểm tra xem deadline đã quá hạn chưa
                if (now.isAfter(weekEvent.getMonthEventDeadline().plusHours(5))) {
                    // Tính toán deadline mới với extendDay
                    LocalDateTime newDeadline = weekEvent.getMonthEventDeadline().plusDays(extendDay);
                    monthEventRepository.updateWeekEventDeadline(newDeadline, weekEvent.getMonthEventId());
                }
            }
        }
    }
}
