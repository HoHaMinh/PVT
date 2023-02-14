package com.hoaphat.pvt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {
    @GetMapping("/employee/home")
    public ModelAndView showHome() {
        return new ModelAndView("home");
    }
}
