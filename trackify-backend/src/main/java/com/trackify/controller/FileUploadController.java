package com.trackify.controller;

import com.trackify.dto.response.ApiResponse;
import com.trackify.dto.response.ReceiptResponse;
import com.trackify.exception.ForbiddenException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.security.UserPrincipal;
import com.trackify.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Management", description = "APIs for file upload and management")
public class FileUploadController {
    
    @Autowired
    private FileService fileService;
    
    @PostMapping("/receipts/upload")
    @Operation(summary = "Upload receipt", description = "Upload a receipt file for an expense")
    public ResponseEntity<ApiResponse<ReceiptResponse>> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam("expenseId") Long expenseId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        try {
            ReceiptResponse receipt = fileService.uploadReceipt(file, expenseId, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Receipt uploaded successfully", receipt));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to upload receipt: " + e.getMessage(), 500));
        }
    }
    
    @PostMapping("/receipts/upload-multiple")
    @Operation(summary = "Upload multiple receipts", description = "Upload multiple receipt files for an expense")
    public ResponseEntity<ApiResponse<List<ReceiptResponse>>> uploadMultipleReceipts(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("expenseId") Long expenseId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        try {
            List<ReceiptResponse> receipts = fileService.uploadMultipleReceipts(files, expenseId, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Receipts uploaded successfully", receipts));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to upload receipts: " + e.getMessage(), 500));
        }
    }
    
    @GetMapping("/receipts/{id}")
    @Operation(summary = "Get receipt details", description = "Get receipt details by ID")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceiptById(
            @Parameter(description = "Receipt ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        ReceiptResponse receipt = fileService.getReceiptById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Receipt retrieved successfully", receipt));
    }
    
    @GetMapping("/receipts/expense/{expenseId}")
    @Operation(summary = "Get expense receipts", description = "Get all receipts for an expense")
    public ResponseEntity<ApiResponse<List<ReceiptResponse>>> getReceiptsByExpense(
            @Parameter(description = "Expense ID") @PathVariable Long expenseId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<ReceiptResponse> receipts = fileService.getReceiptsByExpense(expenseId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Expense receipts retrieved successfully", receipts));
    }
    
    @GetMapping("/receipts/my-receipts")
    @Operation(summary = "Get user receipts", description = "Get all receipts uploaded by the current user")
    public ResponseEntity<ApiResponse<List<ReceiptResponse>>> getUserReceipts(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<ReceiptResponse> receipts = fileService.getUserReceipts(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User receipts retrieved successfully", receipts));
    }
    
    @GetMapping("/receipts/download/{id}")
    @Operation(summary = "Download receipt", description = "Download a receipt file")
    public ResponseEntity<Resource> downloadReceipt(
            @Parameter(description = "Receipt ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        try {
            byte[] fileData = fileService.downloadReceipt(id, currentUser.getId());
            ReceiptResponse receipt = fileService.getReceiptById(id, currentUser.getId());
            
            ByteArrayResource resource = new ByteArrayResource(fileData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + receipt.getOriginalFilename() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, receipt.getMimeType());
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(fileData.length)
                .body(resource);
                
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/receipts/download/filename/{filename}")
    @Operation(summary = "Download receipt by original filename", description = "Download a receipt file by its original filename")
    public ResponseEntity<Resource> downloadReceiptByFilename(
            @Parameter(description = "Original filename") @PathVariable String filename,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        try {
            byte[] fileData = fileService.downloadReceiptByOriginalFilename(filename, currentUser.getId());

            ByteArrayResource resource = new ByteArrayResource(fileData);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            
            // Add content type header
            headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileData.length)
                    .body(resource);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/receipts/{id}")
    @Operation(summary = "Delete receipt", description = "Delete a receipt file")
    public ResponseEntity<ApiResponse<Void>> deleteReceipt(
            @Parameter(description = "Receipt ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        try {
            fileService.deleteReceipt(id, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Receipt deleted successfully", null));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete receipt: " + e.getMessage(), 500));
        }
    }
    
    @PostMapping("/receipts/process/{id}")
    @Operation(summary = "Process receipt", description = "Process a receipt for OCR and data extraction")
    public ResponseEntity<ApiResponse<Void>> processReceipt(
            @Parameter(description = "Receipt ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        try {
            fileService.processReceipt(id);
            return ResponseEntity.ok(ApiResponse.success("Receipt processing started", null));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to process receipt: " + e.getMessage(), 500));
        }
    }
    
    @GetMapping("/validation/file-types")
    @Operation(summary = "Get allowed file types", description = "Get list of allowed file extensions")
    public ResponseEntity<ApiResponse<List<String>>> getAllowedFileTypes() {
        
        List<String> allowedExtensions = fileService.getAllowedFileExtensions();
        return ResponseEntity.ok(ApiResponse.success("Allowed file types retrieved successfully", allowedExtensions));
    }
    
    @GetMapping("/validation/max-size")
    @Operation(summary = "Get max file size", description = "Get maximum allowed file size")
    public ResponseEntity<ApiResponse<Long>> getMaxFileSize() {
        
        long maxSize = fileService.getMaxFileSize();
        return ResponseEntity.ok(ApiResponse.success("Max file size retrieved successfully", maxSize));
    }
    
    @GetMapping("/storage/usage")
    @Operation(summary = "Get storage usage", description = "Get storage usage statistics")
    public ResponseEntity<ApiResponse<Long>> getUserStorageUsage(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        long storageUsed = fileService.getUserStorageUsed(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Storage usage retrieved successfully", storageUsed));
    }
    
    @GetMapping("/storage/total")
    @Operation(summary = "Get total storage usage", description = "Get total storage usage across all users")
    public ResponseEntity<ApiResponse<Long>> getTotalStorageUsage() {
        
        long totalStorage = fileService.getTotalStorageUsed();
        return ResponseEntity.ok(ApiResponse.success("Total storage usage retrieved successfully", totalStorage));
    }
    
    @GetMapping("/storage/large-files")
    @Operation(summary = "Get large files", description = "Get list of large files above threshold")
    public ResponseEntity<ApiResponse<List<ReceiptResponse>>> getLargeFiles(
            @RequestParam(defaultValue = "5242880") long sizeThreshold) { // 5MB default
        
        List<ReceiptResponse> largeFiles = fileService.getLargeFiles(sizeThreshold);
        return ResponseEntity.ok(ApiResponse.success("Large files retrieved successfully", largeFiles));
    }
    
    @PostMapping("/maintenance/cleanup-orphaned")
    @Operation(summary = "Cleanup orphaned files", description = "Remove files that don't have database entries")
    public ResponseEntity<ApiResponse<Void>> cleanupOrphanedFiles() {
        
        fileService.cleanupOrphanedFiles();
        return ResponseEntity.ok(ApiResponse.success("Orphaned files cleanup completed", null));
    }
    
    @PostMapping("/maintenance/cleanup-old")
    @Operation(summary = "Cleanup old files", description = "Remove files older than specified days")
    public ResponseEntity<ApiResponse<Void>> cleanupOldFiles(
            @RequestParam(defaultValue = "365") int daysOld) {
        
        fileService.cleanupOldFiles(daysOld);
        return ResponseEntity.ok(ApiResponse.success("Old files cleanup completed", null));
    }
}