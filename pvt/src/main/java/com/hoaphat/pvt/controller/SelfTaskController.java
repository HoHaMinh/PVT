package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.SelfTaskForm;
import com.hoaphat.pvt.service.NotificationService;
import com.hoaphat.pvt.service.monthEvent.IMonthEventService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;

@Controller
public class SelfTaskController {
    private final IMonthEventService monthEventService;
    private final NotificationService notificationService;

    public SelfTaskController(IMonthEventService monthEventService,
                              NotificationService notificationService) {
        this.monthEventService = monthEventService;
        this.notificationService = notificationService;
    }

    @GetMapping("/home/employee/self-task")
    public String showForm(Model model, HttpServletRequest request) {
        requireEmployeeOnly(request);
        if (!model.containsAttribute("selfTaskForm")) {
            model.addAttribute("selfTaskForm", new SelfTaskForm());
        }
        model.addAttribute("username", request.getSession().getAttribute("username"));
        return "selfTaskForm";
    }

    @PostMapping("/home/employee/self-task")
    public String addSelfTask(@Valid @ModelAttribute("selfTaskForm") SelfTaskForm form,
                              BindingResult bindingResult,
                              Principal principal,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        requireEmployeeOnly(request);
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Nội dung hoặc deadline không đúng định dạng.");
            model.addAttribute("username", request.getSession().getAttribute("username"));
            return "selfTaskForm";
        }
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Phiên đăng nhập đã hết hạn.");
            }
            monthEventService.addSelfAssignedEvent(
                    principal.getName(), form.getDescription(), form.getDeadline());
            notificationService.sendRefreshSignal();
            redirectAttributes.addFlashAttribute("mess", "Đã giao việc cho bản thân thành công.");
            return "redirect:/home/employee/private";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", request.getSession().getAttribute("username"));
            return "selfTaskForm";
        }
    }

    private void requireEmployeeOnly(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_EMPLOYEE") || request.isUserInRole("ROLE_MANAGER")) {
            throw new AccessDeniedException("Chỉ nhân viên được tự giao việc cho bản thân.");
        }
    }
}
