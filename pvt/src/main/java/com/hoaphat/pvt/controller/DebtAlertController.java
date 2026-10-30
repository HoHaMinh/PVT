package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.debt.DebtAlertDashboard;
import com.hoaphat.pvt.service.debt.DebtAlertStoreService;
import com.hoaphat.pvt.service.debt.DebtAlertSyncCoordinator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@Controller
public class DebtAlertController {
    private final DebtAlertStoreService storeService;
    private final DebtAlertSyncCoordinator syncCoordinator;

    public DebtAlertController(DebtAlertStoreService storeService,
                               DebtAlertSyncCoordinator syncCoordinator) {
        this.storeService = storeService;
        this.syncCoordinator = syncCoordinator;
    }

    @GetMapping("/home/employee/debt-alerts")
    public String showDebtAlerts(Model model, Principal principal,
                                 HttpServletRequest request) {
        boolean manager = request.isUserInRole("ROLE_MANAGER");
        String accountName = principal == null ? "" : principal.getName();
        DebtAlertDashboard dashboard = storeService.getDashboard(
                accountName, manager, syncCoordinator.isRunning());
        model.addAttribute("debtDashboard", dashboard);
        model.addAttribute("isManager", manager);
        model.addAttribute("username", request.getSession().getAttribute("username"));
        return "debtAlerts";
    }

    @PostMapping("/home/manager/debt-alerts/sync")
    public String forceSync(RedirectAttributes redirectAttributes) {
        boolean started = syncCoordinator.trigger(true);
        if (started) {
            redirectAttributes.addFlashAttribute("mess",
                    "Đã bắt đầu quét lại Google Drive. Trang sẽ tự cập nhật trạng thái.");
        } else if (syncCoordinator.isRunning()) {
            redirectAttributes.addFlashAttribute("mess",
                    "Hệ thống đang quét Google Drive, vui lòng chờ hoàn tất.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Chưa bật hoặc chưa cấu hình đầy đủ kết nối Apps Script.");
        }
        return "redirect:/home/employee/debt-alerts";
    }

    @PostMapping("/home/manager/debt-alerts/import-alerts/ack")
    public String acknowledgeImportAlert(@RequestParam("alertKey") String alertKey,
                                         Principal principal,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {
        boolean manager = request.isUserInRole("ROLE_MANAGER");
        String accountName = principal == null ? "" : principal.getName();
        try {
            storeService.acknowledgeImportAlert(alertKey, accountName, manager);
            redirectAttributes.addFlashAttribute("mess",
                    "Đã xác nhận hồ sơ NK này đã được báo cho kế toán.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home/employee/debt-alerts";
    }
}
