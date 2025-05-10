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
    
    @GetMapping("/verify")
    public String verify() {
        return "Anda mengakses endpoint '/verify' dengan metode GET. Untuk verifikasi tanda tangan, silakan gunakan:\n\n" +
               "1. Endpoint: /api/signature/verify\n" +
               "2. Metode: POST\n" +
               "3. Parameter yang diperlukan:\n" +
               "   - file: dokumen atau gambar yang akan diverifikasi\n" +
               "   - signature: tanda tangan digital\n\n" +
               "Untuk verifikasi dengan watermark, gunakan /api/signature/verifyWithWatermark";
    }
}
