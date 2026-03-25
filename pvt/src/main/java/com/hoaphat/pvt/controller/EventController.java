package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.UserValidate;
import com.hoaphat.pvt.model.dto.MonthEventManager;
import com.hoaphat.pvt.model.dto.ResponseFilter;
import com.hoaphat.pvt.model.dto.UserDisplayDTO;
import com.hoaphat.pvt.model.event.MonthEvent;
import com.hoaphat.pvt.model.event.ResponseEventInformation;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.service.ISercurityScheduleService;
import com.hoaphat.pvt.service.UserTabOrderService;
import com.hoaphat.pvt.service.account.AccountService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class EventController {
    LocalDateTime timeSetToday;
    @Autowired
    private IMonthEventService monthEventService;
    @Autowired
    private ISercurityScheduleService sercurityScheduleService;
    @Autowired
    private IResponseEventInformationService responseEventInformationService;
    @Autowired
    private UserValidate userValidate;
    @Autowired
    private AccountService accountService;
    @Autowired
    private IAccountRepository accountRepository;
    @Autowired
    private UserTabOrderService userTabOrderService;

    @Scheduled(fixedRate = 1)
    public void setTime() {
        timeSetToday = LocalDateTime.now();
    }

//    *Trang response

    //     *Trang private
    @GetMapping("/home/employee/private")
    public ModelAndView showPrivate(Model model,
                                    @RequestParam(value = "selectedValue", required = false) Optional<String> accountName,
                                    HttpServletRequest request) {

        String currentAccountName = (String) request.getSession().getAttribute("accountName");

        boolean isManager = request.isUserInRole("ROLE_MANAGER");
        model.addAttribute("isManager", isManager);

        String filter;

        if (isManager) {
            // Manager → giữ logic cũ
            filter = accountName.orElse("");
        } else {
            // Employee → luôn filter theo chính nó
            filter = currentAccountName;
        }

        // ⭐ QUAN TRỌNG: set lại selectedValue
        model.addAttribute("selectedValue", filter);

        // giữ nguyên
        monthEventService.checkWeekEventDeadline(timeSetToday);
        model.addAttribute("sercuritySchedule", sercurityScheduleService.getAll());

        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);

        List<UserDisplayDTO> userTabs = accountService.getUserDisplayList();

        if (isManager) {

            List<String> ordered = userTabOrderService.getOrderedTabs(username);

            if (!ordered.isEmpty()) {
                userTabs = userTabs.stream()
                        .sorted(Comparator.comparingInt(a -> {
                            int idx = ordered.indexOf(a.getAccountName());
                            return idx == -1 ? 999 : idx;
                        }))
                        .collect(Collectors.toList());
            }
        }
        model.addAttribute("userTabs", userTabs);
        model.addAttribute("accountName", currentAccountName);

        List<MonthEvent> data = monthEventService.getMonthEventListByFilter(timeSetToday, filter);

        return new ModelAndView("private", "monthEventListByTime", data);
    }

    @GetMapping("/home/employee/filterRestful")
    public ResponseEntity<ResponseFilter> showFilterRestful(@RequestParam(value = "selectedValue", required = false) String nameFilter) {

        if (nameFilter == null) {
            nameFilter = "";
        }

        List<MonthEvent> list = monthEventService.getMonthEventListByFilter(timeSetToday, nameFilter);

        ResponseFilter responseFilter = new ResponseFilter(list, nameFilter);
        return new ResponseEntity<>(responseFilter, HttpStatus.OK);
    }

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
    public String showTask(Model model, @RequestParam("page") Optional<Integer> page, @RequestParam("size") Optional<Integer> size, @RequestParam("sort") Optional<String> sort, HttpServletRequest request) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(11);
        String sortField = sort.orElse("monthEventDeadline");
        model.addAttribute("currentPage", currentPage);
        Page<MonthEvent> monthEventList = monthEventService.getMonthEventListWithPaging(PageRequest.of(currentPage - 1, pageSize, Sort.by(sortField).ascending()));
        model.addAttribute("monthEventList", monthEventList);
        int totalPages = monthEventList.getTotalPages();
        if (totalPages > 1) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
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
        MonthEventManager manager = new MonthEventManager();
        manager.setMonthEvents((ArrayList<MonthEvent>) monthEvents); // ⭐ DÒNG QUAN TRỌNG

        model.addAttribute("monthEventManager", manager);
        model.addAttribute("monthEvents", monthEvents);
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("accounts", accountService.findAll());
        return ("privateEventForm");
    }

    @PostMapping("/home/manager/task/addMonthEvent")
    public String addMonthEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents,
                                RedirectAttributes redirectAttributes,
                                Model model,
                                HttpServletRequest request) {

        for (MonthEvent monthEvent : monthEvents.getMonthEvents()) {

            if (monthEvent.getAccount() != null &&
                    "ALL".equals(monthEvent.getAccount().getAccountName())) {

                // ✅ chọn "Cả phòng"
                monthEvent.setAll(true);
                monthEvent.setAccount(null); // tránh lỗi FK

            } else {
                monthEvent.setAll(false);
            }

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
        model.addAttribute("accountList", accountService.findAll()); // ⭐ THÊM DÒNG NÀY

        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);

        return ("monthEditEventForm");
    }

    @PostMapping("/home/manager/task/editMonthEvent")
    public String editMonthEvent(@ModelAttribute("monthEditEvent") MonthEvent monthEvent,
                                 RedirectAttributes redirectAttributes,
                                 Model model,
                                 HttpServletRequest request,
                                 Principal principal) {

        try {
            MonthEvent oldEvent = monthEventService.findById(monthEvent.getMonthEventId());

            if (oldEvent != null) {

                // ✅ XỬ LÝ CẢ PHÒNG
                if (monthEvent.getAccount() != null &&
                        "ALL".equals(monthEvent.getAccount().getAccountName())) {

                    monthEvent.setAll(true);
                    monthEvent.setAccount(null);

                } else {

                    // ❗ Nếu không chọn → giữ nguyên
                    if (monthEvent.getAccount() == null ||
                            monthEvent.getAccount().getAccountName() == null ||
                            monthEvent.getAccount().getAccountName().isEmpty()) {

                        monthEvent.setAccount(oldEvent.getAccount());
                        monthEvent.setAll(oldEvent.getAll());

                    } else {
                        // chọn user → tắt ALL
                        monthEvent.setAll(false);
                    }
                }
            }

            // ✅ Lưu
            monthEventService.addMonthEvent(monthEvent, false);

            // ================== GIỮ NGUYÊN LOGIC CỦA BẠN ==================

            ResponseEventInformation responseEventInformation = new ResponseEventInformation();

            LocalDateTime deadline = monthEvent.getMonthEventDeadline();
            if (deadline == null) {
                throw new NullPointerException("Deadline của MonthEvent không được null.");
            }

            String eventInformationResponse =
                    "Báo cáo lại " + deadline.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));

            String username = principal.getName();

            responseEventInformation.setCreatedByUser(username);
            responseEventInformation.setCreatedByDate(deadline);
            responseEventInformation.setEventInformationResponse(eventInformationResponse);
            responseEventInformation.setMonthEvent(monthEvent);

            responseEventInformationService.addResponseEventInformation(responseEventInformation);

            redirectAttributes.addFlashAttribute("mess", "Chỉnh sửa thành công");
            model.addAttribute("username", username);

            return "redirect:/home/employee/private";

        } catch (NullPointerException e) {
            redirectAttributes.addFlashAttribute("mess", "Có lỗi: " + e.getMessage());
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
    public String addWeekEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents,
                               RedirectAttributes redirectAttributes,
                               Model model,
                               HttpServletRequest request) {

        for (MonthEvent monthEvent : monthEvents.getMonthEvents()) {

            // ⭐ XỬ LÝ "CẢ PHÒNG"
            if (monthEvent.getAccount() != null &&
                    "ALL".equals(monthEvent.getAccount().getAccountName())) {

                monthEvent.setAll(true);
                monthEvent.setAccount(null); // hoặc giữ cũng được nhưng null là sạch nhất

            } else {
                monthEvent.setAll(false);
            }

            // ⭐ Lưu như cũ
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
        MonthEventManager manager = new MonthEventManager();
        manager.setMonthEvents((ArrayList<MonthEvent>) weekEvents); // ⭐ DÒNG QUAN TRỌNG

        model.addAttribute("monthEventManager", manager);
        model.addAttribute("weekEvents", weekEvents);
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("accounts", accountService.findAll());
        return ("weekEventForm");
    }
    @GetMapping("/home/manager/delete-account")
    public String showDeleteAccountPage(Model model) {
        model.addAttribute("accounts", accountService.findAll());
        return "deleteAccount"; // tên file HTML
    }
    @PostMapping("/home/manager/delete-account")
    public String deleteAccount(@RequestParam String accountName,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {

        if (accountName.equals(principal.getName())) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa chính bạn");
            return "redirect:/home/manager/delete-account";
        }

        accountService.deleteAccountFull(accountName);

        redirectAttributes.addFlashAttribute("message", "Xóa thành công");
        return "redirect:/home/manager/delete-account";
    }

    @PostMapping("/home/manager/task/toggleHidden/{id}")
    public String toggleHidden(@PathVariable Integer id,
                               RedirectAttributes redirectAttributes) {

        MonthEvent event = monthEventService.findById(id);

        if (event != null) {
            event.setHidden(!Boolean.TRUE.equals(event.getHidden()));
            monthEventService.addMonthEvent(event, false);
        }

        redirectAttributes.addFlashAttribute("mess", "Cập nhật trạng thái thành công");

        return "redirect:/home/manager/task";
    }

    @GetMapping("/home/manager/task/hide/{id}")
    public String hideEvent(@PathVariable Integer id) {
        MonthEvent event = monthEventService.findById(id);
        event.setHidden(true);
        monthEventService.addMonthEvent(event, false);
        return "redirect:/home/employee/private";
    }

    @GetMapping("/home/manager/task/show/{id}")
    public String showEvent(@PathVariable Integer id) {
        MonthEvent event = monthEventService.findById(id);
        event.setHidden(false);
        monthEventService.addMonthEvent(event, false);
        return "redirect:/home/employee/private";
    }

    @PostMapping("/home/manager/saveTabOrder")
    @ResponseBody
    public ResponseEntity<?> saveTabOrder(@RequestBody List<String> orderedTabs,
                                          HttpServletRequest request) {

        String username = (String) request.getSession().getAttribute("username");

        userTabOrderService.saveOrder(username, orderedTabs);

        return ResponseEntity.ok().build();
    }
}
