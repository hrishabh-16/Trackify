package com.trackify.service;

import org.springframework.web.multipart.MultipartFile;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Interface for OCR (Optical Character Recognition) operations
 */
public interface OcrService {
    
    /**
     * Extract text from image using OCR
     */
    String extractTextFromImage(MultipartFile imageFile);
    
    /**
     * Extract text from BufferedImage
     */
    String extractTextFromImage(BufferedImage image);
    
    /**
     * Extract text with confidence scores and metadata
     */
    Map<String, Object> extractTextWithConfidence(MultipartFile imageFile);
    
    /**
     * Preprocess image for better OCR results
     */
    BufferedImage preprocessImage(BufferedImage image);
    
    /**
     * Extract text from specific region of image
     */
    String extractTextFromRegion(BufferedImage image, int x, int y, int width, int height);
    
    /**
     * Check if OCR service is available and properly configured
     */
    boolean isOcrAvailable();
    
    /**
     * Get OCR service configuration and status
     */
    Map<String, Object> getOcrConfiguration();
    
    /**
     * Extract structured data from transaction images
     */
    Map<String, Object> extractTransactionData(MultipartFile imageFile);
    
    /**
     * Extract text from receipt image with enhanced processing
     */
    String extractReceiptText(MultipartFile receiptImage);
    
    /**
     * Validate and clean extracted text
     */
    String cleanExtractedText(String rawText);
}