package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.service.ISpecialMessageService;
import com.hoaphat.pvt.service.debt.DebtAlertStoreService;
import com.hoaphat.pvt.service.debt.DebtAlertSyncCoordinator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.security.Principal;

@Controller
public class HomeController {
    @Autowired
    private ISpecialMessageService specialMessageService;
    @Autowired
    private DebtAlertStoreService debtAlertStoreService;
    @Autowired
    private DebtAlertSyncCoordinator debtAlertSyncCoordinator;

    @GetMapping("/home")
    public ModelAndView showHome(Model model, HttpServletRequest request, Principal principal) {
        model.addAttribute("celebration", specialMessageService.getTodayCelebration(LocalDate.now()));
        model.addAttribute("debtAlertSummary", debtAlertStoreService.getDashboard(
                principal == null ? "" : principal.getName(),
                request.isUserInRole("ROLE_MANAGER"),
                debtAlertSyncCoordinator.isRunning()));
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return new ModelAndView("home");
    }
}
