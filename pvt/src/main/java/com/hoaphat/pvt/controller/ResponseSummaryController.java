package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.DeadlineShiftOption;
import com.hoaphat.pvt.model.dto.ResponseSummaryPeriod;
import com.hoaphat.pvt.model.dto.ResponseSummaryView;
import com.hoaphat.pvt.service.NotificationService;
import com.hoaphat.pvt.service.response.ResponseSummaryActionService;
import com.hoaphat.pvt.service.response.ResponseSummaryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
public class ResponseSummaryController {
    private final ResponseSummaryService responseSummaryService;
    private final ResponseSummaryActionService actionService;
    private final NotificationService notificationService;

    public ResponseSummaryController(ResponseSummaryService responseSummaryService,
                                     ResponseSummaryActionService actionService,
                                     NotificationService notificationService) {
        this.responseSummaryService = responseSummaryService;
        this.actionService = actionService;
        this.notificationService = notificationService;
    }

    @GetMapping("/home/employee/response-summary")
    public String showSummary(@RequestParam(value = "accountName", required = false) String accountName,
                              @RequestParam(value = "period", required = false) String periodValue,
                              Principal principal,
                              HttpServletRequest request,
                              Model model) {
        if (principal == null) {
            return "redirect:/login?expired=true";
        }
        boolean manager = request.isUserInRole("ROLE_MANAGER");
        String targetAccount = manager && accountName != null && !accountName.trim().isEmpty()
                ? accountName.trim() : principal.getName();
        ResponseSummaryPeriod period = ResponseSummaryPeriod.fromRequestValue(periodValue);
        ResponseSummaryView summary = responseSummaryService.getSummary(targetAccount, period);

        model.addAttribute("summary", summary);
        model.addAttribute("periods", ResponseSummaryPeriod.values());
        model.addAttribute("deadlineShiftOptions", DeadlineShiftOption.values());
        model.addAttribute("isManager", manager);
        model.addAttribute("selectedValue", targetAccount);
        model.addAttribute("username", request.getSession().getAttribute("username"));
        return "responseSummary";
    }

    @PostMapping("/home/manager/response-summary/shift-deadlines")
    public String shiftDeadlines(@RequestParam("accountName") String accountName,
                                 @RequestParam("period") String periodValue,
                                 @RequestParam("eventIds") List<Integer> eventIds,
                                 @RequestParam(value = "deadlineMode", required = false,
                                         defaultValue = "SHIFT") String deadlineMode,
                                 @RequestParam(value = "shiftOption", required = false) String shiftValue,
                                 @RequestParam(value = "manualDeadline", required = false) String manualDeadline,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        ResponseSummaryPeriod period = ResponseSummaryPeriod.fromRequestValue(periodValue);
        try {
            if (principal == null) {
                return "redirect:/login?expired=true";
            }
            int count;
            if ("MANUAL_DEADLINE".equalsIgnoreCase(deadlineMode)) {
                count = actionService.setDeadlines(accountName, period, eventIds,
                        parseManualDeadline(manualDeadline), principal.getName());
            } else {
                count = actionService.shiftDeadlines(accountName, period, eventIds,
                        DeadlineShiftOption.fromRequestValue(shiftValue), principal.getName());
            }
            notificationService.sendRefreshSignal();
            redirectAttributes.addFlashAttribute("mess",
                    "Đã đổi deadline cho " + count + " công việc.");
        } catch (IllegalArgumentException error) {
            redirectAttributes.addFlashAttribute("error", error.getMessage());
        }
        return redirectToSummary(accountName, period);
    }

    private LocalDateTime parseManualDeadline(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn deadline mới.");
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Deadline mới không đúng định dạng.");
        }
    }

    @PostMapping("/home/manager/response-summary/delete")
    public String moveToTrash(@RequestParam("accountName") String accountName,
                              @RequestParam("period") String periodValue,
                              @RequestParam("eventIds") List<Integer> eventIds,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        ResponseSummaryPeriod period = ResponseSummaryPeriod.fromRequestValue(periodValue);
        try {
            if (principal == null) {
                return "redirect:/login?expired=true";
            }
            int count = actionService.moveToTrash(accountName, period, eventIds);
            notificationService.sendRefreshSignal();
            redirectAttributes.addFlashAttribute("mess",
                    "Đã chuyển " + count + " công việc vào Thư mục đã xóa.");
        } catch (IllegalArgumentException error) {
            redirectAttributes.addFlashAttribute("error", error.getMessage());
        }
        return redirectToSummary(accountName, period);
    }

    private String redirectToSummary(String accountName, ResponseSummaryPeriod period) {
        String normalizedAccount = accountName == null ? "" : accountName.trim();
        return "redirect:/home/employee/response-summary?accountName="
                + UriUtils.encodeQueryParam(normalizedAccount, StandardCharsets.UTF_8)
                + "&period=" + period.name();
    }
}
