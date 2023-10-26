package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.ResponseFilter;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.MonthEventManager;
import com.hoaphat.pvt.model.event.SercuritySchedule;
import com.hoaphat.pvt.service.ISercurityScheduleService;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class EventController {
    @Autowired
    private IMonthEventService monthEventService;

    @Autowired
    private ISercurityScheduleService sercurityScheduleService;

    @GetMapping("/home/manager/task")
    public String showTask(Model model, @RequestParam("page") Optional<Integer> page,
                           @RequestParam("size") Optional<Integer> size,
                           @RequestParam("sort") Optional<String> sort) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(11);
        String sortField = sort.orElse("monthEventDeadline");
        model.addAttribute("currentPage", currentPage);
        Page<MonthEvent> monthEventList = monthEventService.getMonthEventListWithPaging(PageRequest.of(currentPage - 1, pageSize, Sort.by(sortField).ascending()));
        model.addAttribute("monthEventList", monthEventList);
        int totalPages = monthEventList.getTotalPages();
        if (totalPages > 1) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        return ("task");
    }

    @GetMapping("/home/manager/task/showAddMonthEventForm")
    private String showAddMonthEventForm(@RequestParam("rows") Integer rows, Model model) {
        List<MonthEvent> monthEvents = new ArrayList<MonthEvent>(rows);
        for (int i = 0; i < rows; i++) {
            MonthEvent monthEvent = new MonthEvent();
            monthEvents.add(monthEvent);
        }
        model.addAttribute("monthEvents", monthEvents);
        model.addAttribute("monthEventManager", new MonthEventManager());
        return ("monthEventForm");
    }

    @GetMapping("/home/manager/task/showEditMonthEventForm/{id}")
    private String showEditMonthEventForm(@PathVariable("id") Integer id, Model model) {
        MonthEvent monthEditEvent = monthEventService.findById(id);
        model.addAttribute("monthEditEvent", monthEditEvent);
        return ("monthEditEventForm");
    }

    @PostMapping("/home/manager/task/addMonthEvent")
    public String addMonthEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents, RedirectAttributes redirectAttributes) {
        for (MonthEvent monthEvent : monthEvents.getMonthEvents()) {
            monthEventService.addMonthEvent(monthEvent);
        }
        redirectAttributes.addFlashAttribute("mess", "Thêm mới thành công");
        return "redirect:/home/manager/task";
    }

    @PostMapping("/home/manager/task/editMonthEvent")
    public String editMonthEvent(@ModelAttribute("monthEditEvent") MonthEvent monthEvent, RedirectAttributes redirectAttributes) {
        monthEventService.addMonthEvent(monthEvent);
        redirectAttributes.addFlashAttribute("mess", "Chỉnh sửa thành công");
        return "redirect:/home/manager/task";
    }

    LocalDateTime timeSetToday;

    @Scheduled(fixedRate = 1)
    public void setTime() {
        timeSetToday = LocalDateTime.now();
    }

    @GetMapping("/home/employee/private")
    public ModelAndView showPrivate(Model model) {
        monthEventService.checkWeekEventDeadline(timeSetToday);
        model.addAttribute("sercuritySchedule", sercurityScheduleService.getAll());
        return new ModelAndView("private", "monthEventListByTime", monthEventService.getMonthEventListByTime(timeSetToday));
    }

    @GetMapping("/home/employee/private/search")
    public ModelAndView showFilter(@RequestParam(value = "value", required = false) String name, Model model) {
        monthEventService.checkWeekEventDeadline(timeSetToday);
        model.addAttribute("sercuritySchedule", sercurityScheduleService.getAll());
        model.addAttribute("selectedName", name);
        String nameFilter = "".equals(name) ? null : name;
        return new ModelAndView("filter", "monthEventListByFilter", monthEventService.getMonthEventListByFilter(timeSetToday, nameFilter));
    }

    @GetMapping("/home/employee/privateRestful")
    public ResponseEntity<List<MonthEvent>> showPrivateRestful() {
        List<MonthEvent> list = monthEventService.getMonthEventListByTime(timeSetToday);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @GetMapping("/home/employee/filterRestful")
    public ResponseEntity<ResponseFilter> showFilterRestful(@RequestParam(value = "selectedValue", required = false) String nameFilter) {
        List<MonthEvent> list = monthEventService.getMonthEventListByFilter(timeSetToday, nameFilter);
        ResponseFilter responseFilter = new ResponseFilter(list, nameFilter);
        return new ResponseEntity<>(responseFilter, HttpStatus.OK);
    }

    @GetMapping("/home/manager/task/deleteTask")
    public String deleteMonthEventTask(RedirectAttributes redirectAttributes) {
        monthEventService.deleteMonthEvent(timeSetToday);
        redirectAttributes.addFlashAttribute("mess", "Xóa các lịch quá hạn thành công");
        return "redirect:/home/manager/task";
    }

    @GetMapping("/home/manager/weeklyTask")
    public ModelAndView showWeeklyTask() {
        return new ModelAndView("weeklyTask", "weekEventList", monthEventService.getWeekEventList());
    }

    @PostMapping("/home/manager/weeklyTask/addWeekEvent")
    public String addWeekEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents, RedirectAttributes redirectAttributes) {
        for (MonthEvent monthEvent : monthEvents.getMonthEvents()) {
            monthEventService.addMonthEvent(monthEvent);
        }
        redirectAttributes.addFlashAttribute("mess", "Thêm mới thành công");
        return "redirect:/home/manager/weeklyTask";
    }

    @GetMapping("/home/manager/weeklyTask/showAddWeekEventForm")
    public String showAddWeekEventForm(@RequestParam("rows") Integer rows, Model model) {
        List<MonthEvent> weekEvents = new ArrayList<MonthEvent>(rows);
        for (int i = 0; i < rows; i++) {
            MonthEvent weekEvent = new MonthEvent();
            weekEvents.add(weekEvent);
        }
        model.addAttribute("weekEvents", weekEvents);
        model.addAttribute("monthEventManager", new MonthEventManager());
        return ("weekEventForm");
    }

    @GetMapping("/home/manager/weeklyTask/delete/{id}")
    public String deleteWeekEventTask(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        monthEventService.deleteWeekEvent(id);
        redirectAttributes.addFlashAttribute("mess", "Xóa giao việc thành công");
        return "redirect:/home/manager/task";
    }
}
