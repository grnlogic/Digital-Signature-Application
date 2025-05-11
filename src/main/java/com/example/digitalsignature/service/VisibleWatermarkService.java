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
        System.out.println("Processing image: " + imageFile.getOriginalFilename());
        System.out.println("Content Type: " + imageFile.getContentType());
        
        try {
            // Convert MultipartFile to BufferedImage
            BufferedImage originalImage = ImageIO.read(imageFile.getInputStream());
            
            if (originalImage == null) {
                System.out.println("Failed to read image - ImageIO returned null");
                throw new IOException("Cannot read image file - format may be unsupported");
            }
            
            System.out.println("Original image dimensions: " + originalImage.getWidth() + "x" + originalImage.getHeight());
            
            // Create a compatible BufferedImage with RGB color model
            BufferedImage watermarkedImage = new BufferedImage(
                originalImage.getWidth(), 
                originalImage.getHeight(), 
                BufferedImage.TYPE_INT_RGB
            );
            
            // Get graphics context and draw original image
            Graphics2D g2d = watermarkedImage.createGraphics();
            g2d.drawImage(originalImage, 0, 0, null);
            
            // Set font properties for watermark
            g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
            g2d.setColor(Color.WHITE);
            
            // Apply transparency
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            
            // Calculate position (diagonal)
            int textWidth = g2d.getFontMetrics().stringWidth(watermarkText);
            int x = (originalImage.getWidth() - textWidth) / 2;
            int y = originalImage.getHeight() / 2;
            
            // Draw the watermark text
            g2d.drawString(watermarkText, x, y);
            
            // Add drop shadow for visibility on all backgrounds
            g2d.setColor(Color.BLACK);
            g2d.drawString(watermarkText, x+2, y+2);
            g2d.setColor(Color.WHITE);
            g2d.drawString(watermarkText, x, y);
            
            // Clean up
            g2d.dispose();
            
            // Convert back to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String formatName = getImageFormat(imageFile.getOriginalFilename());
            
            System.out.println("Using image format for output: " + formatName);
            
            boolean success = ImageIO.write(watermarkedImage, formatName, outputStream);
            
            if (!success) {
                System.out.println("Failed to write image - no appropriate writer found for format: " + formatName);
                throw new IOException("Failed to encode image in " + formatName + " format");
            }
            
            byte[] result = outputStream.toByteArray();
            System.out.println("Processed image size: " + result.length + " bytes");
            
            return result;
        } catch (Exception e) {
            System.out.println("Error in watermarking process: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to process image: " + e.getMessage(), e);
        }
    }
    
    private String getImageFormat(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg"; // default
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return extension.equals("jpg") ? "jpeg" : extension;
    }
}