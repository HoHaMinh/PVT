package com.hoaphat.pvt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ImageController {
    @GetMapping("/home/employee/image")
    public String showImage(Model model, HttpServletRequest request) {
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return ("images");
    }

    @GetMapping("/home/employee/video")
    public String showVideo(Model model, HttpServletRequest request) {
        String username = (String) request.getSession().getAttribute("username");
        model.addAttribute("username", username);
        return ("video");
    }
}
