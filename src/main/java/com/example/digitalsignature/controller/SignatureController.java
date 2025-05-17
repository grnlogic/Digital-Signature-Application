package com.example.digitalsignature.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
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
import com.example.digitalsignature.service.SteganographyService;
import com.example.digitalsignature.service.VisibleWatermarkService;

@RestController
@RequestMapping("/api/signature")
// Remove the @CrossOrigin annotation as we're handling CORS globally in WebConfig
public class SignatureController {

    @Autowired
    private CryptoService cryptoService;
    
    @Autowired
    private QRCodeService qrCodeService;
    
    @Autowired
    private SteganographyService steganographyService;

    @Autowired
    private VisibleWatermarkService visibleWatermarkService;

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of("status", "running", 
                      "message", "Digital Signature API is active");
    }

    @PostMapping("/sign")
    public Map<String, String> signFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "designerName", required = false) String designerName,
            @RequestParam(value = "validityDays", required = false) Integer validityDays,
            @RequestParam(value = "validityMonths", required = false) Integer validityMonths
    ) throws Exception {
        // Hitung masa berlaku
        long validityMillis;
        
        if (validityMonths != null && validityMonths > 0) {
            // Konversi bulan ke milidetik (30 hari per bulan sebagai pendekatan)
            validityMillis = validityMonths * 30L * 24 * 60 * 60 * 1000;
        } else if (validityDays != null && validityDays > 0) {
            // Konversi hari ke milidetik
            validityMillis = validityDays * 24L * 60 * 60 * 1000;
        } else {
            // Default: 7 hari
            validityMillis = 7 * 24L * 60 * 60 * 1000;
        }
        
        byte[] fileBytes = file.getBytes();
        String hash = cryptoService.hashWithBlake3(fileBytes);
        
        // Tanda tangan dengan masa berlaku yang dihitung
        String signature = cryptoService.signData(hash.getBytes(), validityMillis);
        
        // Hitung tanggal kedaluwarsa untuk respons
        Date expiryDate = new Date(System.currentTimeMillis() + validityMillis);
        
        Map<String, String> result = new HashMap<>();
        result.put("hash", hash);
        result.put("signature", signature);
        result.put("validUntil", expiryDate.toString());
        
        // Generate QR code jika designerName disediakan
        if (designerName != null && !designerName.isEmpty()) {
            String qrContent = qrCodeService.createSignatureQRContent(hash, signature, designerName);
            String qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrContent, 250, 250);
            result.put("qrCode", qrCodeBase64);
        }
        
        return result;
    }
    
   @PostMapping("/signWithWatermark")
public Map<String, String> signFileWithWatermark(
        @RequestParam("file") MultipartFile file,
        @RequestParam("ownerInfo") String ownerInfo,
        @RequestParam(value = "designerName", required = false) String designerName
) throws Exception {
    // Apply watermark if it's an image
    byte[] processedData = steganographyService.embedWatermark(file, ownerInfo);

     // Terapkan kompresi jika ukuran lebih dari 500KB
    if (processedData.length > 500_000) {
        processedData = steganographyService.compressBeforeBase64(processedData, 0.7f);
        System.out.println("Image compressed: " + processedData.length + " bytes");
    }

    // Hash the watermarked data with BLAKE3
    String hash = cryptoService.hashWithBlake3(processedData);

    // Sign hash with ECDSA
    String signature = cryptoService.signData(hash.getBytes());

    // Generate QR Code if designerName is provided
    String qrCodeBase64 = null;
    if (designerName != null && !designerName.isEmpty()) {
        String qrContent = qrCodeService.createSignatureQRContent(hash, signature, designerName);
        qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrContent, 250, 250);
    }

    // Convert processed image to base64
    String imageBase64 = Base64.getEncoder().encodeToString(processedData);

    Map<String, String> result = new HashMap<>();
    result.put("hash", hash);
    result.put("signature", signature);
    result.put("watermarked", "true");
    result.put("watermarkedImage", "data:image/jpeg;base64," + imageBase64);
    if (qrCodeBase64 != null) {
        result.put("qrCode", qrCodeBase64);
    }

    return result;
}

    @PostMapping("/signWithVisibleWatermark")
    public Map<String, String> signFileWithVisibleWatermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam("watermarkText") String watermarkText,
            @RequestParam(value = "opacity", defaultValue = "0.5") float opacity,
            @RequestParam(value = "fontSize", defaultValue = "36") int fontSize,
            @RequestParam(value = "designerName", required = false) String designerName,
            @RequestParam(value = "validityDays", required = false) Integer validityDays,
            @RequestParam(value = "validityMonths", required = false) Integer validityMonths
    ) throws Exception {
        // Hitung masa berlaku
        long validityMillis;
        
        if (validityMonths != null && validityMonths > 0) {
            // Konversi bulan ke milidetik (menggunakan Calendar untuk presisi lebih baik)
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, validityMonths);
            validityMillis = cal.getTimeInMillis() - System.currentTimeMillis();
        } else if (validityDays != null && validityDays > 0) {
            validityMillis = validityDays * 24L * 60 * 60 * 1000;
        } else {
            validityMillis = 7 * 24L * 60 * 60 * 1000; // Default 7 hari
        }
        
        // Proses watermark seperti biasa
        byte[] processedData = visibleWatermarkService.addVisibleWatermark(file, watermarkText, opacity, fontSize);
        
        // Check if processedData is valid
        if (processedData == null || processedData.length == 0) {
            throw new RuntimeException("Failed to create watermarked image - no data returned");
        }
        
        System.out.println("Processed data length: " + processedData.length);
        
        // Convert processed image to base64
        String imageBase64 = Base64.getEncoder().encodeToString(processedData);
        
        
        // Get file extension
        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        String mimeType = "image/" + (extension.equalsIgnoreCase("jpg") ? "jpeg" : extension);
        
        // Create full base64 image URI
        String imageDataUri = "data:" + mimeType + ";base64," + imageBase64;

        // Hash the watermarked data with BLAKE3
        String hash = cryptoService.hashWithBlake3(processedData);

        // Sign hash with ECDSA, passing validity period
        String signature = cryptoService.signData(hash.getBytes(), validityMillis);

        // Generate QR Code if designerName is provided
        String qrCodeBase64 = null;
        if (designerName != null && !designerName.isEmpty()) {
            String qrContent = qrCodeService.createSignatureQRContent(hash, signature, designerName);
            qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrContent, 250, 250);
        }

        // Hasil seperti biasa
        Map<String, String> result = new HashMap<>();
        result.put("hash", hash);
        result.put("signature", signature);
        result.put("validUntil", new Date(System.currentTimeMillis() + validityMillis).toString());
        result.put("visibleWatermark", "true");
        result.put("watermarkedImage", "data:image/jpeg;base64," + imageBase64);
        if (qrCodeBase64 != null) {
            result.put("qrCode", qrCodeBase64);
        }

        return result;
    }

    @PostMapping("/verify")
public Map<String, Object> verifyFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("signature") String signature
) {
    Map<String, Object> response = new HashMap<>();
    try {
        byte[] fileBytes = file.getBytes();
        String hash = cryptoService.hashWithBlake3(fileBytes);
        
        boolean valid = cryptoService.verifySignature(hash.getBytes(), signature);
        response.put("valid", valid);
        
        if (!valid) {
            // Cek apakah tanda tangan sudah kedaluwarsa
            if (signature.contains(":")) {
                long expiryTime = Long.parseLong(signature.split(":")[1]);
                long currentTime = System.currentTimeMillis();
                
                if (currentTime > expiryTime) {
                    response.put("pesan", "Tanda tangan digital sudah kedaluwarsa");
                    response.put("expiredAt", new Date(expiryTime).toString());
                } else {
                    response.put("pesan", "Tanda tangan digital tidak valid untuk file ini");
                }
            } else {
                response.put("pesan", "Tanda tangan digital tidak valid untuk file ini");
            }
        } else {
            // Tambahkan informasi masa berlaku untuk tanda tangan yang valid
            if (signature.contains(":")) {
                long expiryTime = Long.parseLong(signature.split(":")[1]);
                response.put("validUntil", new Date(expiryTime).toString());
            }
        }
    } catch (Exception e) {
        response.put("valid", false);
        response.put("pesan", "Terjadi kesalahan saat verifikasi: " + e.getMessage());
    }
    return response;
}
    
    @PostMapping("/verifyWithWatermark")
    public Map<String, Object> verifyFileWithWatermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam("signature") String signature
    ) throws Exception {
        byte[] fileBytes = file.getBytes();
        
        // Try to extract watermark (if it's an image)
        String watermark = null;
        try {
            watermark = steganographyService.extractWatermark(fileBytes);
        } catch (IOException e) {
            // Not an image or couldn't extract watermark
        }
        
        String hash = cryptoService.hashWithBlake3(fileBytes);
        boolean valid = cryptoService.verifySignature(hash.getBytes(), signature);
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        
        if (watermark != null) {
            result.put("watermarkFound", true);
            result.put("watermarkData", watermark);
        } else {
            result.put("watermarkFound", false);
        }
        
        return result;
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
    
    @PostMapping("/signCollectiveWithWatermark")
    public Map<String, String> signCollectiveFileWithWatermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam("role") String role,
            @RequestParam("ownerInfo") String ownerInfo,
            @RequestParam(value = "designerSignature", required = false) String designerSignature
    ) throws Exception {
        // Apply watermark if it's an image
        byte[] processedData = steganographyService.embedWatermark(file, ownerInfo);
        
        // Tambahkan logging untuk debug
        System.out.println("Processed data size after steganography: " + 
                          (processedData != null ? processedData.length : "NULL") + " bytes");
        
        String hash = cryptoService.hashWithBlake3(processedData);
        
        Map<String, String> result = new HashMap<>();
        result.put("hash", hash);
        result.put("watermarked", "true");
        // Convert processed image to base64
        String imageBase64 = Base64.getEncoder().encodeToString(processedData);
        System.out.println("Base64 string length: " + imageBase64.length());
        
        // Jika base64 kosong, gunakan image original sebagai fallback
        if (imageBase64 == null || imageBase64.isEmpty()) {
            System.out.println("WARNING: Base64 encoding resulted in empty string, using original image");
            imageBase64 = Base64.getEncoder().encodeToString(file.getBytes());
        }
        
        result.put("watermarkedImage", "data:image/jpeg;base64," + imageBase64);
        
        if ("designer".equals(role)) {
            // Sign as designer
            String signature = cryptoService.signData(hash.getBytes());
            result.put("signature", signature);
        } else if ("brand".equals(role) && designerSignature != null) {
            // Sign as brand and combine with designer signature
            String brandSignature = cryptoService.signData(hash.getBytes());
            // Format: HASH || Signature_Designer || Signature_Brand
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
    
    /**
     * Endpoint to extract watermark from an image
     */
    @PostMapping("/extractWatermark")
    public Map<String, Object> extractWatermark(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        byte[] fileBytes = file.getBytes();
        Map<String, Object> result = new HashMap<>();
        
        try {
            String watermark = steganographyService.extractWatermark(fileBytes);
            if (watermark != null) {
                result.put("success", true);
                result.put("watermarkData", watermark);
                
                // Parse the watermark data for easier client processing
                String[] parts = watermark.split(";");
                Map<String, String> parsedData = new HashMap<>();
                
                for (String part : parts) {
                    String[] keyValue = part.split(":");
                    if (keyValue.length == 2) {
                        parsedData.put(keyValue[0], keyValue[1]);
                    }
                }
                
                result.put("parsedWatermark", parsedData);
            } else {
                result.put("success", false);
                result.put("message", "No watermark found in the image");
            }
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "Failed to process image: " + e.getMessage());
        }
        
        return result;
    }
}
