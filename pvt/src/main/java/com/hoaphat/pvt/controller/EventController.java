package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.UserValidate;
import com.hoaphat.pvt.model.dto.*;
import com.hoaphat.pvt.model.event.*;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.service.*;
import com.hoaphat.pvt.service.account.AccountService;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import com.hoaphat.pvt.service.response.IResponseEventInformationService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

@Controller
public class EventController {
    // 🔥 ĐÃ XÓA: @Scheduled(fixedRate = 1) và biến timeSetToday
    
    @Autowired private IMonthEventService monthEventService;
    @Autowired private ISercurityScheduleService sercurityScheduleService;
    @Autowired private IResponseEventInformationService responseEventInformationService;
    @Autowired private UserValidate userValidate;
    @Autowired private AccountService accountService;
    @Autowired private IAccountRepository accountRepository;
    @Autowired private UserTabOrderService userTabOrderService;

    // 🔥 THÊM MỚI: Nhúng NotificationService
    @Autowired private NotificationService notificationService;

    // 🔥 THÊM MỚI: API để giao diện kết nối vào "nghe" thông báo
    @GetMapping("/home/employee/notifications/subscribe")
    public SseEmitter subscribeToNotifications() {
        return notificationService.subscribe();
    }

    @GetMapping("/home/employee/private")
    public ModelAndView showPrivate(Model model, @RequestParam(value = "selectedValue", required = false) Optional<String> accountName, HttpServletRequest request) {
        String currentAccountName = (String) request.getSession().getAttribute("accountName");
        boolean isManager = request.isUserInRole("ROLE_MANAGER");
        model.addAttribute("isManager", isManager);
        String filter = isManager ? accountName.orElse("") : currentAccountName;
        model.addAttribute("selectedValue", filter);
        
        LocalDateTime now = LocalDateTime.now();
        monthEventService.checkWeekEventDeadline(now);
        
        model.addAttribute("sercuritySchedule", sercurityScheduleService.getAll());
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        List<UserDisplayDTO> userTabs = accountService.getUserDisplayList();
        if (isManager) {
            List<String> ordered = userTabOrderService.getOrderedTabs(username);
            if (!ordered.isEmpty()) { userTabs = userTabs.stream().sorted(Comparator.comparingInt(a -> { int idx = ordered.indexOf(a.getAccountName()); return idx == -1 ? 999 : idx; })).collect(Collectors.toList()); }
        }
        model.addAttribute("userTabs", userTabs);
        model.addAttribute("accountName", currentAccountName);
        
        List<MonthEvent> data = monthEventService.getMonthEventListByFilter(now, filter);
        return new ModelAndView("private", "monthEventListByTime", data);
    }

    @GetMapping("/home/employee/filterRestful")
    public ResponseEntity<ResponseFilter> showFilterRestful(@RequestParam(value = "selectedValue", required = false) String nameFilter) {
        if (nameFilter == null) { nameFilter = ""; }
        List<MonthEvent> list = monthEventService.getMonthEventListByFilter(LocalDateTime.now(), nameFilter);
        return new ResponseEntity<>(new ResponseFilter(list, nameFilter), HttpStatus.OK);
    }

    @PostMapping("/home/employee/private/addEventResponse")
    public String addEventResponse(@ModelAttribute("response") ResponseEventInformation res, @RequestParam(value = "selectedValue", required = false) String selectedValue, Principal principal) {
        res.setCreatedByUser(principal.getName());
        res.setEventInformationResponse(StringEscapeUtils.escapeHtml4(res.getEventInformationResponse()));
        responseEventInformationService.addResponseEventInformation(res);
        
        // 🔥 PHÁT TÍN HIỆU: Báo cho mọi người biết có phản hồi mới
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/employee/private/showEventResponseForm/" + res.getMonthEvent().getMonthEventId() + "?selectedValue=" + selectedValue;
    }

    @PostMapping("/home/manager/task/addMonthEvent")
    public String addMonthEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        for (MonthEvent monthEvent : monthEvents.getMonthEvents()) {
            if (monthEvent.getAccount() != null && "ALL".equals(monthEvent.getAccount().getAccountName())) { monthEvent.setAll(true); monthEvent.setAccount(null); }
            else { monthEvent.setAll(false); }
            monthEventService.addMonthEvent(monthEvent, true);
        }
        redirectAttributes.addFlashAttribute("mess", "Thêm mới thành công");
        model.addAttribute("username", (String) request.getSession().getAttribute("username"));
        
        // 🔥 PHÁT TÍN HIỆU: Báo có việc mới
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/manager/task";
    }

    @PostMapping("/home/manager/task/editMonthEvent")
    public String editMonthEvent(@ModelAttribute("monthEditEvent") MonthEvent m, @RequestParam(value = "selectedValue", required = false) String sv, RedirectAttributes ra, Principal principal) {
        try {
            MonthEvent old = monthEventService.findById(m.getMonthEventId());
            if (old != null) {
                if (m.getAccount() != null && "ALL".equals(m.getAccount().getAccountName())) { m.setAll(true); m.setAccount(null); }
                else if (m.getAccount() == null || m.getAccount().getAccountName() == null || m.getAccount().getAccountName().isEmpty()) { m.setAccount(old.getAccount()); m.setAll(old.getAll()); }
                else { m.setAll(false); }
            }
            monthEventService.addMonthEvent(m, false);
            ResponseEventInformation res = new ResponseEventInformation();
            LocalDateTime dl = m.getMonthEventDeadline();
            res.setCreatedByUser(principal.getName()); res.setCreatedByDate(dl);
            res.setEventInformationResponse("Báo cáo lại " + dl.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            res.setMonthEvent(m);
            responseEventInformationService.addResponseEventInformation(res);
            ra.addFlashAttribute("mess", "Sửa thành công");
            
            // 🔥 PHÁT TÍN HIỆU: Báo có thay đổi
            notificationService.sendRefreshSignal();
            
            return "redirect:/home/employee/private?selectedValue=" + sv;
        } catch (Exception e) { return "redirect:/home/employee/private?selectedValue=" + sv; }
    }

    @GetMapping("/home/manager/task/hide/{id}")
    public String hideEvent(@PathVariable Integer id, @RequestParam(value = "selectedValue", required = false) String sv) {
        MonthEvent e = monthEventService.findById(id); e.setHidden(true); monthEventService.addMonthEvent(e, false);
        
        // 🔥 PHÁT TÍN HIỆU: Báo có thay đổi
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/employee/private?selectedValue=" + sv;
    }

    @GetMapping("/home/manager/task/show/{id}")
    public String showEvent(@PathVariable Integer id, @RequestParam(value = "selectedValue", required = false) String sv) {
        MonthEvent e = monthEventService.findById(id); e.setHidden(false); monthEventService.addMonthEvent(e, false);
        
        // 🔥 PHÁT TÍN HIỆU: Báo có thay đổi
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/employee/private?selectedValue=" + sv;
    }

    @GetMapping("/home/manager/weeklyTask/delete/{id}")
    public String deleteWeekEventTask(@PathVariable("id") Integer id, @RequestParam(value = "selectedValue", required = false) String sv, RedirectAttributes ra) {
        monthEventService.deleteWeekEvent(id); ra.addFlashAttribute("mess", "Xóa thành công");
        
        // 🔥 PHÁT TÍN HIỆU: Báo có thay đổi
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/employee/private?selectedValue=" + sv;
    }

    @PostMapping("/home/manager/weeklyTask/addWeekEvent")
    public String addWeekEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents, RedirectAttributes ra) {
        for (MonthEvent monthEvent : monthEvents.getMonthEvents()) {
            if (monthEvent.getAccount() != null && "ALL".equals(monthEvent.getAccount().getAccountName())) { monthEvent.setAll(true); monthEvent.setAccount(null); }
            else { monthEvent.setAll(false); }
            monthEventService.addMonthEvent(monthEvent, true);
        }
        ra.addFlashAttribute("mess", "Thêm mới thành công");
        
        // 🔥 PHÁT TÍN HIỆU: Báo có việc mới
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/manager/weeklyTask";
    }
    
    @PostMapping("/home/manager/task/toggleHidden/{id}")
    public String toggleHidden(@PathVariable Integer id, RedirectAttributes ra) {
        MonthEvent event = monthEventService.findById(id);
        if (event != null) { event.setHidden(!Boolean.TRUE.equals(event.getHidden())); monthEventService.addMonthEvent(event, false); }
        ra.addFlashAttribute("mess", "Cập nhật thành công");
        
        // 🔥 PHÁT TÍN HIỆU: Báo có thay đổi
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/manager/task";
    }

    // --- CÁC HÀM CÒN LẠI KHÔNG THAY ĐỔI DỮ LIỆU NÊN KHÔNG CẦN PHÁT TÍN HIỆU ---
    @GetMapping("/home/employee/private/showEventResponseForm/{id}")
    private String showEventResponseForm(@PathVariable("id") Integer id, @RequestParam(value = "selectedValue", required = false) String selectedValue, Model model, HttpServletRequest request) {
        MonthEvent eventResponse = monthEventService.findById(id);
        model.addAttribute("response", new ResponseEventInformation(eventResponse));
        model.addAttribute("listResponse", responseEventInformationService.getAllResponseById(id));
        model.addAttribute("username", (String) request.getSession().getAttribute("username"));
        model.addAttribute("selectedValue", selectedValue);
        return "eventResponse";
    }
    @GetMapping("/home/employee/private/showEventResponseFormRestful/{id}")
    public ResponseEntity<List<ResponseEventInformation>> showEventResponseFormRestful(@PathVariable("id") Integer id) { return new ResponseEntity<>(responseEventInformationService.getAllResponseById(id), HttpStatus.OK); }
    @GetMapping("/home/manager/task")
    public String showTask(Model model, @RequestParam("page") Optional<Integer> page, @RequestParam("size") Optional<Integer> size, @RequestParam("sort") Optional<String> sort, HttpServletRequest request) {
        int currentPage = page.orElse(1);
        Page<MonthEvent> monthEventList = monthEventService.getMonthEventListWithPaging(PageRequest.of(currentPage - 1, size.orElse(11), Sort.by(sort.orElse("monthEventDeadline")).ascending()));
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("monthEventList", monthEventList);
        if (monthEventList.getTotalPages() > 1) model.addAttribute("pageNumbers", IntStream.rangeClosed(1, monthEventList.getTotalPages()).boxed().collect(Collectors.toList()));
        model.addAttribute("username", (String) request.getSession().getAttribute("username"));
        return "task";
    }
    @GetMapping("/home/manager/task/showAddMonthEventForm")
    private String showAddMonthEventForm(@RequestParam("rows") Integer rows, Model model, HttpServletRequest request) {
        List<MonthEvent> monthEvents = new ArrayList<MonthEvent>(rows);
        for (int i = 0; i < rows; i++) { monthEvents.add(new MonthEvent()); }
        model.addAttribute("monthEvents", monthEvents);
        model.addAttribute("monthEventManager", new MonthEventManager());
        model.addAttribute("username", (String) request.getSession().getAttribute("username"));
        return "monthEventForm";
    }
    @GetMapping("/home/manager/task/showAddPrivateEventForm")
    private String showAddPrivateEventForm(@RequestParam("rows") Integer rows, Model model, HttpServletRequest request) {
        List<MonthEvent> monthEvents = new ArrayList<MonthEvent>(rows);
        for (int i = 0; i < rows; i++) { monthEvents.add(new MonthEvent()); }
        MonthEventManager manager = new MonthEventManager();
        manager.setMonthEvents((ArrayList<MonthEvent>) monthEvents);
        model.addAttribute("monthEventManager", manager);
        model.addAttribute("monthEvents", monthEvents);
        model.addAttribute("accounts", accountService.findAll());
        model.addAttribute("username", (String) request.getSession().getAttribute("username"));
        return "privateEventForm";
    }
    @GetMapping("/home/manager/task/showEditMonthEventForm/{id}")
    private String showEditMonthEventForm(@PathVariable("id") Integer id, @RequestParam(value = "selectedValue", required = false) String sv, Model model, HttpServletRequest request) {
        model.addAttribute("monthEditEvent", monthEventService.findById(id));
        model.addAttribute("accountList", accountService.findAll());
        model.addAttribute("username", (String) request.getSession().getAttribute("username"));
        model.addAttribute("selectedValue", sv);
        return "monthEditEventForm";
    }
    @GetMapping("/home/manager/weeklyTask")
    public ModelAndView showWeeklyTask(Model model, HttpServletRequest request) { model.addAttribute("username", (String) request.getSession().getAttribute("username")); return new ModelAndView("weeklyTask", "weekEventList", monthEventService.getWeekEventList()); }
    @GetMapping("/home/manager/weeklyTask/showAddWeekEventForm")
    public String showAddWeekEventForm(@RequestParam("rows") Integer rows, Model model) {
        List<MonthEvent> weekEvents = new ArrayList<MonthEvent>(rows);
        for (int i = 0; i < rows; i++) { weekEvents.add(new MonthEvent()); }
        MonthEventManager manager = new MonthEventManager(); manager.setMonthEvents((ArrayList<MonthEvent>) weekEvents);
        model.addAttribute("monthEventManager", manager); model.addAttribute("weekEvents", weekEvents);
        model.addAttribute("accounts", accountService.findAll());
        return "weekEventForm";
    }
    @GetMapping("/home/manager/delete-account") public String showDeleteAccountPage(Model model) { model.addAttribute("accounts", accountService.findAll()); return "deleteAccount"; }
    @PostMapping("/home/manager/delete-account") public String deleteAccount(@RequestParam String accountName, Principal principal, RedirectAttributes ra) {
        if (accountName.equals(principal.getName())) { ra.addFlashAttribute("error", "Không thể xóa chính bạn"); return "redirect:/home/manager/delete-account"; }
        accountService.deleteAccountFull(accountName); ra.addFlashAttribute("message", "Xóa thành công");
        return "redirect:/home/manager/delete-account";
    }
    @PostMapping("/home/manager/saveTabOrder") @ResponseBody public ResponseEntity<?> saveTabOrder(@RequestBody List<String> ots, HttpServletRequest req) { userTabOrderService.saveOrder((String) req.getSession().getAttribute("username"), ots); return ResponseEntity.ok().build(); }
}