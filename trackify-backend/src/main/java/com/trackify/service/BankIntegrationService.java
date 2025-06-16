package com.trackify.service;

import com.trackify.entity.Expense;
import com.trackify.integration.sms.TransactionExtractor.SmsMessage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Service interface for bank integration operations including
 * file processing, SMS parsing, and transaction extraction
 */
public interface BankIntegrationService {

    /**
     * Process uploaded file (image/PDF) and extract transaction data
     * 
     * @param file The uploaded file (PNG, JPG, JPEG, PDF)
     * @param userId The user ID
     * @return Created expense from extracted data
     */
    Expense processUploadedFile(MultipartFile file, Long userId);

    /**
     * Process multiple uploaded files in batch
     * 
     * @param files List of uploaded files
     * @param userId The user ID
     * @return List of created expenses
     */
    List<Expense> processUploadedFilesBatch(List<MultipartFile> files, Long userId);

    /**
     * Extract transaction data from image using OCR
     * 
     * @param imageFile The image file
     * @param userId The user ID
     * @return Created expense or null if extraction failed
     */
    Expense extractFromImage(MultipartFile imageFile, Long userId);

    /**
     * Extract transaction data from PDF
     * 
     * @param pdfFile The PDF file
     * @param userId The user ID
     * @return Created expense or null if extraction failed
     */
    Expense extractFromPdf(MultipartFile pdfFile, Long userId);

    /**
     * Process SMS message and extract transaction details
     * 
     * @param smsContent The SMS content
     * @param sender The SMS sender
     * @param userId The user ID
     * @return Created expense or null if not a transaction SMS
     */
    Expense processSmsTransaction(String smsContent, String sender, Long userId);

    /**
     * Process multiple SMS messages in batch
     * 
     * @param smsMessages List of SMS messages
     * @param userId The user ID
     * @return List of created expenses
     */
    List<Expense> processSmsMessagesBatch(List<SmsMessage> smsMessages, Long userId);

    /**
     * Process bank statement file
     * 
     * @param statementFile The bank statement file (PDF/CSV/TXT)
     * @param userId The user ID
     * @return List of created expenses
     */
    List<Expense> processBankStatement(MultipartFile statementFile, Long userId);

    /**
     * Validate if file is supported for processing
     * 
     * @param file The file to validate
     * @return true if file is supported
     */
    boolean isSupportedFileType(MultipartFile file);

    /**
     * Get supported file types
     * 
     * @return List of supported MIME types
     */
    List<String> getSupportedFileTypes();

    /**
     * Extract text from image using OCR
     * 
     * @param imageFile The image file
     * @return Extracted text content
     */
    String extractTextFromImage(MultipartFile imageFile);

    /**
     * Extract text from PDF file
     * 
     * @param pdfFile The PDF file
     * @return Extracted text content
     */
    String extractTextFromPdf(MultipartFile pdfFile);

    /**
     * Parse UPI transaction data from text
     * 
     * @param transactionText The transaction text
     * @param userId The user ID
     * @return Created expense or null
     */
    Expense parseUpiTransaction(String transactionText, Long userId);

    /**
     * Get transaction extraction statistics for user
     * 
     * @param userId The user ID
     * @return Statistics map
     */
    Map<String, Object> getExtractionStatistics(Long userId);

    /**
     * Validate SMS transaction data
     * 
     * @param smsContent The SMS content
     * @param sender The SMS sender
     * @return true if valid transaction SMS
     */
    boolean isValidTransactionSms(String smsContent, String sender);

    /**
     * Get list of recognized bank senders
     * 
     * @return Map of sender codes to bank names
     */
    Map<String, String> getRecognizedBankSenders();

    /**
     * Configure OCR settings
     * 
     * @param settings OCR configuration settings
     */
    void configureOcrSettings(Map<String, Object> settings);

    /**
     * Test OCR functionality
     * 
     * @param testImageFile Test image file
     * @return OCR test results
     */
    Map<String, Object> testOcrFunctionality(MultipartFile testImageFile);

    /**
     * Extract QR code data from image
     * 
     * @param imageFile Image containing QR code
     * @return QR code data or null if not found
     */
    String extractQrCodeData(MultipartFile imageFile);

    /**
     * Process QR code payment data
     * 
     * @param qrData QR code data
     * @param userId User ID
     * @return Created expense or null
     */
    Expense processQrPaymentData(String qrData, Long userId);

    /**
     * Bulk upload and process files from ZIP
     * 
     * @param zipFile ZIP file containing transaction documents
     * @param userId User ID
     * @return Processing results
     */
    Map<String, Object> processBulkUpload(MultipartFile zipFile, Long userId);

    /**
     * Get file processing history for user
     * 
     * @param userId User ID
     * @return Processing history
     */
    List<Map<String, Object>> getProcessingHistory(Long userId);

    /**
     * Reprocess failed file extractions
     * 
     * @param userId User ID
     * @return Reprocessing results
     */
    Map<String, Object> reprocessFailedExtractions(Long userId);

    /**
     * Export extracted transactions to file
     * 
     * @param userId User ID
     * @param format Export format (CSV, PDF, XLSX)
     * @param startDate Start date for export
     * @param endDate End date for export
     * @return Export file content
     */
    byte[] exportExtractedTransactions(Long userId, String format, 
                                      java.time.LocalDate startDate, 
                                      java.time.LocalDate endDate);

    /**
     * Get extraction confidence score for a transaction
     * 
     * @param transactionText Extracted text
     * @return Confidence score (0-100)
     */
    int getExtractionConfidence(String transactionText);

    /**
     * Improve extraction accuracy with user feedback
     * 
     * @param originalText Original extracted text
     * @param correctedData User-corrected data
     * @param userId User ID
     */
    void improveExtractionWithFeedback(String originalText, 
                                      Map<String, Object> correctedData, 
                                      Long userId);

    /**
     * Get AI-suggested categories for extracted transactions
     * 
     * @param transactionText Transaction text
     * @param userId User ID
     * @return Suggested category IDs with confidence scores
     */
    Map<Long, Double> getSuggestedCategories(String transactionText, Long userId);

    /**
     * Enable/disable automatic transaction processing
     * 
     * @param userId User ID
     * @param enabled Whether auto-processing is enabled
     */
    void setAutoProcessingEnabled(Long userId, boolean enabled);

    /**
     * Check if auto-processing is enabled for user
     * 
     * @param userId User ID
     * @return true if auto-processing is enabled
     */
    boolean isAutoProcessingEnabled(Long userId);
}