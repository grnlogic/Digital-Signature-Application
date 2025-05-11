package com.example.digitalsignature.service;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VisibleWatermarkService {

    public byte[] addVisibleWatermark(MultipartFile imageFile, String watermarkText, 
                                     float opacity, int fontSize) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
        
        // Create a new buffered image with transparency support
        BufferedImage watermarkedImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        
        // Get graphics context and copy original image
        Graphics2D g2d = watermarkedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        
        // Set font properties for the watermark
        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
        g2d.setColor(Color.WHITE);
        
        // Calculate position (centered)
        int textWidth = g2d.getFontMetrics().stringWidth(watermarkText);
        int x = (originalImage.getWidth() - textWidth) / 2;
        int y = originalImage.getHeight() / 2;
        
        // Apply transparency
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        
        // Draw the watermark text
        g2d.drawString(watermarkText, x, y);
        g2d.dispose();
        
        // Convert back to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String formatName = getImageFormat(imageFile.getOriginalFilename());
        ImageIO.write(watermarkedImage, formatName, outputStream);
        
        return outputStream.toByteArray();
    }
    
    private String getImageFormat(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return extension.equals("jpg") ? "jpeg" : extension;
    }
}