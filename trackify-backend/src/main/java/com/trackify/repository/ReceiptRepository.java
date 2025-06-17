package com.trackify.repository;

import com.trackify.entity.Receipt;
import com.trackify.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    
    // Find by expense
    List<Receipt> findByExpenseId(Long expenseId);
    List<Receipt> findByExpenseIdOrderByCreatedAtAsc(Long expenseId);
    
    // Find by user
    List<Receipt> findByUploadedBy(Long userId);
    List<Receipt> findByUploadedByOrderByCreatedAtDesc(Long userId);
    
    // Find by file type
    List<Receipt> findByFileType(FileType fileType);
    
    // Find processed/unprocessed receipts
    List<Receipt> findByIsProcessedTrue();
    List<Receipt> findByIsProcessedFalse();
    List<Receipt> findByExpenseIdAndIsProcessedFalse(Long expenseId);
    
    // Find by filename
    Optional<Receipt> findByStoredFilename(String storedFilename);
    Optional<Receipt> findByOriginalFilename(String originalFilename);
    
    // Count receipts
    long countByExpenseId(Long expenseId);
    long countByUploadedBy(Long userId);
    long countByFileType(FileType fileType);
    
    // Check if receipt exists
    boolean existsByStoredFilename(String storedFilename);
    boolean existsByExpenseIdAndOriginalFilename(Long expenseId, String originalFilename);
    
    // Update processing status
    @Modifying
    @Query("UPDATE Receipt r SET r.isProcessed = :isProcessed WHERE r.id = :receiptId")
    void updateProcessingStatus(@Param("receiptId") Long receiptId, @Param("isProcessed") Boolean isProcessed);
    
    // Update OCR text
    @Modifying
    @Query("UPDATE Receipt r SET r.ocrText = :ocrText, r.isProcessed = true WHERE r.id = :receiptId")
    void updateOcrText(@Param("receiptId") Long receiptId, @Param("ocrText") String ocrText);
    
    // Update extracted data
    @Modifying
    @Query("UPDATE Receipt r SET r.extractedData = :extractedData, r.isProcessed = true WHERE r.id = :receiptId")
    void updateExtractedData(@Param("receiptId") Long receiptId, @Param("extractedData") String extractedData);
    
    // Find receipts by size range
    @Query("SELECT r FROM Receipt r WHERE r.fileSize BETWEEN :minSize AND :maxSize")
    List<Receipt> findByFileSizeRange(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);
    
    // Find large receipts
    @Query("SELECT r FROM Receipt r WHERE r.fileSize > :sizeThreshold ORDER BY r.fileSize DESC")
    List<Receipt> findLargeReceipts(@Param("sizeThreshold") Long sizeThreshold);
    
    // Get total storage used by user
    @Query("SELECT SUM(r.fileSize) FROM Receipt r WHERE r.uploadedBy = :userId")
    Long getTotalStorageUsedByUser(@Param("userId") Long userId);
    
 // Find orphaned receipts
    @Query("SELECT r FROM Receipt r WHERE r.expenseId IS NULL AND r.createdAt < :cutoffTime")
    List<Receipt> findOrphanedReceipts(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Get total storage used
    @Query("SELECT SUM(r.fileSize) FROM Receipt r")
    Long getTotalStorageUsed();
    
    // Find receipts with OCR text
    @Query("SELECT r FROM Receipt r WHERE r.ocrText IS NOT NULL AND r.ocrText != ''")
    List<Receipt> findReceiptsWithOcrText();
    
    // Search receipts by OCR text
    @Query("SELECT r FROM Receipt r WHERE r.uploadedBy = :userId AND LOWER(r.ocrText) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Receipt> searchByOcrText(@Param("userId") Long userId, @Param("keyword") String keyword);
}