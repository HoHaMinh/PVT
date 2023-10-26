package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.service.ISpecialMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {
    @Autowired
    private ISpecialMessageService specialMessageService;

    @GetMapping("/home")
    public ModelAndView showHome(Model model) {
        model.addAttribute("message",specialMessageService.getAll());
        return new ModelAndView("home");
    }
}
