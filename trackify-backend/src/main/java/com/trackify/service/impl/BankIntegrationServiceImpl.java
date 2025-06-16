package com.trackify.service.impl;

import com.trackify.entity.Expense;
import com.trackify.integration.bank.UpiTransactionProcessor;
import com.trackify.integration.sms.SmsParser;
import com.trackify.integration.sms.TransactionExtractor;
import com.trackify.integration.sms.TransactionExtractor.SmsMessage;
import com.trackify.service.BankIntegrationService;
import com.trackify.service.OcrService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class BankIntegrationServiceImpl implements BankIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(BankIntegrationServiceImpl.class);

    @Autowired
    private UpiTransactionProcessor upiTransactionProcessor;

    @Autowired
    private TransactionExtractor transactionExtractor;

    @Autowired
    private SmsParser smsParser;

    @Autowired
    private OcrService ocrService;

    // Supported file types
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/png", "image/jpg", "image/jpeg", "image/gif", "image/bmp", "image/tiff"
    );

    private static final List<String> SUPPORTED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf", "text/plain", "text/csv"
    );

    private static final List<String> SUPPORTED_ARCHIVE_TYPES = Arrays.asList(
            "application/zip", "application/x-zip-compressed"
    );

    // Processing statistics
    private final Map<Long, Map<String, Object>> userStatistics = new ConcurrentHashMap<>();
    
    // Processing history
    private final Map<Long, List<Map<String, Object>>> processingHistory = new ConcurrentHashMap<>();

    @Override
    public Expense processUploadedFile(MultipartFile file, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Processing uploaded file: {} ({}KB) for user: {}", 
                       file.getOriginalFilename(), file.getSize() / 1024, userId);

            if (!isSupportedFileType(file)) {
                throw new RuntimeException("Unsupported file type: " + file.getContentType());
            }

            String extractedText = null;
            Map<String, Object> extractionResult = null;

            if (SUPPORTED_IMAGE_TYPES.contains(file.getContentType())) {
                logger.debug("Processing image file with OCR");
                extractionResult = ocrService.extractTextWithConfidence(file);
                extractedText = (String) extractionResult.get("text");
                
            } else if (SUPPORTED_DOCUMENT_TYPES.contains(file.getContentType())) {
                logger.debug("Processing document file");
                extractedText = extractTextFromPdf(file);
                extractionResult = createDocumentExtractionResult(extractedText, file);
            }

            if (extractedText == null || extractedText.trim().isEmpty()) {
                logger.warn("No text extracted from file: {}", file.getOriginalFilename());
                recordProcessingHistory(userId, file, false, "No text extracted", null);
                updateProcessingStatistics(userId, false, file.getContentType());
                return null;
            }

            logger.debug("Extracted text length: {} characters", extractedText.length());

            // Try to parse as transaction
            Expense expense = parseExtractedText(extractedText, userId);

            long endTime = System.currentTimeMillis();
            
            if (expense != null) {
                logger.info("Successfully created expense {} from file: {}", 
                           expense.getId(), file.getOriginalFilename());
                
                recordProcessingHistory(userId, file, true, "Success", expense);
                updateProcessingStatistics(userId, true, file.getContentType());
            } else {
                logger.warn("Could not create expense from extracted text");
                recordProcessingHistory(userId, file, false, "Failed to parse transaction", null);
                updateProcessingStatistics(userId, false, file.getContentType());
            }

            // Log processing time
            logger.debug("File processing completed in {}ms", endTime - startTime);

            return expense;

        } catch (Exception e) {
            logger.error("Error processing uploaded file for user: {}", userId, e);
            recordProcessingHistory(userId, file, false, "Error: " + e.getMessage(), null);
            updateProcessingStatistics(userId, false, file.getContentType());
            throw new RuntimeException("Failed to process uploaded file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Expense> processUploadedFilesBatch(List<MultipartFile> files, Long userId) {
        logger.info("Processing {} files in batch for user: {}", files.size(), userId);
        
        List<Expense> createdExpenses = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (MultipartFile file : files) {
            try {
                Expense expense = processUploadedFile(file, userId);
                if (expense != null) {
                    createdExpenses.add(expense);
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                logger.warn("Failed to process file {}: {}", file.getOriginalFilename(), e.getMessage());
                failCount++;
            }
        }
        
        logger.info("Batch processing completed: {} successful, {} failed", successCount, failCount);
        
        return createdExpenses;
    }

    @Override
    public Expense extractFromImage(MultipartFile imageFile, Long userId) {
        try {
            logger.info("Extracting transaction data from image for user: {}", userId);
            
            if (!SUPPORTED_IMAGE_TYPES.contains(imageFile.getContentType())) {
                throw new RuntimeException("Unsupported image type: " + imageFile.getContentType());
            }

            // Use structured transaction data extraction
            Map<String, Object> transactionData = ocrService.extractTransactionData(imageFile);
            
            if (!(Boolean) transactionData.getOrDefault("success", false)) {
                logger.warn("Failed to extract transaction data from image: {}", 
                           transactionData.get("error"));
                return null;
            }

            // Build transaction text from structured data
            String transactionText = buildTransactionText(transactionData);
            
            return parseUpiTransaction(transactionText, userId);

        } catch (Exception e) {
            logger.error("Error extracting from image for user: {}", userId, e);
            throw new RuntimeException("Failed to extract from image", e);
        }
    }

    @Override
    public Expense extractFromPdf(MultipartFile pdfFile, Long userId) {
        try {
            logger.info("Extracting transaction data from PDF for user: {}", userId);
            
            if (!"application/pdf".equals(pdfFile.getContentType())) {
                throw new RuntimeException("File is not a PDF: " + pdfFile.getContentType());
            }

            String extractedText = extractTextFromPdf(pdfFile);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                // Try OCR on PDF if text extraction fails
                logger.debug("Text extraction failed, trying OCR on PDF");
                extractedText = extractTextFromPdfUsingOcr(pdfFile);
            }
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                logger.warn("No text extracted from PDF: {}", pdfFile.getOriginalFilename());
                return null;
            }

            return parseUpiTransaction(extractedText, userId);

        } catch (Exception e) {
            logger.error("Error extracting from PDF for user: {}", userId, e);
            throw new RuntimeException("Failed to extract from PDF", e);
        }
    }

    @Override
    public Expense processSmsTransaction(String smsContent, String sender, Long userId) {
        try {
            logger.info("Processing SMS transaction for user: {}", userId);
            return transactionExtractor.extractAndCreateExpense(smsContent, sender, userId);
        } catch (Exception e) {
            logger.error("Error processing SMS transaction for user: {}", userId, e);
            throw new RuntimeException("Failed to process SMS transaction", e);
        }
    }

    @Override
    public List<Expense> processSmsMessagesBatch(List<SmsMessage> smsMessages, Long userId) {
        try {
            logger.info("Processing {} SMS messages in batch for user: {}", smsMessages.size(), userId);
            return transactionExtractor.processSmsMessagesBatch(smsMessages, userId);
        } catch (Exception e) {
            logger.error("Error processing SMS messages batch for user: {}", userId, e);
            throw new RuntimeException("Failed to process SMS messages batch", e);
        }
    }

    @Override
    public List<Expense> processBankStatement(MultipartFile statementFile, Long userId) {
        try {
            logger.info("Processing bank statement for user: {}", userId);
            
            String statementContent;
            
            if ("application/pdf".equals(statementFile.getContentType())) {
                statementContent = extractTextFromPdf(statementFile);
                
                // If PDF text extraction fails, try OCR
                if (statementContent == null || statementContent.trim().isEmpty()) {
                    logger.debug("PDF text extraction failed, trying OCR");
                    statementContent = extractTextFromPdfUsingOcr(statementFile);
                }
                
            } else if ("text/plain".equals(statementFile.getContentType()) || 
                      "text/csv".equals(statementFile.getContentType())) {
                statementContent = new String(statementFile.getBytes());
            } else {
                throw new RuntimeException("Unsupported statement file type: " + statementFile.getContentType());
            }

            if (statementContent == null || statementContent.trim().isEmpty()) {
                throw new RuntimeException("No content extracted from bank statement");
            }

            return transactionExtractor.extractFromBankStatement(statementContent, userId);

        } catch (Exception e) {
            logger.error("Error processing bank statement for user: {}", userId, e);
            throw new RuntimeException("Failed to process bank statement", e);
        }
    }

    @Override
    public boolean isSupportedFileType(MultipartFile file) {
        String contentType = file.getContentType();
        return SUPPORTED_IMAGE_TYPES.contains(contentType) || 
               SUPPORTED_DOCUMENT_TYPES.contains(contentType) ||
               SUPPORTED_ARCHIVE_TYPES.contains(contentType);
    }

    @Override
    public List<String> getSupportedFileTypes() {
        List<String> allTypes = new ArrayList<>();
        allTypes.addAll(SUPPORTED_IMAGE_TYPES);
        allTypes.addAll(SUPPORTED_DOCUMENT_TYPES);
        allTypes.addAll(SUPPORTED_ARCHIVE_TYPES);
        return allTypes;
    }

    @Override
    public String extractTextFromImage(MultipartFile imageFile) {
        try {
            logger.debug("Extracting text from image: {}", imageFile.getOriginalFilename());
            return ocrService.extractTextFromImage(imageFile);
        } catch (Exception e) {
            logger.error("Error extracting text from image", e);
            return null;
        }
    }

    @Override
    public String extractTextFromPdf(MultipartFile pdfFile) {
        try {
            logger.debug("Extracting text from PDF: {}", pdfFile.getOriginalFilename());
            
            PDDocument document = PDDocument.load(pdfFile.getInputStream());
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            document.close();
            
            // Clean extracted text
            return ocrService.cleanExtractedText(text);
            
        } catch (Exception e) {
            logger.error("Error extracting text from PDF", e);
            return null;
        }
    }

    @Override
    public Expense parseUpiTransaction(String transactionText, Long userId) {
        try {
            return upiTransactionProcessor.processUpiTransaction(transactionText, userId);
        } catch (Exception e) {
            logger.error("Error parsing UPI transaction for user: {}", userId, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getExtractionStatistics(Long userId) {
        return userStatistics.getOrDefault(userId, new HashMap<>());
    }

    @Override
    public boolean isValidTransactionSms(String smsContent, String sender) {
        try {
            var transactionInfo = smsParser.parseSms(smsContent, sender, java.time.LocalDateTime.now());
            return transactionInfo.isTransaction();
        } catch (Exception e) {
            logger.warn("Error validating SMS transaction", e);
            return false;
        }
    }

    @Override
    public Map<String, String> getRecognizedBankSenders() {
        Map<String, String> bankSenders = new HashMap<>();
        bankSenders.put("SBIINB", "State Bank of India");
        bankSenders.put("HDFCBK", "HDFC Bank");
        bankSenders.put("ICICIB", "ICICI Bank");
        bankSenders.put("AXISBK", "Axis Bank");
        bankSenders.put("KOTAKB", "Kotak Mahindra Bank");
        bankSenders.put("PNBSMS", "Punjab National Bank");
        bankSenders.put("BOBSMS", "Bank of Baroda");
        bankSenders.put("CBSSMS", "Canara Bank");
        bankSenders.put("PAYTM", "Paytm");
        bankSenders.put("PHONEPE", "PhonePe");
        bankSenders.put("GPAY", "Google Pay");
        bankSenders.put("AMAZONP", "Amazon Pay");
        return bankSenders;
    }

    @Override
    public void configureOcrSettings(Map<String, Object> settings) {
        try {
            logger.info("Configuring OCR settings: {}", settings);
            // OCR settings are handled in OcrConfig, but we can log user preferences
        } catch (Exception e) {
            logger.error("Error configuring OCR settings", e);
        }
    }

    @Override
    public Map<String, Object> testOcrFunctionality(MultipartFile testImageFile) {
        try {
            logger.info("Testing OCR functionality with file: {}", testImageFile.getOriginalFilename());
            
            Map<String, Object> result = ocrService.extractTextWithConfidence(testImageFile);
            
            // Add OCR availability status
            result.put("ocrAvailable", ocrService.isOcrAvailable());
            result.put("ocrConfig", ocrService.getOcrConfiguration());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error testing OCR functionality", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("ocrAvailable", false);
            return result;
        }
    }

    @Override
    public String extractQrCodeData(MultipartFile imageFile) {
        try {
            logger.info("Extracting QR code data from: {}", imageFile.getOriginalFilename());
            
            // First extract all text from image
            String extractedText = ocrService.extractTextFromImage(imageFile);
            
            if (extractedText != null) {
                // Look for UPI QR patterns in the extracted text
                Pattern upiPattern = Pattern.compile("upi://pay\\?[^\\s]+", Pattern.CASE_INSENSITIVE);
                Matcher matcher = upiPattern.matcher(extractedText);
                
                if (matcher.find()) {
                    return matcher.group();
                }
            }
            
            // TODO: Implement dedicated QR code detection using ZXing library
            logger.warn("Dedicated QR code detection not implemented yet");
            return null;
            
        } catch (Exception e) {
            logger.error("Error extracting QR code data", e);
            return null;
        }
    }

    @Override
    public Expense processQrPaymentData(String qrData, Long userId) {
        try {
            logger.info("Processing QR payment data for user: {}", userId);
            
            if (qrData != null && qrData.toLowerCase().startsWith("upi://")) {
                // Parse UPI QR code data
                return parseUpiQrCode(qrData, userId);
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error processing QR payment data", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> processBulkUpload(MultipartFile zipFile, Long userId) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Processing bulk upload from ZIP file for user: {}", userId);
            
            if (!SUPPORTED_ARCHIVE_TYPES.contains(zipFile.getContentType())) {
                throw new RuntimeException("Unsupported archive type: " + zipFile.getContentType());
            }
            
            List<Expense> createdExpenses = new ArrayList<>();
            List<String> failedFiles = new ArrayList<>();
            int totalFiles = 0;
            
            try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
                ZipEntry entry;
                
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        totalFiles++;
                        
                        try {
                            // Read file content from ZIP
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = zipInputStream.read(buffer)) > 0) {
                                baos.write(buffer, 0, len);
                            }
                            
                            // Create MultipartFile from ZIP entry
                            MultipartFile entryFile = createMultipartFileFromBytes(
                                entry.getName(), baos.toByteArray());
                            
                            if (isSupportedFileType(entryFile)) {
                                Expense expense = processUploadedFile(entryFile, userId);
                                if (expense != null) {
                                    createdExpenses.add(expense);
                                } else {
                                    failedFiles.add(entry.getName());
                                }
                            } else {
                                failedFiles.add(entry.getName() + " (unsupported type)");
                            }
                            
                        } catch (Exception e) {
                            logger.warn("Failed to process file {} from ZIP: {}", entry.getName(), e.getMessage());
                            failedFiles.add(entry.getName() + " (error: " + e.getMessage() + ")");
                        }
                    }
                }
            }
            
            results.put("success", true);
            results.put("totalFiles", totalFiles);
            results.put("successfulExtractions", createdExpenses.size());
            results.put("failedFiles", failedFiles.size());
            results.put("createdExpenses", createdExpenses);
            results.put("failedFilesList", failedFiles);
            
            logger.info("Bulk upload completed: {}/{} files processed successfully", 
                       createdExpenses.size(), totalFiles);
            
        } catch (Exception e) {
            logger.error("Error processing bulk upload", e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }
        
        return results;
    }

    @Override
    public List<Map<String, Object>> getProcessingHistory(Long userId) {
        return processingHistory.getOrDefault(userId, new ArrayList<>());
    }

    @Override
    public Map<String, Object> reprocessFailedExtractions(Long userId) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Reprocessing failed extractions for user: {}", userId);
            
            List<Map<String, Object>> history = getProcessingHistory(userId);
            List<Map<String, Object>> failedItems = history.stream()
                .filter(item -> !(Boolean) item.getOrDefault("success", false))
                .toList();
            
            results.put("success", true);
            results.put("failedItemsFound", failedItems.size());
            results.put("message", "Reprocessing feature requires file storage implementation");
            
        } catch (Exception e) {
            logger.error("Error reprocessing failed extractions", e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }
        
        return results;
    }

    @Override
    public byte[] exportExtractedTransactions(Long userId, String format, 
                                            LocalDate startDate, LocalDate endDate) {
        try {
            logger.info("Exporting extracted transactions for user: {} in format: {}", userId, format);
            
            // TODO: Implement export functionality
            throw new RuntimeException("Export functionality not implemented yet");
            
        } catch (Exception e) {
            logger.error("Error exporting transactions", e);
            throw new RuntimeException("Failed to export transactions", e);
        }
    }

    @Override
    public int getExtractionConfidence(String transactionText) {
        try {
            if (transactionText == null || transactionText.trim().isEmpty()) {
                return 0;
            }
            
            int confidence = 0;
            String text = transactionText.toLowerCase();
            
            // Check for amount patterns
            if (text.matches(".*(?:rs\\.?|inr|₹)\\s*\\d+.*")) {
                confidence += 30;
            }
            
            // Check for transaction keywords
            if (text.contains("paid") || text.contains("received") || 
                text.contains("transaction") || text.contains("upi")) {
                confidence += 25;
            }
            
            // Check for UPI patterns
            if (text.contains("@") && text.contains("upi")) {
                confidence += 20;
            }
            
            // Check for merchant patterns
            if (text.contains("to:") || text.contains("from:") || text.contains("merchant")) {
                confidence += 15;
            }
            
            // Check for reference/transaction ID
            if (text.matches(".*\\b[A-Z0-9]{8,}\\b.*")) {
                confidence += 10;
            }
            
            return Math.min(confidence, 100);
            
        } catch (Exception e) {
            logger.error("Error calculating extraction confidence", e);
            return 0;
        }
    }

    @Override
    public void improveExtractionWithFeedback(String originalText, 
                                            Map<String, Object> correctedData, 
                                            Long userId) {
        try {
            logger.info("Recording extraction feedback for user: {}", userId);
            
            // TODO: Implement machine learning feedback loop
            // Store feedback for model improvement
            Map<String, Object> feedbackEntry = new HashMap<>();
            feedbackEntry.put("originalText", originalText);
            feedbackEntry.put("correctedData", correctedData);
            feedbackEntry.put("userId", userId);
            feedbackEntry.put("timestamp", new Date());
            
            // For now, just log the feedback
            logger.debug("Feedback recorded: {}", feedbackEntry);
            
        } catch (Exception e) {
            logger.error("Error recording extraction feedback", e);
        }
    }

    @Override
    public Map<Long, Double> getSuggestedCategories(String transactionText, Long userId) {
        try {
            logger.debug("Getting suggested categories for user: {}", userId);
            
            // TODO: Implement AI-based category suggestion with confidence scores
            Map<Long, Double> suggestions = new HashMap<>();
            
            if (transactionText != null) {
                String text = transactionText.toLowerCase();
                
                // Simple rule-based suggestions for now
                if (text.contains("food") || text.contains("restaurant") || text.contains("zomato") || text.contains("swiggy")) {
                    suggestions.put(1L, 0.85); // Food & Dining
                } else if (text.contains("uber") || text.contains("ola") || text.contains("taxi") || text.contains("petrol")) {
                    suggestions.put(2L, 0.80); // Transportation
                } else if (text.contains("amazon") || text.contains("flipkart") || text.contains("shopping")) {
                    suggestions.put(3L, 0.75); // Shopping
                } else {
                    suggestions.put(4L, 0.60); // Others
                }
            }
            
            return suggestions;
            
        } catch (Exception e) {
            logger.error("Error getting suggested categories", e);
            return new HashMap<>();
        }
    }

    @Override
    public void setAutoProcessingEnabled(Long userId, boolean enabled) {
        try {
            logger.info("Auto-processing {} for user: {}", enabled ? "enabled" : "disabled", userId);
            
            // TODO: Store user preference in database
            Map<String, Object> userStats = userStatistics.computeIfAbsent(userId, k -> new HashMap<>());
            userStats.put("autoProcessingEnabled", enabled);
            
        } catch (Exception e) {
            logger.error("Error setting auto-processing preference", e);
        }
    }

    @Override
    public boolean isAutoProcessingEnabled(Long userId) {
        try {
            Map<String, Object> userStats = userStatistics.get(userId);
            if (userStats != null) {
                return (Boolean) userStats.getOrDefault("autoProcessingEnabled", true);
            }
            return true; // Default to enabled
            
        } catch (Exception e) {
            logger.error("Error checking auto-processing preference", e);
            return false;
        }
    }

    // Private helper methods

    private String extractTextFromPdfUsingOcr(MultipartFile pdfFile) {
        try {
            logger.debug("Extracting text from PDF using OCR: {}", pdfFile.getOriginalFilename());
            
            PDDocument document = PDDocument.load(pdfFile.getInputStream());
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            StringBuilder allText = new StringBuilder();
            
            // Convert each page to image and OCR it
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                String pageText = ocrService.extractTextFromImage(image);
                
                if (pageText != null && !pageText.trim().isEmpty()) {
                    allText.append(pageText).append("\n");
                }
            }
            
            document.close();
            
            return allText.toString();
            
        } catch (Exception e) {
            logger.error("Error extracting text from PDF using OCR", e);
            return null;
        }
    }

    private Map<String, Object> createDocumentExtractionResult(String text, MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", text != null && !text.trim().isEmpty());
        result.put("text", text);
        result.put("confidence", text != null ? 95.0 : 0.0); // High confidence for text documents
        result.put("fileType", file.getContentType());
        result.put("fileName", file.getOriginalFilename());
        return result;
    }

    private String buildTransactionText(Map<String, Object> transactionData) {
        StringBuilder text = new StringBuilder();
        
        if (transactionData.get("amount") != null) {
            text.append("Amount: ₹").append(transactionData.get("amount")).append("\n");
        }
        
        if (transactionData.get("merchantName") != null) {
            text.append("Merchant: ").append(transactionData.get("merchantName")).append("\n");
        }
        
        if (transactionData.get("date") != null) {
            text.append("Date: ").append(transactionData.get("date")).append("\n");
        }
        
        if (transactionData.get("time") != null) {
            text.append("Time: ").append(transactionData.get("time")).append("\n");
        }
        
        if (transactionData.get("upiId") != null) {
            text.append("UPI ID: ").append(transactionData.get("upiId")).append("\n");
        }
        
        if (transactionData.get("transactionId") != null) {
            text.append("Transaction ID: ").append(transactionData.get("transactionId")).append("\n");
        }
        
        // Include raw text as well
        if (transactionData.get("rawText") != null) {
            text.append("\nRaw Text:\n").append(transactionData.get("rawText"));
        }
        
        return text.toString();
    }

    private Expense parseExtractedText(String extractedText, Long userId) {
        // Try UPI transaction parsing first
        Expense expense = parseUpiTransaction(extractedText, userId);
        
        if (expense == null) {
            // Try general transaction parsing
            expense = parseGeneralTransaction(extractedText, userId);
        }
        
        return expense;
    }

    private Expense parseGeneralTransaction(String extractedText, Long userId) {
        try {
            // Look for transaction patterns in the extracted text
            Pattern amountPattern = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
            Matcher amountMatcher = amountPattern.matcher(extractedText);
            
            if (amountMatcher.find()) {
                // Found amount, try to create transaction using UPI processor
                return upiTransactionProcessor.processUpiTransaction(extractedText, userId);
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn("Error parsing general transaction", e);
            return null;
        }
    }

    private Expense parseUpiQrCode(String qrData, Long userId) {
        try {
            // Parse UPI QR code format: upi://pay?pa=merchant@upi&pn=MerchantName&am=100&cu=INR&tn=TransactionNote
            String transactionText = "UPI QR Payment\n";
            
            // Extract parameters from QR code
            String[] params = qrData.split("[?&]");
            for (String param : params) {
                if (param.contains("=")) {
                    String[] keyValue = param.split("=", 2);
                    String key = keyValue[0];
                    String value = keyValue.length > 1 ? keyValue[1] : "";
                    
                    switch (key) {
                        case "pa":
                            transactionText += "To: " + value + "\n";
                            break;
                        case "pn":
                            transactionText += "Merchant: " + value + "\n";
                            break;
                        case "am":
                            transactionText += "Amount: ₹" + value + "\n";
                            break;
                        case "tn":
                            transactionText += "Note: " + value + "\n";
                            break;
                    }
                }
            }
            
            return upiTransactionProcessor.processUpiTransaction(transactionText, userId);
            
        } catch (Exception e) {
            logger.error("Error parsing UPI QR code", e);
            return null;
        }
    }

    private void recordProcessingHistory(Long userId, MultipartFile file, boolean success, 
                                       String message, Expense expense) {
        try {
            List<Map<String, Object>> history = processingHistory.computeIfAbsent(userId, k -> new ArrayList<>());
            
            Map<String, Object> entry = new HashMap<>();
            entry.put("fileName", file.getOriginalFilename());
            entry.put("fileSize", file.getSize());
            entry.put("fileType", file.getContentType());
            entry.put("success", success);
            entry.put("message", message);
            entry.put("timestamp", new Date());
            
            if (expense != null) {
                entry.put("expenseId", expense.getId());
                entry.put("amount", expense.getAmount());
            }
            
            history.add(entry);
            
            // Keep only last 100 entries per user
            if (history.size() > 100) {
                history.remove(0);
            }
            
        } catch (Exception e) {
            logger.error("Error recording processing history", e);
        }
    }

    private void updateProcessingStatistics(Long userId, boolean success, String fileType) {
        try {
            Map<String, Object> stats = userStatistics.computeIfAbsent(userId, k -> new HashMap<>());
            
            stats.put("totalProcessed", (Integer) stats.getOrDefault("totalProcessed", 0) + 1);
            
            if (success) {
                stats.put("successfulExtractions", (Integer) stats.getOrDefault("successfulExtractions", 0) + 1);
            } else {
                stats.put("failedExtractions", (Integer) stats.getOrDefault("failedExtractions", 0) + 1);
            }
            
            // Track by file type
            @SuppressWarnings("unchecked")
            Map<String, Integer> typeStats = (Map<String, Integer>) stats.computeIfAbsent("byFileType", k -> new HashMap<>());
            typeStats.put(fileType, typeStats.getOrDefault(fileType, 0) + 1);
            
            stats.put("lastProcessed", new Date());
            
        } catch (Exception e) {
            logger.error("Error updating processing statistics", e);
        }
    }

    private MultipartFile createMultipartFileFromBytes(String filename, byte[] content) {
        return new MultipartFile() {
            @Override
            public String getName() { return "file"; }
            
            @Override
            public String getOriginalFilename() { return filename; }
            
            @Override
            public String getContentType() {
                // Determine content type based on file extension
                String lower = filename.toLowerCase();
                if (lower.endsWith(".pdf")) return "application/pdf";
                if (lower.endsWith(".png")) return "image/png";
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
                if (lower.endsWith(".txt")) return "text/plain";
                return "application/octet-stream";
            }
            
            @Override
            public boolean isEmpty() { return content.length == 0; }
            
            @Override
            public long getSize() { return content.length; }
            
            @Override
            public byte[] getBytes() { return content; }
            
            @Override
            public InputStream getInputStream() { return new ByteArrayInputStream(content); }
            
            @Override
            public void transferTo(java.io.File dest) throws IOException {
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(content);
                }
            }
        };
    }
}