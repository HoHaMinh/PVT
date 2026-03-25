package com.hoaphat.pvt.service.monthEvent;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.repository.event.IMonthEventRepository;
import com.hoaphat.pvt.repository.event.IResponseEventInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonthEventService implements IMonthEventService {
    @Autowired
    private IMonthEventRepository monthEventRepository;

    @Autowired
    private IResponseEventInformationRepository responseEventInformationRepository;
    @Autowired
    private IAccountRepository accountRepository;
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
    public MonthEvent findById(Integer id) {
        return monthEventRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteWeekEvent(Integer id) {
        responseEventInformationRepository.deleteResponseById(id);
        monthEventRepository.deleteById(id);
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