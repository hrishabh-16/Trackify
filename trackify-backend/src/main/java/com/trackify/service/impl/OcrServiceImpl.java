package com.trackify.service.impl;

import com.trackify.service.OcrService;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrServiceImpl implements OcrService {
    
    private static final Logger logger = LoggerFactory.getLogger(OcrServiceImpl.class);
    
    @Autowired
    private Tesseract tesseract;
    
    // Store configuration values to avoid calling non-existent getter methods
    private String configuredLanguage = "eng";
    private int configuredOcrEngineMode = 1;
    private int configuredPageSegMode = 6;
    private String configuredDataPath = "src/main/resources/tessdata";
    
    // Patterns for transaction data extraction
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?:rs\\.?|inr|₹|amount:?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})"
    );
    
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*(?:am|pm))?)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern UPI_ID_PATTERN = Pattern.compile(
        "([\\w.-]+@[\\w-]+)"
    );
    
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile(
        "(?:txn|transaction|ref|reference|id)\\s*:?\\s*([a-z0-9]{6,})", 
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String extractTextFromImage(MultipartFile imageFile) {
        try {
            logger.info("Extracting text from image: {}", imageFile.getOriginalFilename());
            
            // Convert MultipartFile to BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
            
            if (image == null) {
                logger.warn("Could not read image file: {}", imageFile.getOriginalFilename());
                return null;
            }
            
            return extractTextFromImage(image);
            
        } catch (IOException e) {
            logger.error("Error reading image file", e);
            return null;
        }
    }
    
    @Override
    public String extractTextFromImage(BufferedImage image) {
        try {
            logger.debug("Starting OCR text extraction");
            
            // Preprocess image for better OCR results
            BufferedImage processedImage = preprocessImage(image);
            
            // Extract text using Tesseract
            String extractedText = tesseract.doOCR(processedImage);
            
            // Clean and validate extracted text
            String cleanedText = cleanExtractedText(extractedText);
            
            logger.debug("OCR extraction completed. Text length: {}", 
                        cleanedText != null ? cleanedText.length() : 0);
            
            return cleanedText;
            
        } catch (TesseractException e) {
            logger.error("Tesseract OCR error", e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error during OCR", e);
            return null;
        }
    }
    
    @Override
    public Map<String, Object> extractTextWithConfidence(MultipartFile imageFile) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Extracting text with confidence from: {}", imageFile.getOriginalFilename());
            
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
            
            if (image == null) {
                result.put("success", false);
                result.put("error", "Could not read image file");
                return result;
            }
            
            // Preprocess image
            BufferedImage processedImage = preprocessImage(image);
            
            // Extract text
            String extractedText = tesseract.doOCR(processedImage);
            String cleanedText = cleanExtractedText(extractedText);
            
            // Calculate confidence based on text quality
            double confidence = calculateTextConfidence(cleanedText);
            
            long endTime = System.currentTimeMillis();
            
            result.put("success", true);
            result.put("text", cleanedText);
            result.put("confidence", confidence);
            result.put("processingTimeMs", endTime - startTime);
            result.put("imageWidth", image.getWidth());
            result.put("imageHeight", image.getHeight());
            result.put("textLength", cleanedText != null ? cleanedText.length() : 0);
            
        } catch (Exception e) {
            logger.error("Error extracting text with confidence", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public BufferedImage preprocessImage(BufferedImage image) {
        try {
            logger.debug("Preprocessing image for OCR");
            
            // Convert to grayscale
            BufferedImage grayImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY
            );
            Graphics2D g2d = grayImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            
            // Scale image if too small (OCR works better on larger images)
            int minWidth = 800;
            int minHeight = 600;
            
            if (grayImage.getWidth() < minWidth || grayImage.getHeight() < minHeight) {
                double scaleX = (double) minWidth / grayImage.getWidth();
                double scaleY = (double) minHeight / grayImage.getHeight();
                double scale = Math.max(scaleX, scaleY);
                
                int newWidth = (int) (grayImage.getWidth() * scale);
                int newHeight = (int) (grayImage.getHeight() * scale);
                
                BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g2 = scaledImage.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(grayImage, 0, 0, newWidth, newHeight, null);
                g2.dispose();
                
                grayImage = scaledImage;
            }
            
            // Apply contrast enhancement
            BufferedImage contrastImage = enhanceContrast(grayImage);
            
            logger.debug("Image preprocessing completed. Size: {}x{}", 
                        contrastImage.getWidth(), contrastImage.getHeight());
            
            return contrastImage;
            
        } catch (Exception e) {
            logger.warn("Error preprocessing image, using original", e);
            return image;
        }
    }
    
    @Override
    public String extractTextFromRegion(BufferedImage image, int x, int y, int width, int height) {
        try {
            // Extract specific region
            BufferedImage regionImage = image.getSubimage(x, y, width, height);
            return extractTextFromImage(regionImage);
        } catch (Exception e) {
            logger.error("Error extracting text from region", e);
            return null;
        }
    }
    
    @Override
    public boolean isOcrAvailable() {
        try {
            // Test OCR with a small test image
            BufferedImage testImage = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = testImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 100, 50);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("TEST", 20, 30);
            g.dispose();
            
            String result = tesseract.doOCR(testImage);
            return result != null && result.trim().length() > 0;
            
        } catch (Exception e) {
            logger.error("OCR availability test failed", e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getOcrConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        try {
            config.put("provider", "Tesseract 4J");
            config.put("available", isOcrAvailable());
            // FIXED: Using stored configuration values instead of getter methods
            config.put("language", configuredLanguage);
            config.put("ocrEngineMode", configuredOcrEngineMode);
            config.put("pageSegMode", configuredPageSegMode);
            config.put("dataPath", configuredDataPath);
            config.put("supportedFormats", new String[]{"PNG", "JPG", "JPEG", "TIFF", "BMP", "GIF"});
            
        } catch (Exception e) {
            config.put("available", false);
            config.put("error", e.getMessage());
        }
        
        return config;
    }
    
    @Override
    public Map<String, Object> extractTransactionData(MultipartFile imageFile) {
        Map<String, Object> transactionData = new HashMap<>();
        
        try {
            logger.info("Extracting structured transaction data from: {}", imageFile.getOriginalFilename());
            
            String extractedText = extractTextFromImage(imageFile);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                transactionData.put("success", false);
                transactionData.put("error", "No text extracted from image");
                return transactionData;
            }
            
            // Extract structured data
            transactionData.put("success", true);
            transactionData.put("rawText", extractedText);
            transactionData.put("amount", extractAmount(extractedText));
            transactionData.put("date", extractDate(extractedText));
            transactionData.put("time", extractTime(extractedText));
            transactionData.put("upiId", extractUpiId(extractedText));
            transactionData.put("transactionId", extractTransactionId(extractedText));
            transactionData.put("merchantName", extractMerchantName(extractedText));
            
        } catch (Exception e) {
            logger.error("Error extracting transaction data", e);
            transactionData.put("success", false);
            transactionData.put("error", e.getMessage());
        }
        
        return transactionData;
    }
    
    @Override
    public String extractReceiptText(MultipartFile receiptImage) {
        try {
            logger.info("Extracting receipt text from: {}", receiptImage.getOriginalFilename());
            
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(receiptImage.getBytes()));
            
            if (image == null) {
                return null;
            }
            
            // Enhanced preprocessing for receipts
            BufferedImage processedImage = preprocessReceiptImage(image);
            
            // Use different OCR settings for receipts
            Tesseract receiptTesseract = new Tesseract();
            receiptTesseract.setDatapath(configuredDataPath);
            receiptTesseract.setLanguage(configuredLanguage);
            receiptTesseract.setPageSegMode(6); // Single uniform block
            receiptTesseract.setOcrEngineMode(1); // LSTM only
            
            String extractedText = receiptTesseract.doOCR(processedImage);
            return cleanExtractedText(extractedText);
            
        } catch (Exception e) {
            logger.error("Error extracting receipt text", e);
            return null;
        }
    }
    
    @Override
    public String cleanExtractedText(String rawText) {
        if (rawText == null) {
            return null;
        }
        
        // Remove extra whitespace and normalize
        String cleaned = rawText.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\u0000-\u001f]", "") // Remove control characters
                .replaceAll("[^\\p{Print}\\p{Space}]", ""); // Keep only printable characters
        
        // Remove very short lines that are likely OCR noise
        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.length() >= 2) { // Keep lines with at least 2 characters
                result.append(trimmedLine).append("\n");
            }
        }
        
        return result.toString().trim();
    }
    
    // Private helper methods
    
    private BufferedImage enhanceContrast(BufferedImage image) {
        try {
            // Simple contrast enhancement
            BufferedImage enhanced = new BufferedImage(
                image.getWidth(), image.getHeight(), image.getType()
            );
            
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    int gray = (pixel >> 16) & 0xFF; // Get red component (same as gray in grayscale)
                    
                    // Enhance contrast
                    gray = Math.max(0, Math.min(255, (int) ((gray - 128) * 1.5 + 128)));
                    
                    int newPixel = (gray << 16) | (gray << 8) | gray;
                    enhanced.setRGB(x, y, newPixel);
                }
            }
            
            return enhanced;
            
        } catch (Exception e) {
            logger.warn("Error enhancing contrast", e);
            return image;
        }
    }
    
    private BufferedImage preprocessReceiptImage(BufferedImage image) {
        // Additional preprocessing specifically for receipts
        BufferedImage processed = preprocessImage(image);
        
        // Apply additional filtering for receipts
        // This could include noise reduction, line detection, etc.
        
        return processed;
    }
    
    private double calculateTextConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        double confidence = 50.0; // Base confidence
        
        // Check for common transaction patterns
        if (AMOUNT_PATTERN.matcher(text).find()) {
            confidence += 20.0;
        }
        
        if (DATE_PATTERN.matcher(text).find()) {
            confidence += 15.0;
        }
        
        if (UPI_ID_PATTERN.matcher(text).find()) {
            confidence += 10.0;
        }
        
        if (TRANSACTION_ID_PATTERN.matcher(text).find()) {
            confidence += 5.0;
        }
        
        // Penalize very short text
        if (text.length() < 20) {
            confidence -= 20.0;
        }
        
        return Math.max(0.0, Math.min(100.0, confidence));
    }
    
    private String extractAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll(",", "");
        }
        return null;
    }
    
    private String extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String extractTime(String text) {
        Matcher matcher = TIME_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String extractUpiId(String text) {
        Matcher matcher = UPI_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String extractTransactionId(String text) {
        Matcher matcher = TRANSACTION_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String extractMerchantName(String text) {
        // Look for common merchant patterns
        Pattern merchantPattern = Pattern.compile(
            "(?:to|paid to|received from|merchant)\\s*:?\\s*([\\w\\s&.-]{3,30})", 
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = merchantPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }
}