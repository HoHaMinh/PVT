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
    public void addMonthEvent(MonthEvent monthEvent, boolean flag) {
        if (flag) {
            monthEvent.setRegisteredDay(LocalDateTime.now());
        }
        if (Boolean.TRUE.equals(monthEvent.getAll())) {
            // 👉 chọn "All"
            monthEvent.setAccount(null);
        } else {
            // 👉 chọn cá nhân
            if (monthEvent.getAccount() != null
                    && monthEvent.getAccount().getAccountName() != null) {

                String accountName = monthEvent.getAccount().getAccountName();

                Account acc = accountRepository.findByAccountName(accountName);

                monthEvent.setAccount(acc); // ⭐ lấy object đã tồn tại trong DB
            }
        } monthEventRepository.save(monthEvent);
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

        MonthEvent existingEvent = monthEventRepository.findByIdForUpdate(editedEvent.getMonthEventId())
                .filter(event -> !Boolean.TRUE.equals(event.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Công việc không còn tồn tại."));

        existingEvent.setMonthEventDescription(editedEvent.getMonthEventDescription());
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
        MonthEvent event = findDeletedEvent(id);
        event.setDeleted(false);
        event.setHidden(false);
        monthEventRepository.save(event);
    }

    @Override
    @Transactional
    public void permanentlyDelete(Integer id) {
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
