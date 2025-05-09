package com.example.digitalsignature.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SteganographyService {

    /**
     * Embeds an invisible watermark into an image file
     * @param imageFile The original image file
     * @param ownerInfo The owner information to embed
     * @return The watermarked image as byte array
     */
    public byte[] embedWatermark(MultipartFile imageFile, String ownerInfo) throws IOException {
        // Check if file is an image
        if (!isImage(imageFile)) {
            return imageFile.getBytes(); // Return original if not an image
        }
        
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
        if (originalImage == null) {
            throw new IOException("Could not read image file");
        }
        
        // Generate watermark data
        String watermarkData = createWatermarkData(ownerInfo);
        
        // Embed watermark into image using LSB steganography
        BufferedImage watermarkedImage = embedLSBWatermark(originalImage, watermarkData);
        
        // Convert back to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(watermarkedImage, getImageFormat(imageFile.getOriginalFilename()), outputStream);
        
        return outputStream.toByteArray();
    }
    
    /**
     * Extracts the watermark from an image
     * @param imageData The image data as byte array
     * @return The extracted watermark data or null if no watermark found
     */
    public String extractWatermark(byte[] imageData) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            throw new IOException("Could not read image data");
        }
        
        return extractLSBWatermark(image);
    }
    
    /**
     * Creates watermark data string with timestamp and ID
     */
    private String createWatermarkData(String ownerInfo) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String id = UUID.randomUUID().toString().substring(0, 8); // Short UUID
        
        return String.format("OWNER:%s;DATE:%s;ID:%s", ownerInfo, timestamp, id);
    }
    
    /**
     * Embeds watermark using LSB (Least Significant Bit) steganography
     */
    private BufferedImage embedLSBWatermark(BufferedImage image, String watermarkData) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Convert watermark to binary string
        String binaryWatermark = convertToBinary(watermarkData);
        int dataLength = binaryWatermark.length();
        
        // First, embed the length of data as a 32-bit integer
        String binaryLength = String.format("%32s", Integer.toBinaryString(dataLength)).replace(' ', '0');
        
        // Create a copy of the image
        BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Embed length first (32 pixels)
        int pixelIndex = 0;
        for (int i = 0; i < binaryLength.length(); i++) {
            int x = pixelIndex % width;
            int y = pixelIndex / width;
            
            if (y >= height) break; // Safety check
            
            int rgb = image.getRGB(x, y);
            int newRgb = (rgb & 0xFFFFFFFE) | (binaryLength.charAt(i) == '1' ? 1 : 0);
            watermarkedImage.setRGB(x, y, newRgb);
            pixelIndex++;
        }
        
        // Embed actual data
        for (int i = 0; i < binaryWatermark.length(); i++) {
            int x = pixelIndex % width;
            int y = pixelIndex / width;
            
            if (y >= height) break; // Safety check
            
            int rgb = image.getRGB(x, y);
            int newRgb = (rgb & 0xFFFFFFFE) | (binaryWatermark.charAt(i) == '1' ? 1 : 0);
            watermarkedImage.setRGB(x, y, newRgb);
            pixelIndex++;
        }
        
        // Copy remaining pixels unchanged
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y * width + x >= pixelIndex) {
                    watermarkedImage.setRGB(x, y, image.getRGB(x, y));
                }
            }
        }
        
        return watermarkedImage;
    }
    
    /**
     * Extracts LSB watermark from image
     */
    private String extractLSBWatermark(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Extract length first (32 bits)
        StringBuilder lengthBinary = new StringBuilder();
        int pixelIndex = 0;
        
        for (int i = 0; i < 32; i++) {
            int x = pixelIndex % width;
            int y = pixelIndex / width;
            
            if (y >= height) return null; // Safety check
            
            int rgb = image.getRGB(x, y);
            lengthBinary.append((rgb & 1) == 1 ? '1' : '0');
            pixelIndex++;
        }
        
        // Convert binary length to integer
        int dataLength;
        try {
            dataLength = Integer.parseInt(lengthBinary.toString(), 2);
            if (dataLength <= 0 || dataLength > 10000) {
                return null; // Invalid length, probably not a watermarked image
            }
        } catch (NumberFormatException e) {
            return null; // Not a valid binary number
        }
        
        // Extract the actual data
        StringBuilder dataBinary = new StringBuilder();
        for (int i = 0; i < dataLength; i++) {
            int x = pixelIndex % width;
            int y = pixelIndex / width;
            
            if (y >= height) break; // Safety check
            
            int rgb = image.getRGB(x, y);
            dataBinary.append((rgb & 1) == 1 ? '1' : '0');
            pixelIndex++;
        }
        
        // Convert binary data back to string
        return convertFromBinary(dataBinary.toString());
    }
    
    /**
     * Converts a string to binary representation
     */
    private String convertToBinary(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            String binary = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
            result.append(binary);
        }
        return result.toString();
    }
    
    /**
     * Converts binary back to a string
     */
    private String convertFromBinary(String binary) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < binary.length(); i += 8) {
            if (i + 8 <= binary.length()) {
                String byteStr = binary.substring(i, i + 8);
                int charCode = Integer.parseInt(byteStr, 2);
                result.append((char) charCode);
            }
        }
        return result.toString();
    }
    
    /**
     * Checks if a file is an image based on file extension
     */
    private boolean isImage(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) return false;
        
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return extension.equals("jpg") || extension.equals("jpeg") || 
               extension.equals("png") || extension.equals("gif") || 
               extension.equals("bmp");
    }
    
    /**
     * Gets the image format from filename
     */
    private String getImageFormat(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return extension.equals("jpg") ? "jpeg" : extension;
    }
}
