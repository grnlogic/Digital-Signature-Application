package com.example.digitalsignature.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.digitalsignature.service.CryptoService;
import com.example.digitalsignature.service.QRCodeService;

@RestController
@RequestMapping("/api/signature")
// Remove the @CrossOrigin annotation as we're handling CORS globally in WebConfig
public class SignatureController {

    @Autowired
    private CryptoService cryptoService;
    
    @Autowired
    private QRCodeService qrCodeService;

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of("status", "running", 
                      "message", "Digital Signature API is active");
    }

    @PostMapping("/sign")
    public Map<String, String> signFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "designerName", required = false) String designerName
    ) throws Exception {
        byte[] fileBytes = file.getBytes();

        // Hash dokumen dengan BLAKE3
        String hash = cryptoService.hashWithBlake3(fileBytes);

        // Tanda tangan hash dengan ECDSA
        String signature = cryptoService.signData(hash.getBytes());

        // Generate QR Code jika designerName diberikan
        String qrCodeBase64 = null;
        if (designerName != null && !designerName.isEmpty()) {
            String qrContent = qrCodeService.createSignatureQRContent(hash, signature, designerName);
            qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrContent, 250, 250);
        }

        Map<String, String> result = new HashMap<>();
        result.put("hash", hash);
        result.put("signature", signature);
        if (qrCodeBase64 != null) {
            result.put("qrCode", qrCodeBase64);
        }

        return result;
    }

    @PostMapping("/verify")
    public Map<String, Boolean> verifyFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("signature") String signature
    ) throws Exception {
        byte[] fileBytes = file.getBytes();
        String hash = cryptoService.hashWithBlake3(fileBytes);

        boolean valid = cryptoService.verifySignature(hash.getBytes(), signature);
        return Map.of("valid", valid);
    }
    
    @PostMapping("/signCollective")
    public Map<String, String> signCollectiveFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("role") String role,
            @RequestParam(value = "designerSignature", required = false) String designerSignature
    ) throws Exception {
        byte[] fileBytes = file.getBytes();
        String hash = cryptoService.hashWithBlake3(fileBytes);
        
        Map<String, String> result = new HashMap<>();
        result.put("hash", hash);
        
        if ("designer".equals(role)) {
            // Tanda tangan sebagai desainer
            String signature = cryptoService.signData(hash.getBytes());
            result.put("signature", signature);
        } else if ("brand".equals(role) && designerSignature != null) {
            // Tanda tangan sebagai brand dan gabungkan dengan tanda tangan desainer
            String brandSignature = cryptoService.signData(hash.getBytes());
            // Format: HASH || Signature_Desainer || Signature_Brand
            String collectiveSignature = hash + "||" + designerSignature + "||" + brandSignature;
            result.put("signature", brandSignature);
            result.put("collectiveSignature", collectiveSignature);
        }
        
        return result;
    }
    
    @PostMapping("/verifyCollective")
    public Map<String, Object> verifyCollectiveFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("signature") String collectiveSignature
    ) throws Exception {
        byte[] fileBytes = file.getBytes();
        String calculatedHash = cryptoService.hashWithBlake3(fileBytes);
        
        Map<String, Object> result = new HashMap<>();
        
        // Parse tanda tangan kolektif (format: HASH || Signature_Desainer || Signature_Brand)
        String[] parts = collectiveSignature.split("\\|\\|");
        if (parts.length != 3) {
            result.put("valid", false);
            result.put("message", "Format tanda tangan kolektif tidak valid");
            return result;
        }
        
        String storedHash = parts[0];
        String designerSignature = parts[1];
        String brandSignature = parts[2];
        
        // Verifikasi hash
        boolean hashValid = calculatedHash.equals(storedHash);
        
        // Verifikasi tanda tangan desainer dan brand
        boolean designerValid = cryptoService.verifySignature(storedHash.getBytes(), designerSignature);
        boolean brandValid = cryptoService.verifySignature(storedHash.getBytes(), brandSignature);
        
        boolean allValid = hashValid && designerValid && brandValid;
        
        result.put("valid", allValid);
        result.put("hashValid", hashValid);
        result.put("designerValid", designerValid);
        result.put("brandValid", brandValid);
        
        return result;
    }

    /**
     * Endpoint untuk generate QR Code dari data tanda tangan yang sudah ada
     */
    @PostMapping("/generateQR")
    public Map<String, String> generateQRCode(
            @RequestParam("hash") String hash,
            @RequestParam("signature") String signature,
            @RequestParam("designerName") String designerName
    ) throws Exception {
        String qrContent = qrCodeService.createSignatureQRContent(hash, signature, designerName);
        String qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrContent, 250, 250);
        
        Map<String, String> result = new HashMap<>();
        result.put("qrCode", qrCodeBase64);
        return result;
    }
}
