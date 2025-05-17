package com.example.digitalsignature.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SteganographyService {

    /**
 * Menerapkan kompresi sebelum konversi ke base64
 * @param imageData Data gambar yang akan dikompresi
 * @param quality Kualitas kompresi (0.0-1.0)
 * @return Data gambar terkompresi
 */
public byte[] compressBeforeBase64(byte[] imageData, float quality) throws IOException {
    // Baca gambar dari byte array
    ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
    BufferedImage image = ImageIO.read(inputStream);
    
    // Siapkan output stream
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
    // Buat objek image writer
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) throw new IOException("No JPEG writer found");
    
    ImageWriter writer = writers.next();
    ImageWriteParam param = writer.getDefaultWriteParam();
    
    // Set kompresi
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(quality); // 0.0 (buruk) hingga 1.0 (terbaik)
    
    // Tulis gambar terkompresi
    ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
    writer.setOutput(imageOutputStream);
    writer.write(null, new IIOImage(image, null, null), param);
    writer.dispose();
    
    return outputStream.toByteArray();
}

    /**
     * Embeds an invisible watermark into an image file
     * @param imageFile The original image file
     * @param ownerInfo The owner information to embed
     * @return The watermarked image as byte array
     */
    public byte[] embedWatermark(MultipartFile file, String ownerInfo) throws IOException {
        // Tambahkan validasi dan logging
        System.out.println("Processing file: " + file.getOriginalFilename() + ", size: " + file.getSize() + " bytes");
        
        if (!isValidImageFormat(file)) {
            System.out.println("WARNING: Not a supported image format, returning original file");
            return file.getBytes();
        }
        
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        if (originalImage == null) {
            throw new IOException("Could not read image file");
        }
        
        // Generate watermark data
        String watermarkData = createWatermarkData(ownerInfo);
        
        // Embed watermark into image using LSB steganography
        BufferedImage watermarkedImage = embedLSBWatermark(originalImage, watermarkData);
        
        // Convert back to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(watermarkedImage, getImageFormat(file.getOriginalFilename()), outputStream);
        
        byte[] resultBytes = outputStream.toByteArray();
        
        // Validasi hasil sebelum return
        if (resultBytes == null || resultBytes.length == 0) {
            System.out.println("ERROR: Watermarking resulted in empty data");
            return file.getBytes(); // Return original as fallback
        }
        
        System.out.println("Successfully applied watermark, result size: " + resultBytes.length + " bytes");
        return resultBytes;
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
    
    /**
     * Validates if the image format is supported
     */
    private boolean isValidImageFormat(MultipartFile file) {
        String[] supportedFormats = new String[] {"jpg", "jpeg", "png", "gif", "bmp"};
        String filename = file.getOriginalFilename();
        if (filename == null) return false;
        
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        for (String format : supportedFormats) {
            if (format.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    // Metode baru di SteganographyService
    public byte[] compressImage(byte[] imageData, float quality) throws IOException {
        // Kode untuk kompresi gambar
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Kompresi dengan JPEG
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality); // 0.7 = 70% quality
        
        ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        ios.close();
        
        return outputStream.toByteArray();
    }
}
