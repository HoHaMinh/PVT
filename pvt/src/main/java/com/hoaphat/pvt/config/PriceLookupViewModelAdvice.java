package com.hoaphat.pvt.config;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PriceLookupViewModelAdvice {
    private final PriceLookupProperties properties;

    public PriceLookupViewModelAdvice(PriceLookupProperties properties) {
        this.properties = properties;
    }

    @ModelAttribute
    public void addPriceLookupState(Model model) {
        model.addAttribute("priceLookupEnabled", properties.isEnabled());
    }
}
