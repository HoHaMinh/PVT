package com.hoaphat.pvt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ImageController {
    @GetMapping("/home/employee/image")
    public String showImage() {
        return ("images");
    }

    @GetMapping("/home/employee/video")
    public String showVideo() {
        return ("video");
    }
}
