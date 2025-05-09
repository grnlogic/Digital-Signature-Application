package com.example.digitalsignature.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

@Service
public class QRCodeService {
    
    /**
     * Generate QR code sebagai string Base64
     */
    public String generateQRCodeBase64(String content, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
    
    /**
     * Membuat konten QR Code yang berisi informasi tanda tangan
     */
    public String createSignatureQRContent(String hash, String signature, String designerName) {
        Map<String, String> qrData = new HashMap<>();
        qrData.put("hash", hash);
        qrData.put("signature", signature);
        qrData.put("designer", designerName);
        qrData.put("date", java.time.LocalDate.now().toString());
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : qrData.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        
        return sb.toString();
    }
}
