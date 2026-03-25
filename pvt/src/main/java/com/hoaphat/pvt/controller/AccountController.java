package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.service.account.IAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/home/manager")
public class AccountController {
    @Autowired
    private IAccountService accountService;

    // Hiển thị form
    @GetMapping("/create-account")
    public String showForm() {
        return "create-account";
    }

    // Xử lý submit
    @PostMapping("/create-account")
    public String createAccount(@RequestParam String username, @RequestParam String password, @RequestParam String name, @RequestParam String role, Model model) {

        try {
            accountService.createAccount(username, password, name, role);
            model.addAttribute("message", "Tạo tài khoản thành công!");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "create-account";
    }
}
