package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.service.ISpecialMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Controller
public class HomeController {
    @Autowired
    private ISpecialMessageService specialMessageService;

    @GetMapping("/home")
    public ModelAndView showHome(Model model, HttpServletRequest request) {
        model.addAttribute("message",specialMessageService.getAll(LocalDateTime.now()));
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return new ModelAndView("home");
    }
}
