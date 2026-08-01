package com.hoaphat.pvt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SiteViewModelAdvice {
    private final String siteName;

    public SiteViewModelAdvice(@Value("${app.site-name}") String siteName) {
        if (siteName == null || siteName.trim().isEmpty()) {
            throw new IllegalArgumentException("app.site-name không được để trống.");
        }
        this.siteName = siteName.trim();
    }

    @ModelAttribute
    public void addSiteName(Model model) {
        model.addAttribute("siteName", siteName);
    }
}
