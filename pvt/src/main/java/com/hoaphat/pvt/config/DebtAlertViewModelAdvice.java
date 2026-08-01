package com.hoaphat.pvt.config;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class DebtAlertViewModelAdvice {
    private final DebtAlertProperties properties;

    public DebtAlertViewModelAdvice(DebtAlertProperties properties) {
        this.properties = properties;
    }

    @ModelAttribute
    public void addDebtAlertFlags(Model model) {
        model.addAttribute("debtAlertEnabled", properties.isEnabled());
    }
}
