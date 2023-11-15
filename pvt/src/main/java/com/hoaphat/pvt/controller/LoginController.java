package com.hoaphat.pvt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
public class LoginController {
    @GetMapping("/deny")
    public ModelAndView deny(Model model, HttpServletRequest request) {
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return new ModelAndView("deny");
    }

    @RequestMapping(value = {"/", "/login", "/logout"})
    public String showLogin() {
        return "login";
    }
}
