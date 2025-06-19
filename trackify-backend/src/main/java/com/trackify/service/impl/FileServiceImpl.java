package com.trackify.service.impl;

import com.trackify.dto.response.ReceiptResponse;
import com.trackify.entity.Receipt;
import com.trackify.enums.FileType;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ForbiddenException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.ReceiptRepository;
import com.trackify.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class FileServiceImpl implements FileService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
    
    @Autowired
    private ReceiptRepository receiptRepository;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Value("${app.file.upload-dir:uploads/}")
    private String uploadDirectory;
    
    @Value("${app.file.receipts-dir:uploads/receipts/}")
    private String receiptsDirectory;
    
    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSize;
    
    @Value("${app.file.allowed-extensions:pdf,jpg,jpeg,png,gif}")
    private String allowedExtensions;
    
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif",
        "application/pdf"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    @Override
    public ReceiptResponse uploadReceipt(MultipartFile file, Long expenseId, Long userId) throws IOException {
        logger.info("Uploading receipt for expense {} by user: {}", expenseId, userId);
        
        // Validate file
        String validationError = validateFile(file);
        if (validationError != null) {
            throw new BadRequestException(validationError);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String storedFilename = generateUniqueFilename(originalFilename);
        
        // Save file to disk
        String filePath = saveFile(file, receiptsDirectory);
        
        // Create receipt entity using constructor
        Receipt receipt = new Receipt();
        receipt.setOriginalFilename(originalFilename);
        receipt.setStoredFilename(storedFilename);
        receipt.setFilePath(filePath);
        receipt.setFileSize(file.getSize());
        receipt.setFileType(determineFileType(originalFilename, file.getContentType()));
        receipt.setMimeType(file.getContentType());
        receipt.setFileUrl(generateFileUrl(storedFilename));
        receipt.setIsProcessed(false);
        receipt.setExpenseId(expenseId);
        receipt.setUploadedBy(userId);
        
        // Generate thumbnail for images
        if (receipt.isImage()) {
            try {
                String thumbnailUrl = generateThumbnail(filePath);
                receipt.setThumbnailUrl(thumbnailUrl);
            } catch (Exception e) {
                logger.warn("Failed to generate thumbnail for receipt: {}", storedFilename, e);
            }
        }
        
        Receipt savedReceipt = receiptRepository.save(receipt);
        
        logger.info("Receipt uploaded successfully with id: {}", savedReceipt.getId());
        return convertToResponse(savedReceipt);
    }
    
    @Override
    public List<ReceiptResponse> uploadMultipleReceipts(List<MultipartFile> files, Long expenseId, Long userId) throws IOException {
        logger.info("Uploading {} receipts for expense {} by user: {}", files.size(), expenseId, userId);
        
        return files.stream()
                .map(file -> {
                    try {
                        return uploadReceipt(file, expenseId, userId);
                    } catch (IOException e) {
                        logger.error("Failed to upload receipt: {}", file.getOriginalFilename(), e);
                        throw new RuntimeException("Failed to upload receipt: " + file.getOriginalFilename(), e);
                    }
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public byte[] downloadReceipt(Long receiptId, Long userId) throws IOException {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + receiptId));

        // Validate access
        if (!receipt.getUploadedBy().equals(userId)) {
            throw new ForbiddenException("You don't have access to this receipt");
        }

        Path filePath = Paths.get(receipt.getFilePath());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Receipt file not found on disk");
        }

        return Files.readAllBytes(filePath);
    }
    
    @Override
    @Transactional(readOnly = true)
    public byte[] downloadReceiptByFilename(String filename, Long userId) throws IOException {
        Receipt receipt = receiptRepository.findByStoredFilename(filename)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with filename: " + filename));
        
        return downloadReceipt(receipt.getId(), userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public byte[] downloadReceiptByOriginalFilename(String originalFilename, Long userId) throws IOException {
        Receipt receipt = receiptRepository.findByOriginalFilename(originalFilename)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with filename: " + originalFilename));

        return downloadReceipt(receipt.getId(), userId);
    }

    @Override
    public void deleteReceipt(Long receiptId, Long userId) throws IOException {
        logger.info("Deleting receipt {} by user: {}", receiptId, userId);
        
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + receiptId));
        
        // Validate access
        if (!receipt.getUploadedBy().equals(userId)) {
            throw new ForbiddenException("You don't have access to this receipt");
        }
        
        // Delete file from disk
        deleteFile(receipt.getFilePath());
        
        // Delete thumbnail if exists
        if (receipt.getThumbnailUrl() != null) {
            try {
                deleteFile(receipt.getThumbnailUrl());
            } catch (Exception e) {
                logger.warn("Failed to delete thumbnail for receipt: {}", receiptId, e);
            }
        }
        
        // Delete from database
        receiptRepository.deleteById(receiptId);
        
        logger.info("Receipt deleted successfully: {}", receiptId);
    }
    
    @Override
    public void deleteReceiptFile(String filename) throws IOException {
        Path filePath = Paths.get(receiptsDirectory, filename);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            logger.info("Deleted file: {}", filename);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(Long receiptId, Long userId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + receiptId));
        
        // Validate access
        if (!receipt.getUploadedBy().equals(userId)) {
            throw new ForbiddenException("You don't have access to this receipt");
        }
        
        return convertToResponse(receipt);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByExpense(Long expenseId, Long userId) {
        List<Receipt> receipts = receiptRepository.findByExpenseIdOrderByCreatedAtAsc(expenseId);
        
        // Filter receipts that user has access to
        return receipts.stream()
                .filter(receipt -> receipt.getUploadedBy().equals(userId))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getUserReceipts(Long userId) {
        List<Receipt> receipts = receiptRepository.findByUploadedByOrderByCreatedAtDesc(userId);
        return receipts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isValidFileType(MultipartFile file) {
        if (file.getContentType() == null) {
            return false;
        }
        
        return ALLOWED_CONTENT_TYPES.contains(file.getContentType().toLowerCase());
    }
    
    @Override
    public boolean isValidFileSize(MultipartFile file) {
        return file.getSize() <= MAX_FILE_SIZE;
    }
    
    @Override
    public String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "File is required";
        }
        
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            return "Filename is required";
        }
        
        if (!isValidFileType(file)) {
            return "File type not allowed. Allowed types: " + String.join(", ", ALLOWED_CONTENT_TYPES);
        }
        
        if (!isValidFileSize(file)) {
            return "File size too large. Maximum size: " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB";
        }
        
        return null; // Valid file
    }
    
    @Override
    public void processReceipt(Long receiptId) throws IOException {
        logger.info("Processing receipt: {}", receiptId);
        
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + receiptId));
        
        if (receipt.getIsProcessed()) {
            logger.info("Receipt already processed: {}", receiptId);
            return;
        }
        
        try {
            // Extract text using OCR (placeholder implementation)
            String ocrText = extractTextFromReceipt(receiptId);
            
            // Extract structured data (placeholder implementation)
            String extractedData = extractDataFromReceipt(receiptId);
            
            // Update receipt
            receipt.setOcrText(ocrText);
            receipt.setExtractedData(extractedData);
            receipt.setIsProcessed(true);
            
            receiptRepository.save(receipt);
            
            logger.info("Receipt processed successfully: {}", receiptId);
        } catch (Exception e) {
            logger.error("Failed to process receipt: {}", receiptId, e);
            throw new IOException("Failed to process receipt", e);
        }
    }
    //TODO for extract Text from Receipt
    @Override
    public String extractTextFromReceipt(Long receiptId) throws IOException {
        // Placeholder implementation for OCR
        // In a real implementation, this would use OCR libraries like Tesseract
        logger.info("Extracting text from receipt: {}", receiptId);
        return "Sample OCR text extracted from receipt";
    }
    
    //TODO for extract data from Receipt
    @Override
    public String extractDataFromReceipt(Long receiptId) throws IOException {
        // Placeholder implementation for data extraction
        // In a real implementation, this would parse OCR text to extract structured data
        logger.info("Extracting structured data from receipt: {}", receiptId);
        return "{\"merchant\": \"Sample Store\", \"amount\": \"25.99\", \"date\": \"2024-01-15\"}";
    }
    
    @Override
    public String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFilename);
        
        return String.format("%s_%s.%s", timestamp, uuid, extension);
    }
    
    @Override
    public String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
    
    @Override
    public FileType determineFileType(String filename, String contentType) {
        String extension = getFileExtension(filename);
        
        if (Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension)) {
            return FileType.IMAGE;
        } else if ("pdf".equals(extension)) {
            return FileType.PDF;
        } else {
            return FileType.OTHER;
        }
    }
    
    
    //TODO generate Thumbnail  from the receipt uploaded
    @Override
    public String generateThumbnail(String filePath) throws IOException {
        // Placeholder implementation for thumbnail generation
        // In a real implementation, this would create image thumbnails
        logger.info("Generating thumbnail for file: {}", filePath);
        return "/thumbnails/" + getFileExtension(filePath) + "_thumbnail.jpg";
    }
    
    @Override
    public String saveFile(MultipartFile file, String directory) throws IOException {
        // Create directory if it doesn't exist
        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        // Generate unique filename
        String storedFilename = generateUniqueFilename(file.getOriginalFilename());
        Path filePath = dirPath.resolve(storedFilename);
        
        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return filePath.toString();
    }
    
    @Override
    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }
    
    @Override
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
    
    @Override
    public long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getTotalStorageUsed() {
        Long total = receiptRepository.getTotalStorageUsed();
        return total != null ? total : 0L;
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getUserStorageUsed(Long userId) {
        Long total = receiptRepository.getTotalStorageUsedByUser(userId);
        return total != null ? total : 0L;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getLargeFiles(long sizeThreshold) {
        List<Receipt> receipts = receiptRepository.findLargeReceipts(sizeThreshold);
        return receipts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public String generateFileUrl(String filename) {
        return "/api/files/receipts/" + filename;
    }
    
    @Override
    public String generateThumbnailUrl(String filename) {
        return "/api/files/thumbnails/" + filename;
    }
    
    @Override
    public String generateDownloadUrl(Long receiptId) {
        return "/api/files/receipts/download/" + receiptId;
    }
    
    
    //TODO cleanup orphaned files logic
    @Override
    public void cleanupOrphanedFiles() {
        logger.info("Starting cleanup of orphaned files");
        
        try {
            // Find files on disk that don't have corresponding database entries
            // This is a placeholder implementation
            logger.info("Orphaned files cleanup completed");
        } catch (Exception e) {
            logger.error("Failed to cleanup orphaned files", e);
        }
    }
    
    
    //TODO cleanupOldFiles logic
    @Override
    public void cleanupOldFiles(int daysOld) {
        logger.info("Starting cleanup of files older than {} days", daysOld);
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            // Implementation would find and delete old files
            logger.info("Old files cleanup completed");
        } catch (Exception e) {
            logger.error("Failed to cleanup old files", e);
        }
    }
    
    @Override
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }
    
    @Override
    public List<String> getAllowedFileExtensions() {
        return Arrays.asList(allowedExtensions.split(","));
    }
    
    @Override
    public String getUploadDirectory() {
        return uploadDirectory;
    }
    
    // Private helper methods
    
    private ReceiptResponse convertToResponse(Receipt receipt) {
        ReceiptResponse response = modelMapper.map(receipt, ReceiptResponse.class);
        response.setDisplaySize(receipt.getDisplaySize());
        response.setFileExtension(receipt.getFileExtension());
        response.setCanDownload(true);
        response.setCanDelete(true);
        return response;
    }
}