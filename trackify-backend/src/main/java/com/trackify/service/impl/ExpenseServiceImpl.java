package com.trackify.service.impl;



import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.trackify.dto.request.ExpenseRequest;
import com.trackify.dto.response.ExpenseResponse;
import com.trackify.dto.response.ReceiptResponse;
import com.trackify.entity.Category;
import com.trackify.entity.Expense;
import com.trackify.entity.Receipt;
import com.trackify.entity.User;
import com.trackify.enums.ExpenseStatus;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ForbiddenException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.CategoryRepository;
import com.trackify.repository.ExpenseRepository;
import com.trackify.repository.ReceiptRepository;
import com.trackify.repository.UserRepository;
import com.trackify.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseServiceImpl implements ExpenseService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpenseServiceImpl.class);
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ReceiptRepository receiptRepository;
    
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Override
    public ExpenseResponse createExpense(ExpenseRequest expenseRequest, Long userId) {
        logger.info("Creating expense '{}' for user: {}", expenseRequest.getTitle(), userId);
        
        // Validate category exists and user has access
        validateCategoryAccess(expenseRequest.getCategoryId(), userId);
        
        // Create expense entity
        Expense expense = convertToEntity(expenseRequest, userId);
        Expense savedExpense = expenseRepository.save(expense);
        
        logger.info("Expense created successfully with id: {}", savedExpense.getId());
        return convertToResponse(savedExpense);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        
        validateExpenseAccess(expenseId, userId);
        return convertToResponse(expense);
    }
    
    @Override
    public ExpenseResponse updateExpense(Long expenseId, ExpenseRequest expenseRequest, Long userId) {
        logger.info("Updating expense {} by user: {}", expenseId, userId);
        
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        
        validateExpenseAccess(expenseId, userId);
        
        // Check if expense can be edited
        if (!canUserEditExpense(expenseId, userId)) {
            throw new ForbiddenException("This expense cannot be edited");
        }
        
        // Validate category if changed
        if (!expense.getCategoryId().equals(expenseRequest.getCategoryId())) {
            validateCategoryAccess(expenseRequest.getCategoryId(), userId);
        }
        
        // Update expense fields
        updateExpenseFromRequest(expense, expenseRequest);
        Expense updatedExpense = expenseRepository.save(expense);
        
        logger.info("Expense updated successfully: {}", expenseId);
        return convertToResponse(updatedExpense);
    }
    
    @Override
    public void deleteExpense(Long expenseId, Long userId) {
        logger.info("Deleting expense {} by user: {}", expenseId, userId);
        
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        
        validateExpenseAccess(expenseId, userId);
        
        if (!canUserDeleteExpense(expenseId, userId)) {
            throw new ForbiddenException("This expense cannot be deleted");
        }
        
        expenseRepository.deleteById(expenseId);
        logger.info("Expense deleted successfully: {}", expenseId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getUserExpenses(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdOrderByExpenseDateDesc(userId);
        return convertToResponseList(expenses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getUserExpensesPaginated(Long userId, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByUserId(userId, pageable);
        return expenses.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByCategory(Long categoryId, Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdAndCategoryId(userId, categoryId);
        return convertToResponseList(expenses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByCategoryPaginated(Long categoryId, Long userId, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByUserIdAndCategoryId(userId, categoryId, pageable);
        return expenses.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByStatus(Long userId, ExpenseStatus status) {
        List<Expense> expenses = expenseRepository.findByUserIdAndStatus(userId, status);
        return convertToResponseList(expenses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByStatusPaginated(Long userId, ExpenseStatus status, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByUserIdAndStatus(userId, status, pageable);
        return expenses.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        return convertToResponseList(expenses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByDateRangePaginated(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate, pageable);
        return expenses.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByAmountRange(Long userId, BigDecimal minAmount, BigDecimal maxAmount) {
        List<Expense> expenses = expenseRepository.findByUserIdAndAmountRange(userId, minAmount, maxAmount);
        return convertToResponseList(expenses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> searchExpenses(Long userId, String keyword, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.searchByKeyword(userId, keyword, pageable);
        return expenses.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getTeamExpenses(Long teamId) {
        List<Expense> expenses = expenseRepository.findByTeamId(teamId);
        return convertToResponseList(expenses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getTeamExpensesPaginated(Long teamId, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByTeamId(teamId, pageable);
        return expenses.map(this::convertToResponse);
    }
    
    @Override
    public ExpenseResponse approveExpense(Long expenseId, Long approvedBy) {
        logger.info("Approving expense {} by user: {}", expenseId, approvedBy);
        
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new BadRequestException("Only pending expenses can be approved");
        }
        
        expenseRepository.approveExpense(expenseId, approvedBy, LocalDateTime.now());
        
        Expense updatedExpense = expenseRepository.findById(expenseId).orElseThrow();
        logger.info("Expense approved successfully: {}", expenseId);
        
        return convertToResponse(updatedExpense);
    }
    
    @Override
    public ExpenseResponse rejectExpense(Long expenseId, Long rejectedBy, String rejectionReason) {
        logger.info("Rejecting expense {} by user: {}", expenseId, rejectedBy);
        
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new BadRequestException("Only pending expenses can be rejected");
        }
        
        expenseRepository.rejectExpense(expenseId, rejectedBy, LocalDateTime.now(), rejectionReason);
        
        Expense updatedExpense = expenseRepository.findById(expenseId).orElseThrow();
        logger.info("Expense rejected successfully: {}", expenseId);
        
        return convertToResponse(updatedExpense);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getPendingExpensesForApproval() {
        List<Expense> expenses = expenseRepository.findPendingExpensesForApproval();
        return convertToResponseList(expenses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getReimbursableExpenses(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdAndIsReimbursableTrue(userId);
        return convertToResponseList(expenses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getUnreimbursedExpenses(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdAndIsReimbursableTrueAndReimbursedFalse(userId);
        return convertToResponseList(expenses);
    }
    
    @Override
    public ExpenseResponse markAsReimbursed(Long expenseId, Long userId) {
        logger.info("Marking expense {} as reimbursed by user: {}", expenseId, userId);
        
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        
        validateExpenseAccess(expenseId, userId);
        
        if (!expense.getIsReimbursable()) {
            throw new BadRequestException("This expense is not marked as reimbursable");
        }
        
        expense.setReimbursed(true);
        expense.setReimbursedDate(LocalDate.now());
        Expense updatedExpense = expenseRepository.save(expense);
        
        logger.info("Expense marked as reimbursed: {}", expenseId);
        return convertToResponse(updatedExpense);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenseAmount(Long userId) {
        BigDecimal total = expenseRepository.getTotalAmountByUser(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenseAmountByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = expenseRepository.getTotalAmountByUserAndDateRange(userId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenseAmountByCategory(Long userId, Long categoryId) {
        BigDecimal total = expenseRepository.getTotalAmountByUserAndCategory(userId, categoryId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getExpenseCount(Long userId) {
        return expenseRepository.countByUserId(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getExpenseCountByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.countByUserIdAndDateRange(userId, startDate, endDate);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlySummary(Long userId) {
        List<Object[]> results = expenseRepository.getMonthlySummary(userId);
        return results.stream().map(result -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("year", result[0]);
            summary.put("month", result[1]);
            summary.put("totalAmount", result[2]);
            summary.put("expenseCount", result[3]);
            return summary;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategorySummary(Long userId) {
        List<Object[]> results = expenseRepository.getCategorySummary(userId);
        return results.stream().map(result -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("categoryName", result[0]);
            summary.put("totalAmount", result[1]);
            summary.put("expenseCount", result[2]);
            return summary;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getRecentExpenses(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Expense> expenses = expenseRepository.findRecentExpenses(userId, pageable);
        return convertToResponseList(expenses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getRecurringExpenses(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdOrderByExpenseDateDesc(userId)
                .stream()
                .filter(expense -> expense.getIsRecurring())
                .collect(Collectors.toList());
        return convertToResponseList(expenses);
    }
    
    @Override
    public void createRecurringExpense(ExpenseRequest expenseRequest, Long userId) {
        // Implementation for creating recurring expenses
        // This would typically involve scheduling logic
        logger.info("Creating recurring expense for user: {}", userId);
        createExpense(expenseRequest, userId);
    }
    
    @Override
    public void validateExpenseAccess(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        
        if (!expense.getUserId().equals(userId)) {
            throw new ForbiddenException("You don't have access to this expense");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canUserEditExpense(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findById(expenseId).orElse(null);
        if (expense == null || !expense.getUserId().equals(userId)) {
            return false;
        }
        return expense.canBeEdited();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canUserDeleteExpense(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findById(expenseId).orElse(null);
        if (expense == null || !expense.getUserId().equals(userId)) {
            return false;
        }
        return expense.canBeDeleted();
    }
    
    @Override
    public void deleteMultipleExpenses(List<Long> expenseIds, Long userId) {
        logger.info("Deleting {} expenses for user: {}", expenseIds.size(), userId);
        
        for (Long expenseId : expenseIds) {
            validateExpenseAccess(expenseId, userId);
            if (canUserDeleteExpense(expenseId, userId)) {
                expenseRepository.deleteById(expenseId);
            }
        }
        
        logger.info("Bulk delete completed for user: {}", userId);
    }
    
    @Override
    public void updateMultipleExpenseStatus(List<Long> expenseIds, ExpenseStatus status, Long userId) {
        logger.info("Updating status for {} expenses to {} for user: {}", expenseIds.size(), status, userId);
        
        for (Long expenseId : expenseIds) {
            validateExpenseAccess(expenseId, userId);
            expenseRepository.updateStatus(expenseId, status);
        }
        
        logger.info("Bulk status update completed for user: {}", userId);
    }
    
    @Override
    public byte[] exportExpensesToCsv(Long userId, LocalDate startDate, LocalDate endDate) {
        logger.info("Exporting expenses to CSV for user: {} from {} to {}", userId, startDate, endDate);
        
        try {
            // Get expenses for the date range
            List<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate);
            
            if (expenses.isEmpty()) {
                logger.warn("No expenses found for user: {} in date range {} to {}", userId, startDate, endDate);
            }
            
            // Create CSV content
            StringBuilder csvBuilder = new StringBuilder();
            
            // Add UTF-8 BOM for proper Excel compatibility
            csvBuilder.append('\ufeff');
            
            // Add CSV header
            csvBuilder.append("Date,Title,Description,Amount,Currency,Category,Status,Payment Method,Merchant,Location,Notes,Reference Number,Created Date\n");
            
            // Add expense data
            for (Expense expense : expenses) {
                csvBuilder.append(formatCsvValue(expense.getExpenseDate() != null ? expense.getExpenseDate().toString() : ""))
                         .append(",")
                         .append(formatCsvValue(expense.getTitle()))
                         .append(",")
                         .append(formatCsvValue(expense.getDescription()))
                         .append(",")
                         .append(formatCsvValue(expense.getAmount() != null ? expense.getAmount().toString() : "0.00"))
                         .append(",")
                         .append(formatCsvValue(expense.getCurrencyCode()))
                         .append(",")
                         .append(formatCsvValue(getCategoryName(expense.getCategoryId())))
                         .append(",")
                         .append(formatCsvValue(expense.getStatus() != null ? expense.getStatus().getDisplayName() : ""))
                         .append(",")
                         .append(formatCsvValue(expense.getPaymentMethod() != null ? expense.getPaymentMethod().name() : ""))
                         .append(",")
                         .append(formatCsvValue(expense.getMerchantName()))
                         .append(",")
                         .append(formatCsvValue(expense.getLocation()))
                         .append(",")
                         .append(formatCsvValue(expense.getNotes()))
                         .append(",")
                         .append(formatCsvValue(expense.getReferenceNumber()))
                         .append(",")
                         .append(formatCsvValue(expense.getCreatedAt() != null ? 
                                 expense.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : ""))
                         .append("\n");
            }
            
            // Add summary row
            csvBuilder.append("\n");
            csvBuilder.append("SUMMARY,,,,,,,,,,,,\n");
            
            BigDecimal totalAmount = expenses.stream()
                    .map(Expense::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            csvBuilder.append("Total Records: ").append(expenses.size()).append(",,,,,,,,,,,,\n");
            csvBuilder.append("Total Amount: ").append(totalAmount).append(",,,,,,,,,,,,\n");
            csvBuilder.append("Export Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                     .append(",,,,,,,,,,,,\n");
            
            byte[] csvBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
            
            logger.info("Successfully exported {} expenses to CSV for user: {}, total size: {} bytes", 
                    expenses.size(), userId, csvBytes.length);
            
            return csvBytes;
            
        } catch (Exception e) {
            logger.error("Error exporting expenses to CSV for user: {}", userId, e);
            throw new RuntimeException("Failed to export expenses to CSV", e);
        }
    }

    @Override
    public byte[] exportExpensesToPdf(Long userId, LocalDate startDate, LocalDate endDate) {
        logger.info("Exporting expenses to PDF for user: {} from {} to {}", userId, startDate, endDate);
        
        try {
            // Get expenses for the date range
            List<Expense> expenses = expenseRepository.findByUserIdAndDateRange(userId, startDate, endDate);
            
            if (expenses.isEmpty()) {
                logger.warn("No expenses found for user: {} in date range {} to {}", userId, startDate, endDate);
            }
            
            // Get user information
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
            
            // Create PDF document
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate()); // Landscape for better table display
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);
            
            // Document header
            Paragraph title = new Paragraph("Expense Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Report details
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(20);
            
            addCellToTable(headerTable, "User:", headerFont, Element.ALIGN_LEFT);
            addCellToTable(headerTable, user.getFullName() + " (" + user.getEmail() + ")", normalFont, Element.ALIGN_LEFT);
            addCellToTable(headerTable, "Period:", headerFont, Element.ALIGN_LEFT);
            addCellToTable(headerTable, startDate + " to " + endDate, normalFont, Element.ALIGN_LEFT);
            addCellToTable(headerTable, "Generated:", headerFont, Element.ALIGN_LEFT);
            addCellToTable(headerTable, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), normalFont, Element.ALIGN_LEFT);
            addCellToTable(headerTable, "Total Records:", headerFont, Element.ALIGN_LEFT);
            addCellToTable(headerTable, String.valueOf(expenses.size()), normalFont, Element.ALIGN_LEFT);
            
            document.add(headerTable);
            
            if (expenses.isEmpty()) {
                Paragraph noData = new Paragraph("No expenses found for the specified period.", normalFont);
                noData.setAlignment(Element.ALIGN_CENTER);
                document.add(noData);
            } else {
                // Create expenses table
                PdfPTable expenseTable = new PdfPTable(9);
                expenseTable.setWidthPercentage(100);
                expenseTable.setSpacingBefore(10);
                
                // Set column widths
                float[] columnWidths = {10f, 15f, 10f, 8f, 12f, 10f, 8f, 12f, 15f};
                expenseTable.setWidths(columnWidths);
                
                // Add table headers
                String[] headers = {"Date", "Title", "Amount", "Currency", "Category", "Status", "Payment", "Merchant", "Description"};
                for (String header : headers) {
                    PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                    headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    headerCell.setPadding(5);
                    expenseTable.addCell(headerCell);
                }
                
                // Add expense data
                BigDecimal totalAmount = BigDecimal.ZERO;
                
                for (Expense expense : expenses) {
                    // Date
                    addCellToTable(expenseTable, 
                            expense.getExpenseDate() != null ? expense.getExpenseDate().toString() : "", 
                            smallFont, Element.ALIGN_CENTER);
                    
                    // Title
                    addCellToTable(expenseTable, 
                            truncateText(expense.getTitle(), 25), 
                            smallFont, Element.ALIGN_LEFT);
                    
                    // Amount
                    String amountText = expense.getAmount() != null ? 
                            NumberFormat.getCurrencyInstance(Locale.US).format(expense.getAmount()) : "$0.00";
                    addCellToTable(expenseTable, amountText, smallFont, Element.ALIGN_RIGHT);
                    
                    // Currency
                    addCellToTable(expenseTable, expense.getCurrencyCode(), smallFont, Element.ALIGN_CENTER);
                    
                    // Category
                    addCellToTable(expenseTable, 
                            truncateText(getCategoryName(expense.getCategoryId()), 15), 
                            smallFont, Element.ALIGN_LEFT);
                    
                    // Status
                    addCellToTable(expenseTable, 
                            expense.getStatus() != null ? expense.getStatus().getDisplayName() : "", 
                            smallFont, Element.ALIGN_CENTER);
                    
                    // Payment Method
                    addCellToTable(expenseTable, 
                            expense.getPaymentMethod() != null ? expense.getPaymentMethod().name() : "", 
                            smallFont, Element.ALIGN_CENTER);
                    
                    // Merchant
                    addCellToTable(expenseTable, 
                            truncateText(expense.getMerchantName(), 15), 
                            smallFont, Element.ALIGN_LEFT);
                    
                    // Description
                    addCellToTable(expenseTable, 
                            truncateText(expense.getDescription(), 20), 
                            smallFont, Element.ALIGN_LEFT);
                    
                    // Add to total
                    if (expense.getAmount() != null) {
                        totalAmount = totalAmount.add(expense.getAmount());
                    }
                }
                
                document.add(expenseTable);
                
                // Add summary section
                PdfPTable summaryTable = new PdfPTable(2);
                summaryTable.setWidthPercentage(50);
                summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                summaryTable.setSpacingBefore(20);
                
                addCellToTable(summaryTable, "Total Amount:", headerFont, Element.ALIGN_RIGHT);
                addCellToTable(summaryTable, NumberFormat.getCurrencyInstance(Locale.US).format(totalAmount), 
                        headerFont, Element.ALIGN_RIGHT);
                
                // Calculate average
                BigDecimal averageAmount = expenses.size() > 0 ? 
                        totalAmount.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP) : 
                        BigDecimal.ZERO;
                
                addCellToTable(summaryTable, "Average Amount:", normalFont, Element.ALIGN_RIGHT);
                addCellToTable(summaryTable, NumberFormat.getCurrencyInstance(Locale.US).format(averageAmount), 
                        normalFont, Element.ALIGN_RIGHT);
                
                document.add(summaryTable);
                
                // Add category breakdown if there are expenses
                addCategoryBreakdown(document, expenses, headerFont, normalFont);
            }
            
            // Add footer
            Paragraph footer = new Paragraph("Generated by Trackify Expense Management System", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);
            
            document.close();
            writer.close();
            
            byte[] pdfBytes = outputStream.toByteArray();
            outputStream.close();
            
            logger.info("Successfully exported {} expenses to PDF for user: {}, total size: {} bytes", 
                    expenses.size(), userId, pdfBytes.length);
            
            return pdfBytes;
            
        } catch (DocumentException | IOException e)  {
            logger.error("Error creating PDF document for user: {}", userId, e);
            throw new RuntimeException("Failed to export expenses to PDF", e);
        } catch (Exception e) {
            logger.error("Error exporting expenses to PDF for user: {}", userId, e);
            throw new RuntimeException("Failed to export expenses to PDF", e);
        }
    }

    // Helper methods

    private String formatCsvValue(String value) {
        if (value == null) {
            return "";
        }
        
        // Escape quotes and wrap in quotes if contains comma, newline, or quote
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "Unknown";
        }
        
        try {
            return categoryRepository.findById(categoryId)
                    .map(category -> category.getName())
                    .orElse("Unknown");
        } catch (Exception e) {
            logger.warn("Error fetching category name for ID: {}", categoryId, e);
            return "Unknown";
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }

    private void addCellToTable(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(3);
        cell.setBorder(Rectangle.BOX);
        table.addCell(cell);
    }

    private void addCategoryBreakdown(Document document, List<Expense> expenses, Font headerFont, Font normalFont) 
            throws DocumentException {
        
        // Group expenses by category
        Map<String, List<Expense>> categoryGroups = expenses.stream()
                .collect(Collectors.groupingBy(expense -> getCategoryName(expense.getCategoryId())));
        
        if (categoryGroups.size() <= 1) {
            return; // Skip if only one or no categories
        }
        
        // Add category breakdown section
        Paragraph categoryHeader = new Paragraph("Category Breakdown", headerFont);
        categoryHeader.setSpacingBefore(20);
        categoryHeader.setSpacingAfter(10);
        document.add(categoryHeader);
        
        PdfPTable categoryTable = new PdfPTable(3);
        categoryTable.setWidthPercentage(70);
        categoryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        
        // Headers
        addCellToTable(categoryTable, "Category", headerFont, Element.ALIGN_LEFT);
        addCellToTable(categoryTable, "Count", headerFont, Element.ALIGN_CENTER);
        addCellToTable(categoryTable, "Total Amount", headerFont, Element.ALIGN_RIGHT);
        
        // Sort categories by total amount (descending)
        List<Map.Entry<String, List<Expense>>> sortedCategories = categoryGroups.entrySet().stream()
                .sorted((e1, e2) -> {
                    BigDecimal total1 = e1.getValue().stream()
                            .map(Expense::getAmount)
                            .filter(amount -> amount != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal total2 = e2.getValue().stream()
                            .map(Expense::getAmount)
                            .filter(amount -> amount != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return total2.compareTo(total1);
                })
                .collect(Collectors.toList());
        
        for (Map.Entry<String, List<Expense>> entry : sortedCategories) {
            String categoryName = entry.getKey();
            List<Expense> categoryExpenses = entry.getValue();
            
            BigDecimal categoryTotal = categoryExpenses.stream()
                    .map(Expense::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            addCellToTable(categoryTable, categoryName, normalFont, Element.ALIGN_LEFT);
            addCellToTable(categoryTable, String.valueOf(categoryExpenses.size()), normalFont, Element.ALIGN_CENTER);
            addCellToTable(categoryTable, NumberFormat.getCurrencyInstance(Locale.US).format(categoryTotal), 
                    normalFont, Element.ALIGN_RIGHT);
        }
        
        document.add(categoryTable);
    }
    
    // Private helper methods
    
    private Expense convertToEntity(ExpenseRequest request, Long userId) {
        Expense expense = new Expense();
        expense.setTitle(request.getTitle().trim());
        expense.setDescription(StringUtils.hasText(request.getDescription()) ? 
                              request.getDescription().trim() : null);
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setStatus(request.getStatus() != null ? request.getStatus() : ExpenseStatus.PENDING);
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setMerchantName(StringUtils.hasText(request.getMerchantName()) ? 
                               request.getMerchantName().trim() : null);
        expense.setLocation(StringUtils.hasText(request.getLocation()) ? 
                           request.getLocation().trim() : null);
        expense.setTags(StringUtils.hasText(request.getTags()) ? 
                       request.getTags().trim() : null);
        expense.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        expense.setCurrencyCode(StringUtils.hasText(request.getCurrencyCode()) ? 
                               request.getCurrencyCode() : "USD");
        expense.setExchangeRate(request.getExchangeRate());
        expense.setOriginalAmount(request.getOriginalAmount());
        expense.setOriginalCurrency(request.getOriginalCurrency());
        expense.setNotes(StringUtils.hasText(request.getNotes()) ? 
                        request.getNotes().trim() : null);
        expense.setReferenceNumber(StringUtils.hasText(request.getReferenceNumber()) ? 
                                  request.getReferenceNumber().trim() : null);
        expense.setIsBusinessExpense(request.getIsBusinessExpense() != null ? 
                                   request.getIsBusinessExpense() : false);
        expense.setIsReimbursable(request.getIsReimbursable() != null ? 
                                request.getIsReimbursable() : false);
        expense.setReimbursed(false);
        expense.setUserId(userId);
        expense.setCategoryId(request.getCategoryId());
        expense.setTeamId(request.getTeamId());
        expense.setProjectId(request.getProjectId());
        
        return expense;
    }
    
    private void updateExpenseFromRequest(Expense expense, ExpenseRequest request) {
        expense.setTitle(request.getTitle().trim());
        expense.setDescription(StringUtils.hasText(request.getDescription()) ? 
                              request.getDescription().trim() : null);
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setMerchantName(StringUtils.hasText(request.getMerchantName()) ? 
                               request.getMerchantName().trim() : null);
        expense.setLocation(StringUtils.hasText(request.getLocation()) ? 
                           request.getLocation().trim() : null);
        expense.setTags(StringUtils.hasText(request.getTags()) ? 
                       request.getTags().trim() : null);
        expense.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        expense.setCurrencyCode(StringUtils.hasText(request.getCurrencyCode()) ? 
                               request.getCurrencyCode() : "USD");
        expense.setExchangeRate(request.getExchangeRate());
        expense.setOriginalAmount(request.getOriginalAmount());
        expense.setOriginalCurrency(request.getOriginalCurrency());
        expense.setNotes(StringUtils.hasText(request.getNotes()) ? 
                        request.getNotes().trim() : null);
        expense.setReferenceNumber(StringUtils.hasText(request.getReferenceNumber()) ? 
                                  request.getReferenceNumber().trim() : null);
        expense.setIsBusinessExpense(request.getIsBusinessExpense() != null ? 
                                   request.getIsBusinessExpense() : false);
        expense.setIsReimbursable(request.getIsReimbursable() != null ? 
                                request.getIsReimbursable() : false);
        expense.setCategoryId(request.getCategoryId());
        expense.setTeamId(request.getTeamId());
        expense.setProjectId(request.getProjectId());
    }
    
    private ExpenseResponse convertToResponse(Expense expense) {
        ExpenseResponse response = modelMapper.map(expense, ExpenseResponse.class);
        
        // Get category information
        categoryRepository.findById(expense.getCategoryId()).ifPresent(category -> {
            response.setCategoryName(category.getName());
            response.setCategoryColor(category.getColor());
            response.setCategoryIcon(category.getIcon());
        });
        
        // Get receipts
        List<Receipt> receipts = receiptRepository.findByExpenseIdOrderByCreatedAtAsc(expense.getId());
        List<ReceiptResponse> receiptResponses = receipts.stream()
                .map(receipt -> {
                    ReceiptResponse receiptResponse = modelMapper.map(receipt, ReceiptResponse.class);
                    receiptResponse.setDisplaySize(receipt.getDisplaySize());
                    receiptResponse.setFileExtension(receipt.getFileExtension());
                    receiptResponse.setCanDownload(true);
                    receiptResponse.setCanDelete(expense.canBeEdited());
                    return receiptResponse;
                })
                .collect(Collectors.toList());
        response.setReceipts(receiptResponses);
        
        // Set utility fields
        response.setCanEdit(expense.canBeEdited());
        response.setCanDelete(expense.canBeDeleted());
        response.setCanApprove(expense.getStatus() == ExpenseStatus.PENDING && expense.getTeamId() != null);
        response.setCanReject(expense.getStatus() == ExpenseStatus.PENDING && expense.getTeamId() != null);
        response.setStatusDisplayName(getStatusDisplayName(expense.getStatus()));
        response.setFormattedAmount(formatAmount(expense.getAmount(), expense.getCurrencyCode()));
        response.setReceiptCount(receipts.size());
        
        return response;
    }
    
    private List<ExpenseResponse> convertToResponseList(List<Expense> expenses) {
        return expenses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    private void validateCategoryAccess(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        
        // Check if user has access to this category (personal or team category)
        if (!category.getCreatedBy().equals(userId) && !category.getIsSystem()) {
            throw new ForbiddenException("You don't have access to this category");
        }
    }
    
    private String getStatusDisplayName(ExpenseStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case DRAFT -> "Draft";
            case SUBMITTED -> "Submitted";
		default -> throw new IllegalArgumentException("Unexpected value: " + status);
        };
    }
    
    private String formatAmount(BigDecimal amount, String currencyCode) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        if ("USD".equals(currencyCode)) {
            formatter.setCurrency(Currency.getInstance("USD"));
        } else if ("EUR".equals(currencyCode)) {
            formatter.setCurrency(Currency.getInstance("EUR"));
        }
        // Add more currencies as needed
        return formatter.format(amount);
    }
}