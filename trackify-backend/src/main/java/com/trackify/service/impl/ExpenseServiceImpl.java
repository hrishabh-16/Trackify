package com.trackify.service.impl;

import com.trackify.dto.request.ExpenseRequest;
import com.trackify.dto.response.ExpenseResponse;
import com.trackify.dto.response.ReceiptResponse;
import com.trackify.entity.Category;
import com.trackify.entity.Expense;
import com.trackify.entity.Receipt;
import com.trackify.enums.ExpenseStatus;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ForbiddenException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.CategoryRepository;
import com.trackify.repository.ExpenseRepository;
import com.trackify.repository.ReceiptRepository;
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

import java.math.BigDecimal;
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
        // Implementation for CSV export
        logger.info("Exporting expenses to CSV for user: {} from {} to {}", userId, startDate, endDate);
        // This would generate CSV content and return as byte array
        return new byte[0]; // Placeholder
    }
    
    @Override
    public byte[] exportExpensesToPdf(Long userId, LocalDate startDate, LocalDate endDate) {
        // Implementation for PDF export
        logger.info("Exporting expenses to PDF for user: {} from {} to {}", userId, startDate, endDate);
        // This would generate PDF content and return as byte array
        return new byte[0]; // Placeholder
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