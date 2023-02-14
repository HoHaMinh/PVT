package com.hoaphat.pvt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LoginController {
    @GetMapping("/deny")
    public String deny() {
        return "deny";
    }

    @RequestMapping(value = {"/", "/login", "/logout"})
    public String showLogin() {
        return "login";
    }
}
