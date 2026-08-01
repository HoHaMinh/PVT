package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.DailyPriorityForm;
import com.hoaphat.pvt.service.NotificationService;
import com.hoaphat.pvt.service.account.AccountService;
import com.hoaphat.pvt.service.priority.WorkPriorityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@Controller
public class PriorityController {
    private final WorkPriorityService workPriorityService;
    private final AccountService accountService;
    private final NotificationService notificationService;

    public PriorityController(WorkPriorityService workPriorityService,
                              AccountService accountService,
                              NotificationService notificationService) {
        this.workPriorityService = workPriorityService;
        this.accountService = accountService;
        this.notificationService = notificationService;
    }

    @GetMapping("/home/employee/priorities")
    public String showEmployeePriorities(Model model, Principal principal,
                                         HttpServletRequest request) {
        requireEmployeeOnly(request);
        if (principal == null) {
            throw new AccessDeniedException("Phiên đăng nhập đã hết hạn.");
        }
        model.addAttribute("priorityForm", workPriorityService.getTodayForm(principal.getName()));
        populateCommonModel(model, request, false);
        return "priority";
    }

    @PostMapping("/home/employee/priorities")
    public String saveEmployeePriorities(
            @ModelAttribute("priorityForm") DailyPriorityForm form,
            Principal principal,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        requireEmployeeOnly(request);
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Phiên đăng nhập đã hết hạn.");
            }
            workPriorityService.submitEmployeePriorities(principal.getName(), form.getTasks());
            notificationService.sendRefreshSignal();
            redirectAttributes.addFlashAttribute("mess", "Đã lưu ưu tiên công việc hôm nay.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home/employee/priorities";
    }

    @GetMapping("/home/manager/priorities")
    public String showManagerPriorities(
            @RequestParam(value = "accountName", required = false) String accountName,
            Model model,
            HttpServletRequest request) {
        model.addAttribute("accounts", accountService.findAll());
        model.addAttribute("selectedAccountName", accountName == null ? "" : accountName.trim());
        if (accountName != null && !accountName.trim().isEmpty()) {
            model.addAttribute("priorityForm", workPriorityService.getTodayForm(accountName));
        }
        populateCommonModel(model, request, true);
        return "priority";
    }

    @PostMapping("/home/manager/priorities")
    public String saveManagerPriorities(
            @ModelAttribute("priorityForm") DailyPriorityForm form,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String target = form == null ? null : form.getAccountName();
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Phiên đăng nhập đã hết hạn.");
            }
            workPriorityService.saveManagerPriorities(
                    principal.getName(), target, form == null ? null : form.getTasks());
            notificationService.sendRefreshSignal();
            redirectAttributes.addFlashAttribute("mess", "Đã cập nhật ưu tiên công việc.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (target != null && !target.trim().isEmpty()) {
            redirectAttributes.addAttribute("accountName", target.trim());
        }
        return "redirect:/home/manager/priorities";
    }

    private void populateCommonModel(Model model, HttpServletRequest request, boolean manager) {
        model.addAttribute("isManagerPriority", manager);
        model.addAttribute("username", request.getSession().getAttribute("username"));
    }

    private void requireEmployeeOnly(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_EMPLOYEE") || request.isUserInRole("ROLE_MANAGER")) {
            throw new AccessDeniedException(
                    "Quản lý vui lòng sử dụng chức năng Quản lý ưu tiên công việc.");
        }
    }
}
