package com.trackify.controller;

import com.trackify.dto.response.ApiResponse;
import com.trackify.entity.Expense;
import com.trackify.service.BankIntegrationService;
import com.trackify.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bank-integration")
@CrossOrigin(origins = "*")
public class BankIntegrationController {

    private static final Logger logger = LoggerFactory.getLogger(BankIntegrationController.class);

    @Autowired
    private BankIntegrationService bankIntegrationService;

    @Autowired
    private OcrService ocrService;

    @PostMapping("/upload-file/{userId}")
    public ResponseEntity<ApiResponse<Expense>> uploadFile(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            logger.info("Processing file upload for user: {}", userId);
            
            Expense expense = bankIntegrationService.processUploadedFile(file, userId);
            
            if (expense != null) {
                return ResponseEntity.ok(ApiResponse.success("File processed successfully", expense));
            } else {
                // FIXED: Using the correct error method signature
                return ResponseEntity.ok(ApiResponse.<Expense>error("No transaction data found in file", null));
            }
            
        } catch (Exception e) {
            logger.error("Error processing file upload", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Expense>error("Failed to process file: " + e.getMessage(), null));
        }
    }

    @PostMapping("/upload-batch/{userId}")
    public ResponseEntity<ApiResponse<List<Expense>>> uploadBatch(
            @PathVariable Long userId,
            @RequestParam("files") List<MultipartFile> files) {
        
        try {
            logger.info("Processing batch upload for user: {}", userId);
            
            List<Expense> expenses = bankIntegrationService.processUploadedFilesBatch(files, userId);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Processed %d files successfully", expenses.size()), 
                    expenses));
            
        } catch (Exception e) {
            logger.error("Error processing batch upload", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<Expense>>error("Failed to process files: " + e.getMessage(), null));
        }
    }

    @PostMapping("/test-ocr")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testOcr(
            @RequestParam("file") MultipartFile file) {
        
        try {
            Map<String, Object> result = bankIntegrationService.testOcrFunctionality(file);
            return ResponseEntity.ok(ApiResponse.success("OCR test completed", result));
            
        } catch (Exception e) {
            logger.error("Error testing OCR", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>error("OCR test failed: " + e.getMessage(), null));
        }
    }

    @GetMapping("/ocr-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOcrStatus() {
        try {
            Map<String, Object> status = ocrService.getOcrConfiguration();
            status.put("available", ocrService.isOcrAvailable());
            
            return ResponseEntity.ok(ApiResponse.success("OCR status retrieved", status));
            
        } catch (Exception e) {
            logger.error("Error getting OCR status", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>error("Failed to get OCR status: " + e.getMessage(), null));
        }
    }

    @GetMapping("/supported-types")
    public ResponseEntity<ApiResponse<List<String>>> getSupportedFileTypes() {
        try {
            List<String> types = bankIntegrationService.getSupportedFileTypes();
            return ResponseEntity.ok(ApiResponse.success("Supported file types retrieved", types));
            
        } catch (Exception e) {
            logger.error("Error getting supported file types", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<String>>error("Failed to get supported types: " + e.getMessage(), null));
        }
    }

    @GetMapping("/statistics/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = bankIntegrationService.getExtractionStatistics(userId);
            return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", stats));
            
        } catch (Exception e) {
            logger.error("Error getting statistics", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>error("Failed to get statistics: " + e.getMessage(), null));
        }
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProcessingHistory(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> history = bankIntegrationService.getProcessingHistory(userId);
            return ResponseEntity.ok(ApiResponse.success("Processing history retrieved", history));
            
        } catch (Exception e) {
            logger.error("Error getting processing history", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<Map<String, Object>>>error("Failed to get processing history: " + e.getMessage(), null));
        }
    }

    @PostMapping("/sms-transaction/{userId}")
    public ResponseEntity<ApiResponse<Expense>> processSmsTransaction(
            @PathVariable Long userId,
            @RequestBody Map<String, String> smsData) {
        
        try {
            String content = smsData.get("content");
            String sender = smsData.get("sender");
            
            Expense expense = bankIntegrationService.processSmsTransaction(content, sender, userId);
            
            if (expense != null) {
                return ResponseEntity.ok(ApiResponse.success("SMS processed successfully", expense));
            } else {
                return ResponseEntity.ok(ApiResponse.<Expense>error("No transaction data found in SMS", null));
            }
            
        } catch (Exception e) {
            logger.error("Error processing SMS transaction", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Expense>error("Failed to process SMS: " + e.getMessage(), null));
        }
    }

    @PostMapping("/bank-statement/{userId}")
    public ResponseEntity<ApiResponse<List<Expense>>> processBankStatement(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile statementFile) {
        
        try {
            List<Expense> expenses = bankIntegrationService.processBankStatement(statementFile, userId);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Processed %d transactions from statement", expenses.size()), 
                    expenses));
            
        } catch (Exception e) {
            logger.error("Error processing bank statement", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<Expense>>error("Failed to process bank statement: " + e.getMessage(), null));
        }
    }

    @PostMapping("/bulk-upload/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processBulkUpload(
            @PathVariable Long userId,
            @RequestParam("zipFile") MultipartFile zipFile) {
        
        try {
            Map<String, Object> result = bankIntegrationService.processBulkUpload(zipFile, userId);
            
            return ResponseEntity.ok(ApiResponse.success("Bulk upload processed", result));
            
        } catch (Exception e) {
            logger.error("Error processing bulk upload", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>error("Failed to process bulk upload: " + e.getMessage(), null));
        }
    }

    @PostMapping("/extract-text")
    public ResponseEntity<ApiResponse<String>> extractTextOnly(
            @RequestParam("file") MultipartFile file) {
        
        try {
            String extractedText;
            
            if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
                extractedText = bankIntegrationService.extractTextFromImage(file);
            } else if ("application/pdf".equals(file.getContentType())) {
                extractedText = bankIntegrationService.extractTextFromPdf(file);
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<String>error("Unsupported file type", null));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Text extracted successfully", extractedText));
            
        } catch (Exception e) {
            logger.error("Error extracting text", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>error("Failed to extract text: " + e.getMessage(), null));
        }
    }

    // Additional endpoints for enhanced functionality
    
    @PostMapping("/extract-structured-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> extractStructuredData(
            @RequestParam("file") MultipartFile file) {
        
        try {
            Map<String, Object> result = ocrService.extractTransactionData(file);
            return ResponseEntity.ok(ApiResponse.success("Structured data extracted successfully", result));
            
        } catch (Exception e) {
            logger.error("Error extracting structured data", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>error("Failed to extract structured data: " + e.getMessage(), null));
        }
    }

    @PostMapping("/validate-sms")
    public ResponseEntity<ApiResponse<Boolean>> validateSms(
            @RequestBody Map<String, String> smsData) {
        
        try {
            String content = smsData.get("content");
            String sender = smsData.get("sender");
            
            boolean isValid = bankIntegrationService.isValidTransactionSms(content, sender);
            
            return ResponseEntity.ok(ApiResponse.success("SMS validation completed", isValid));
            
        } catch (Exception e) {
            logger.error("Error validating SMS", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Boolean>error("Failed to validate SMS: " + e.getMessage(), null));
        }
    }

    @GetMapping("/bank-senders")
    public ResponseEntity<ApiResponse<Map<String, String>>> getBankSenders() {
        try {
            Map<String, String> senders = bankIntegrationService.getRecognizedBankSenders();
            return ResponseEntity.ok(ApiResponse.success("Bank senders retrieved", senders));
            
        } catch (Exception e) {
            logger.error("Error getting bank senders", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, String>>error("Failed to get bank senders: " + e.getMessage(), null));
        }
    }

    @PostMapping("/confidence-score")
    public ResponseEntity<ApiResponse<Integer>> getConfidenceScore(
            @RequestBody Map<String, String> data) {
        
        try {
            String text = data.get("text");
            int confidence = bankIntegrationService.getExtractionConfidence(text);
            
            return ResponseEntity.ok(ApiResponse.success("Confidence score calculated", confidence));
            
        } catch (Exception e) {
            logger.error("Error calculating confidence score", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Integer>error("Failed to calculate confidence: " + e.getMessage(), null));
        }
    }

    @PostMapping("/auto-processing/{userId}")
    public ResponseEntity<ApiResponse<Void>> setAutoProcessing(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> settings) {
        
        try {
            boolean enabled = settings.getOrDefault("enabled", true);
            bankIntegrationService.setAutoProcessingEnabled(userId, enabled);
            
            String message = enabled ? "Auto-processing enabled" : "Auto-processing disabled";
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            logger.error("Error setting auto-processing", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>error("Failed to set auto-processing: " + e.getMessage(), null));
        }
    }

    @GetMapping("/auto-processing/{userId}")
    public ResponseEntity<ApiResponse<Boolean>> getAutoProcessingStatus(@PathVariable Long userId) {
        try {
            boolean enabled = bankIntegrationService.isAutoProcessingEnabled(userId);
            return ResponseEntity.ok(ApiResponse.success("Auto-processing status retrieved", enabled));
            
        } catch (Exception e) {
            logger.error("Error getting auto-processing status", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Boolean>error("Failed to get auto-processing status: " + e.getMessage(), null));
        }
    }
}