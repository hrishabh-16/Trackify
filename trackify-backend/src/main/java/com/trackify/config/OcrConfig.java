package com.trackify.config;

import net.sourceforge.tess4j.Tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class OcrConfig {

    private static final Logger logger = LoggerFactory.getLogger(OcrConfig.class);

    @Value("${app.ocr.tesseract.data-path:src/main/resources/tessdata}")
    private String tesseractDataPath;

    @Value("${app.ocr.tesseract.language:eng}")
    private String tesseractLanguage;

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        
        try {
            // Set the tessdata path
            File tessDataDir = new File(tesseractDataPath);
            if (tessDataDir.exists()) {
                tesseract.setDatapath(tesseractDataPath);
                logger.info("Tesseract data path set to: {}", tesseractDataPath);
            } else {
                logger.warn("Tesseract data path not found: {}. Using system default.", tesseractDataPath);
            }
            
            // Set language
            tesseract.setLanguage(tesseractLanguage);
            
            // OCR Engine Mode - Use LSTM OCR engine only
            tesseract.setOcrEngineMode(1);
            
            // Page Segmentation Mode - Assume a single uniform block of text
            tesseract.setPageSegMode(6);
            
            logger.info("Tesseract OCR configured successfully with language: {}", tesseractLanguage);
            
        } catch (Exception e) {
            logger.error("Error configuring Tesseract OCR", e);
        }
        
        return tesseract;
    }
}