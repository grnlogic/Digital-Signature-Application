package com.example.digitalsignature.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Backend Digital Signature aktif!";
    }
    
    @GetMapping("/health")
    public String health() {
        return "UP";
    }
}
