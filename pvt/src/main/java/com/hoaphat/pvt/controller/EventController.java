package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.UserValidate;
import com.hoaphat.pvt.model.dto.ResponseFilter;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.dto.MonthEventManager;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.service.ISercurityScheduleService;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import com.hoaphat.pvt.service.response.IResponseEventInformationService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private IResponseEventInformationService responseEventInformationService;

    @Autowired
    private UserValidate userValidate;

    LocalDateTime timeSetToday;

    @Scheduled(fixedRate = 1)
    public void setTime() {
        timeSetToday = LocalDateTime.now();
    }

    //     *Trang private
    @GetMapping("/home/employee/private")
    public ModelAndView showPrivate(Model model, @RequestParam(value = "selectedValue", required = false) Optional<String> name, HttpServletRequest request) {
        String nameFilter = name.orElse("");
        monthEventService.checkWeekEventDeadline(timeSetToday);
        model.addAttribute("sercuritySchedule", sercurityScheduleService.getAll());
        model.addAttribute("selectedValue", nameFilter);
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return new ModelAndView("private", "monthEventListByTime", monthEventService.getMonthEventListByFilter(timeSetToday, nameFilter));
    }

    @GetMapping("/home/employee/filterRestful")
    public ResponseEntity<ResponseFilter> showFilterRestful(@RequestParam(value = "selectedValue", required = false) String nameFilter) {
        List<MonthEvent> list = monthEventService.getMonthEventListByFilter(timeSetToday, nameFilter);
        ResponseFilter responseFilter = new ResponseFilter(list, nameFilter);
        return new ResponseEntity<>(responseFilter, HttpStatus.OK);
    }

//    *Trang response

    @GetMapping("/home/employee/private/showEventResponseForm/{id}")
    private String showEventResponseForm(@PathVariable("id") Integer id, Model model, HttpServletRequest request) {
        MonthEvent eventResponse = monthEventService.findById(id);
        model.addAttribute("response", new ResponseEventInformation(eventResponse));
        model.addAttribute("listResponse", responseEventInformationService.getAllResponseById(id));
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return ("eventResponse");
    }

    @GetMapping("/home/employee/private/showEventResponseFormRestful/{id}")
    public ResponseEntity<List<ResponseEventInformation>> showEventResponseFormRestful(@PathVariable("id") Integer id) {
        return new ResponseEntity<>(responseEventInformationService.getAllResponseById(id), HttpStatus.OK);
    }

    @PostMapping("/home/employee/private/addEventResponse")
    public String addEventResponse(@ModelAttribute("response") ResponseEventInformation responseEventInformation, Principal principal, Model model, HttpServletRequest request) {
        String name = principal.getName();
        responseEventInformation.setCreatedByUser(name);
        String escapedEventInformation = StringEscapeUtils.escapeHtml4(responseEventInformation.getEventInformationResponse());
        responseEventInformation.setEventInformationResponse(escapedEventInformation);
        responseEventInformationService.addResponseEventInformation(responseEventInformation);
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return "redirect:/home/employee/private/showEventResponseForm/" + responseEventInformation.getMonthEvent().getMonthEventId();
    }

    //    *Trang task
    @GetMapping("/home/manager/task")
    public String showTask(Model model, @RequestParam("page") Optional<Integer> page,
                           @RequestParam("size") Optional<Integer> size,
                           @RequestParam("sort") Optional<String> sort,
                           HttpServletRequest request) {
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
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return ("task");
    }

    @GetMapping("/home/manager/task/showAddMonthEventForm")
    private String showAddMonthEventForm(@RequestParam("rows") Integer rows, Model model, HttpServletRequest request) {
        List<MonthEvent> monthEvents = new ArrayList<MonthEvent>(rows);
        for (int i = 0; i < rows; i++) {
            MonthEvent monthEvent = new MonthEvent();
            monthEvents.add(monthEvent);
        }
        model.addAttribute("monthEvents", monthEvents);
        model.addAttribute("monthEventManager", new MonthEventManager());
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return ("monthEventForm");
    }

    @GetMapping("/home/manager/task/showAddPrivateEventForm")
    private String showAddPrivateEventForm(@RequestParam("rows") Integer rows, Model model, HttpServletRequest request) {
        List<MonthEvent> monthEvents = new ArrayList<MonthEvent>(rows);
        for (int i = 0; i < rows; i++) {
            MonthEvent monthEvent = new MonthEvent();
            monthEvents.add(monthEvent);
        }
        model.addAttribute("monthEvents", monthEvents);
        model.addAttribute("monthEventManager", new MonthEventManager());
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return ("privateEventForm");
    }

    @PostMapping("/home/manager/task/addMonthEvent")
    public String addMonthEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        for (MonthEvent monthEvent : monthEvents.getMonthEvents()) {
            monthEventService.addMonthEvent(monthEvent, true);
        }
        redirectAttributes.addFlashAttribute("mess", "Thêm mới thành công");
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return "redirect:/home/manager/task";
    }

    @GetMapping("/home/manager/task/showEditMonthEventForm/{id}")
    private String showEditMonthEventForm(@PathVariable("id") Integer id, Model model, HttpServletRequest request) {
        MonthEvent monthEditEvent = monthEventService.findById(id);
        model.addAttribute("monthEditEvent", monthEditEvent);
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return ("monthEditEventForm");
    }

//    CODE GỐC BAN ĐẦU CHO VIỆC LƯU EDIT MONTH EVENT
//    @PostMapping("/home/manager/task/editMonthEvent")
//    public String editMonthEvent(@ModelAttribute("monthEditEvent") MonthEvent monthEvent, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
//        // Chỉnh sửa MonthEvent
//        monthEventService.addMonthEvent(monthEvent,false);
//        // Tạo và thêm ResponseEventInformation
//        ResponseEventInformation responseEventInformation = new ResponseEventInformation();
//        responseEventInformation.setMonthEvent(monthEvent);
//        redirectAttributes.addFlashAttribute("mess", "Chỉnh sửa thành công");
//        String username = (String) request.getSession().getAttribute("username");
//        model.addAttribute("username", username);
//        return "redirect:/home/manager/task";
//    }

    @PostMapping("/home/manager/task/editMonthEvent")
    public String editMonthEvent(@ModelAttribute("monthEditEvent") MonthEvent monthEvent,
                                 RedirectAttributes redirectAttributes, Model model,
                                 HttpServletRequest request, Principal principal) {
        try {
            // Chỉnh sửa MonthEvent
            monthEventService.addMonthEvent(monthEvent, false);
            // Tạo ResponseEventInformation tự động
            ResponseEventInformation responseEventInformation = new ResponseEventInformation();
            // Lấy ngày tháng năm từ monthEvent (ví dụ monthEvent.getMonthEventDeadline())
            LocalDateTime deadline = monthEvent.getMonthEventDeadline();
            if (deadline == null) {
                throw new NullPointerException("Deadline của MonthEvent không được null.");
            }
            // Định dạng thời gian: "Báo cáo lại giờ:phút ngày/tháng/năm"
            String eventInformationResponse = "Báo cáo lại " + deadline.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
            // Thiết lập dữ liệu cho responseEventInformation
            String username = principal.getName();  // Lấy tên người dùng tự động từ Principal
            responseEventInformation.setCreatedByUser(username);  // Gán người dùng vào thông tin phản hồi
            responseEventInformation.setCreatedByDate(deadline);  // Sử dụng ngày giờ từ deadline của MonthEvent
            responseEventInformation.setEventInformationResponse(eventInformationResponse);  // Thêm thông tin báo cáo
            // Gán đối tượng MonthEvent vào ResponseEventInformation
            if (monthEvent != null) {
                responseEventInformation.setMonthEvent(monthEvent); // Giả sử ResponseEventInformation có thuộc tính MonthEvent
            } else {
                throw new NullPointerException("MonthEvent không hợp lệ.");
            }
            // Thêm ResponseEventInformation vào cơ sở dữ liệu
            responseEventInformationService.addResponseEventInformation(responseEventInformation);
            redirectAttributes.addFlashAttribute("mess", "Chỉnh sửa thành công và tạo phản hồi sự kiện tự động");
            model.addAttribute("username", username);
            return "redirect:/home/employee/private";
        } catch (NullPointerException e) {
            redirectAttributes.addFlashAttribute("mess", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/home/employee/private";
        }
    }

    @GetMapping("/home/manager/weeklyTask/delete/{id}")
    public String deleteWeekEventTask(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        monthEventService.deleteWeekEvent(id);
        redirectAttributes.addFlashAttribute("mess", "Xóa giao việc thành công");
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return "redirect:/home/manager/task";
    }

    //    *Trang weekly task
    @GetMapping("/home/manager/weeklyTask")
    public ModelAndView showWeeklyTask(Model model, HttpServletRequest request) {
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return new ModelAndView("weeklyTask", "weekEventList", monthEventService.getWeekEventList());
    }

    @PostMapping("/home/manager/weeklyTask/addWeekEvent")
    public String addWeekEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        for (MonthEvent monthEvent : monthEvents.getMonthEvents()) {
            monthEventService.addMonthEvent(monthEvent, true);
        }
        redirectAttributes.addFlashAttribute("mess", "Thêm mới thành công");
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return "redirect:/home/manager/weeklyTask";
    }

    @GetMapping("/home/manager/weeklyTask/showAddWeekEventForm")
    public String showAddWeekEventForm(@RequestParam("rows") Integer rows, Model model, HttpServletRequest request) {
        List<MonthEvent> weekEvents = new ArrayList<MonthEvent>(rows);
        for (int i = 0; i < rows; i++) {
            MonthEvent weekEvent = new MonthEvent();
            weekEvents.add(weekEvent);
        }
        model.addAttribute("weekEvents", weekEvents);
        model.addAttribute("monthEventManager", new MonthEventManager());
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return ("weekEventForm");
    }
}
