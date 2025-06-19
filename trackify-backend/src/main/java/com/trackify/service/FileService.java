package com.trackify.service;

import com.trackify.dto.response.ReceiptResponse;
import com.trackify.enums.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService {
    
    // File upload operations
    ReceiptResponse uploadReceipt(MultipartFile file, Long expenseId, Long userId) throws IOException;
    List<ReceiptResponse> uploadMultipleReceipts(List<MultipartFile> files, Long expenseId, Long userId) throws IOException;
    
    // File download operations
    byte[] downloadReceipt(Long receiptId, Long userId) throws IOException;
    byte[] downloadReceiptByFilename(String filename, Long userId) throws IOException;
    byte[] downloadReceiptByOriginalFilename(String originalFilename, Long userId) throws IOException;
    
    // File management
    void deleteReceipt(Long receiptId, Long userId) throws IOException;
    void deleteReceiptFile(String filename) throws IOException;
    
    // Receipt operations
    ReceiptResponse getReceiptById(Long receiptId, Long userId);
    List<ReceiptResponse> getReceiptsByExpense(Long expenseId, Long userId);
    List<ReceiptResponse> getUserReceipts(Long userId);
    
    // File validation
    boolean isValidFileType(MultipartFile file);
    boolean isValidFileSize(MultipartFile file);
    String validateFile(MultipartFile file);
    
    // File processing
    void processReceipt(Long receiptId) throws IOException;
    String extractTextFromReceipt(Long receiptId) throws IOException;
    String extractDataFromReceipt(Long receiptId) throws IOException;
    
    // File utility methods
    String generateUniqueFilename(String originalFilename);
    String getFileExtension(String filename);
    FileType determineFileType(String filename, String contentType);
    String generateThumbnail(String filePath) throws IOException;
    
    // Storage operations
    String saveFile(MultipartFile file, String directory) throws IOException;
    void deleteFile(String filePath) throws IOException;
    boolean fileExists(String filePath);
    long getFileSize(String filePath);
    
    // Storage statistics
    long getTotalStorageUsed();
    long getUserStorageUsed(Long userId);
    List<ReceiptResponse> getLargeFiles(long sizeThreshold);
    
    // File URL generation
    String generateFileUrl(String filename);
    String generateThumbnailUrl(String filename);
    String generateDownloadUrl(Long receiptId);
    
    // Cleanup operations
    void cleanupOrphanedFiles();
    void cleanupOldFiles(int daysOld);
    
    // Configuration
    long getMaxFileSize();
    List<String> getAllowedFileExtensions();
    String getUploadDirectory();
}