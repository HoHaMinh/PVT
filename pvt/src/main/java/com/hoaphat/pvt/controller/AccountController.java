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
    public String createAccount(@RequestParam(required = false) String username,
                                @RequestParam(required = false) String password,
                                @RequestParam(required = false) String name,
                                @RequestParam(required = false) String role,
                                Model model) {

        try {
            accountService.createAccount(username, password, name, role);
            model.addAttribute("message", "Tạo tài khoản thành công!");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tạo tài khoản. Vui lòng kiểm tra dữ liệu và thử lại.");
        }

        return "create-account";
    }
}
