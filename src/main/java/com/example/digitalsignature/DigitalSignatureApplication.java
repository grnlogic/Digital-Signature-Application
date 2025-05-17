package com.example.digitalsignature;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DigitalSignatureApplication {

    public static void main(String[] args) {
        // Tambahkan logging untuk memeriksa environment variables
        System.out.println("==== CHECKING ENVIRONMENT VARIABLES ====");
        System.out.println("ENV SIGNATURE_PRIVATE_KEY: " + 
            (System.getenv("SIGNATURE_PRIVATE_KEY") != null ? 
             System.getenv("SIGNATURE_PRIVATE_KEY").substring(0, 10) + "..." : "NULL"));
        System.out.println("ENV SIGNATURE_PUBLIC_KEY: " + 
            (System.getenv("SIGNATURE_PUBLIC_KEY") != null ? 
             System.getenv("SIGNATURE_PUBLIC_KEY").substring(0, 10) + "..." : "NULL"));
        System.out.println("=======================================");
        
        SpringApplication.run(DigitalSignatureApplication.class, args);
    }
}
