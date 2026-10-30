package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.UserValidate;
import com.hoaphat.pvt.model.dto.*;
import com.hoaphat.pvt.model.event.*;
import com.hoaphat.pvt.repository.account.IAccountRepository;
import com.hoaphat.pvt.service.*;
import com.hoaphat.pvt.service.account.AccountService;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import com.hoaphat.pvt.service.response.IResponseEventInformationService;
import com.hoaphat.pvt.config.ResponseChatProperties;
import com.hoaphat.pvt.service.priority.WorkPriorityService;
import com.hoaphat.pvt.service.response.ResponseAttachmentStorageService;
import com.hoaphat.pvt.service.response.ResponseSummaryService;
import com.hoaphat.pvt.service.reminder.PersonalDailyReminderService;
import org.springframework.core.io.Resource;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

@Controller
public class EventController {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventController.class);

    // 🔥 ĐÃ XÓA: @Scheduled(fixedRate = 1) và biến timeSetToday
    
    @Autowired private IMonthEventService monthEventService;
    @Autowired private ISercurityScheduleService sercurityScheduleService;
    @Autowired private IResponseEventInformationService responseEventInformationService;
    @Autowired private ResponseChatProperties responseChatProperties;
    @Autowired private ResponseAttachmentStorageService responseAttachmentStorageService;
    @Autowired private UserValidate userValidate;
    @Autowired private AccountService accountService;
    @Autowired private IAccountRepository accountRepository;
    @Autowired private UserTabOrderService userTabOrderService;

    // 🔥 THÊM MỚI: Nhúng NotificationService
    @Autowired private NotificationService notificationService;
    @Autowired private WorkPriorityService workPriorityService;
    @Autowired private ResponseSummaryService responseSummaryService;
    @Autowired private PersonalDailyReminderService personalDailyReminderService;

    // 🔥 THÊM MỚI: API để giao diện kết nối vào "nghe" thông báo
    @GetMapping("/home/employee/notifications/subscribe")
    public SseEmitter subscribeToNotifications() {
        return notificationService.subscribe();
    }

    @GetMapping("/home/employee/private")
    public ModelAndView showPrivate(Model model,
                                    @RequestParam(value = "selectedValue", required = false) Optional<String> accountName,
                                    HttpServletRequest request,
                                    Principal principal) {
        String currentAccountName = principal == null ? "" : principal.getName();
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
        model.addAttribute("personalReminder",
                personalDailyReminderService.getTodayForAccount(currentAccountName));
        
        List<MonthEvent> data = monthEventService.getMonthEventListByFilter(now, filter);
        workPriorityService.applyTodayPriorities(data, filter);
        if (filter != null && !filter.trim().isEmpty()) {
            model.addAttribute("responseSummaryPreview", responseSummaryService.getTodayPreview(filter));
        }
        return new ModelAndView("private", "monthEventListByTime", data);
    }

    @GetMapping("/home/employee/filterRestful")
    public ResponseEntity<ResponseFilter> showFilterRestful(
            @RequestParam(value = "selectedValue", required = false) String nameFilter,
            Principal principal,
            HttpServletRequest request) {
        String filter;
        if (request.isUserInRole("ROLE_MANAGER")) {
            filter = nameFilter == null ? "" : nameFilter.trim();
        } else {
            filter = principal == null ? "" : principal.getName();
        }
        List<MonthEvent> list = monthEventService.getMonthEventListByFilter(LocalDateTime.now(), filter);
        workPriorityService.applyTodayPriorities(list, filter);
        ResponseSummaryPreview preview = filter.isEmpty()
                ? null : responseSummaryService.getTodayPreview(filter);
        return new ResponseEntity<>(new ResponseFilter(list, filter, preview), HttpStatus.OK);
    }

    @PostMapping("/home/employee/private/addEventResponse")
    public Object addEventResponse(@ModelAttribute("response") ResponseEventInformation res,
                                   BindingResult bindingResult,
                                   @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
                                   @RequestParam(value = "selectedValue", required = false) String selectedValue,
                                   Principal principal,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        boolean ajaxRequest = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        try {
            if (bindingResult.hasErrors()) {
                throw new IllegalArgumentException("Dữ liệu phản hồi không đúng định dạng.");
            }
            if (principal == null) {
                throw new IllegalArgumentException("Phiên đăng nhập đã hết hạn.");
            }
            res.setCreatedByUser(principal.getName());
            res.setEventInformationResponse(StringEscapeUtils.escapeHtml4(res.getEventInformationResponse()));
            responseAttachmentStorageService.addResponseWithAttachments(res, attachments);
            notificationService.sendRefreshSignal();

            if (ajaxRequest) {
                return ResponseEntity.ok(Collections.singletonMap("message", "Thêm phản hồi thành công."));
            }
            return "redirect:/home/employee/private/showEventResponseForm/"
                    + res.getMonthEvent().getMonthEventId() + "?selectedValue=" + selectedValue;
        } catch (IllegalArgumentException e) {
            if (ajaxRequest) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("message", e.getMessage()));
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("selectedValue", selectedValue == null ? "" : selectedValue);
            return "redirect:/home/employee/private";
        }
    }

    @PostMapping("/home/manager/task/addMonthEvent")
    public String addMonthEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model,
                                HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return showPrivateEventFormError(monthEvents,
                    "Deadline hoặc dữ liệu nhập vào không đúng định dạng.", model, request);
        }
        try {
            monthEventService.addMonthEvents(
                    monthEvents == null ? null : monthEvents.getMonthEvents(), 0);
        } catch (IllegalArgumentException e) {
            return showPrivateEventFormError(monthEvents, e.getMessage(), model, request);
        }
        redirectAttributes.addFlashAttribute("mess", "Thêm mới thành công");
        model.addAttribute("username", (String) request.getSession().getAttribute("username"));
        
        // 🔥 PHÁT TÍN HIỆU: Báo có việc mới
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/manager/task";
    }

    @PostMapping("/home/manager/task/editMonthEvent")
    public Object editMonthEvent(@ModelAttribute("monthEditEvent") MonthEvent m,
                                 BindingResult bindingResult,
                                 @RequestParam(value = "selectedValue", required = false) String sv,
                                 RedirectAttributes ra,
                                 Principal principal,
                                 HttpServletRequest request) {
        boolean ajaxRequest = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        String selectedValue = sv == null ? "" : sv;

        if (bindingResult.hasErrors()) {
            String message = "Deadline hoặc dữ liệu công việc không đúng định dạng.";
            if (ajaxRequest) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("message", message));
            }
            ra.addFlashAttribute("editError", message);
            if (m != null) {
                ra.addFlashAttribute("monthEditEvent", m);
            }
            ra.addAttribute("selectedValue", selectedValue);
            return m == null || m.getMonthEventId() == null
                    ? "redirect:/home/manager/task"
                    : "redirect:/home/manager/task/showEditMonthEventForm/" + m.getMonthEventId();
        }

        try {
            if (principal == null) {
                throw new IllegalArgumentException("Phiên đăng nhập đã hết hạn.");
            }
            monthEventService.editMonthEvent(m, principal.getName());
            ra.addFlashAttribute("mess", "Sửa thành công");
            notificationService.sendRefreshSignal();

            if (ajaxRequest) {
                Map<String, String> result = new HashMap<>();
                result.put("redirectUrl", "/home/employee/private?selectedValue=" + selectedValue);
                return ResponseEntity.ok(result);
            }

            ra.addAttribute("selectedValue", selectedValue);
            return "redirect:/home/employee/private";
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Không thể sửa công việc {}: {}", m.getMonthEventId(), e.getMessage());
            if (ajaxRequest) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("message", e.getMessage()));
            }
            ra.addFlashAttribute("editError", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Lỗi khi sửa công việc " + m.getMonthEventId(), e);
            String message = "Không thể lưu thay đổi. Dữ liệu bạn vừa nhập vẫn được giữ lại để thử lại.";
            if (ajaxRequest) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("message", message));
            }
            ra.addFlashAttribute("editError", message);
        }

        ra.addFlashAttribute("monthEditEvent", m);
        ra.addAttribute("selectedValue", selectedValue);
        return "redirect:/home/manager/task/showEditMonthEventForm/" + m.getMonthEventId();
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

    @PostMapping("/home/manager/weeklyTask/addWeekEvent")
    public String addWeekEvent(@ModelAttribute("monthEventManager") MonthEventManager monthEvents,
                               BindingResult bindingResult,
                               RedirectAttributes ra,
                               Model model,
                               HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return showWeekEventFormError(monthEvents,
                    "Deadline, số ngày gia hạn hoặc dữ liệu nhập vào không đúng định dạng.", model, request);
        }
        try {
            monthEventService.addMonthEvents(
                    monthEvents == null ? null : monthEvents.getMonthEvents(), 2);
        } catch (IllegalArgumentException e) {
            return showWeekEventFormError(monthEvents, e.getMessage(), model, request);
        }
        ra.addFlashAttribute("mess", "Thêm mới thành công");
        
        // 🔥 PHÁT TÍN HIỆU: Báo có việc mới
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/manager/weeklyTask";
    }
    
    @PostMapping("/home/manager/task/toggleHidden/{id}")
    public String toggleHidden(@PathVariable Integer id, RedirectAttributes ra) {
        MonthEvent event = monthEventService.findById(id);
        if (event == null) {
            ra.addFlashAttribute("error", "Công việc không còn tồn tại.");
            return "redirect:/home/manager/task";
        }
        event.setHidden(!Boolean.TRUE.equals(event.getHidden()));
        monthEventService.addMonthEvent(event, false);
        ra.addFlashAttribute("mess", "Cập nhật thành công");
        
        // 🔥 PHÁT TÍN HIỆU: Báo có thay đổi
        notificationService.sendRefreshSignal();
        
        return "redirect:/home/manager/task";
    }

    // --- CÁC HÀM CÒN LẠI KHÔNG THAY ĐỔI DỮ LIỆU NÊN KHÔNG CẦN PHÁT TÍN HIỆU ---
    @GetMapping("/home/employee/private/showEventResponseForm/{id}")
    private String showEventResponseForm(@PathVariable("id") Integer id, @RequestParam(value = "selectedValue", required = false) String selectedValue, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes, Principal principal) {
        MonthEvent eventResponse = monthEventService.findById(id);
        if (eventResponse == null) {
            redirectAttributes.addFlashAttribute("mess", "Công việc không còn tồn tại hoặc đã được chuyển vào Thư mục đã xóa.");
            redirectAttributes.addAttribute("selectedValue", selectedValue == null ? "" : selectedValue);
            return "redirect:/home/employee/private";
        }
        model.addAttribute("response", new ResponseEventInformation(eventResponse));
        model.addAttribute("listResponse", responseEventInformationService.getAllResponseById(id));
        model.addAttribute("username", (String) request.getSession().getAttribute("username"));
        model.addAttribute("currentAccountName", principal == null ? "" : principal.getName());
        model.addAttribute("selectedValue", selectedValue);
        model.addAttribute("responseChatMaxImages", responseChatProperties.getMaxImagesPerMessage());
        model.addAttribute("responseChatMaxAttachments", responseChatProperties.getMaxAttachmentsPerMessage());
        model.addAttribute("responseChatMaxAttachmentSizeBytes", responseChatProperties.getMaxAttachmentSizeBytes());
        return responseChatProperties.isEnabled() ? "eventResponseChat" : "eventResponse";
    }
    @GetMapping("/home/employee/private/showEventResponseFormRestful/{id}")
    public ResponseEntity<List<ResponseChatMessageView>> showEventResponseFormRestful(@PathVariable("id") Integer id) {
        if (monthEventService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return new ResponseEntity<>(
                responseAttachmentStorageService.toChatMessageViews(
                        responseEventInformationService.getAllResponseById(id)),
                HttpStatus.OK);
    }
    @GetMapping("/home/employee/private/response-attachment/{id}")
    public ResponseEntity<Resource> showResponseAttachment(@PathVariable("id") Integer id) {
        try {
            ResponseAttachmentStorageService.AttachmentFile attachmentFile =
                    responseAttachmentStorageService.loadAttachment(id);
            MediaType mediaType = MediaType.parseMediaType(
                    attachmentFile.getAttachment().getContentType());
            String fileName = attachmentFile.getAttachment().getOriginalFileName()
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\"", "");
            ContentDisposition disposition = ContentDisposition.inline()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            disposition.toString())
                    .body(attachmentFile.getResource());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
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
        if (!model.containsAttribute("monthEditEvent")) {
            model.addAttribute("monthEditEvent", monthEventService.findById(id));
        }
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
    @PostMapping("/home/manager/delete-account") public String deleteAccount(@RequestParam(required = false) String accountName, Principal principal, RedirectAttributes ra) {
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Phiên đăng nhập đã hết hạn.");
            }
            if (accountName == null || accountName.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn tài khoản cần xóa.");
            }
            if (accountName.trim().equals(principal.getName())) {
                throw new IllegalArgumentException("Không thể xóa chính bạn.");
            }
            accountService.deleteAccountFull(accountName);
            ra.addFlashAttribute("message", "Xóa thành công");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Không thể xóa tài khoản " + accountName, e);
            ra.addFlashAttribute("error",
                    "Không thể xóa tài khoản. Dữ liệu chưa bị thay đổi; vui lòng kiểm tra log máy chủ.");
        }
        return "redirect:/home/manager/delete-account";
    }
    @PostMapping("/home/manager/saveTabOrder")
    @ResponseBody
    public ResponseEntity<?> saveTabOrder(@RequestBody(required = false) List<String> order,
                                          HttpServletRequest request) {
        try {
            userTabOrderService.saveOrder(
                    (String) request.getSession().getAttribute("username"), order);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    private String showPrivateEventFormError(MonthEventManager manager, String message,
                                             Model model, HttpServletRequest request) {
        List<MonthEvent> events = manager == null || manager.getMonthEvents() == null
                ? Collections.emptyList() : manager.getMonthEvents();
        model.addAttribute("error", message);
        model.addAttribute("monthEventManager", manager == null ? new MonthEventManager() : manager);
        model.addAttribute("monthEvents", events);
        model.addAttribute("accounts", accountService.findAll());
        model.addAttribute("username", request.getSession().getAttribute("username"));
        return "privateEventForm";
    }

    private String showWeekEventFormError(MonthEventManager manager, String message,
                                          Model model, HttpServletRequest request) {
        List<MonthEvent> events = manager == null || manager.getMonthEvents() == null
                ? Collections.emptyList() : manager.getMonthEvents();
        model.addAttribute("error", message);
        model.addAttribute("monthEventManager", manager == null ? new MonthEventManager() : manager);
        model.addAttribute("weekEvents", events);
        model.addAttribute("accounts", accountService.findAll());
        model.addAttribute("username", request.getSession().getAttribute("username"));
        return "weekEventForm";
    }
}
