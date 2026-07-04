package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import com.hoaphat.pvt.service.response.IResponseEventInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

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

        monthEventRepository.save(existingEvent);

        ResponseEventInformation response = new ResponseEventInformation();
        response.setCreatedByUser(editedBy);
        response.setCreatedByDate(LocalDateTime.now());
        response.setEventInformationResponse("Báo cáo lại "
                + existingEvent.getMonthEventDeadline().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        response.setMonthEvent(existingEvent);
        responseEventInformationService.addResponseEventInformation(response);
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
        responseEventInformationRepository.deleteResponseById(id);
        monthEventRepository.delete(event);
    }

    @Override
    @Transactional
    public int emptyTrash() {
        List<MonthEvent> deletedEvents = monthEventRepository.findDeletedEventsForUpdate();
        for (MonthEvent event : deletedEvents) {
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
