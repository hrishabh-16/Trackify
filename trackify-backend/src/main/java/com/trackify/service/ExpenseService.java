package com.trackify.service;

import com.trackify.dto.request.ExpenseRequest;
import com.trackify.dto.response.ExpenseResponse;
import com.trackify.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExpenseService {
    
    // Basic CRUD operations
    ExpenseResponse createExpense(ExpenseRequest expenseRequest, Long userId);
    ExpenseResponse getExpenseById(Long expenseId, Long userId);
    ExpenseResponse updateExpense(Long expenseId, ExpenseRequest expenseRequest, Long userId);
    void deleteExpense(Long expenseId, Long userId);
    
    // User expenses
    List<ExpenseResponse> getUserExpenses(Long userId);
    Page<ExpenseResponse> getUserExpensesPaginated(Long userId, Pageable pageable);
    
    // Category expenses
    List<ExpenseResponse> getExpensesByCategory(Long categoryId, Long userId);
    Page<ExpenseResponse> getExpensesByCategoryPaginated(Long categoryId, Long userId, Pageable pageable);
    
    // Status-based queries
    List<ExpenseResponse> getExpensesByStatus(Long userId, ExpenseStatus status);
    Page<ExpenseResponse> getExpensesByStatusPaginated(Long userId, ExpenseStatus status, Pageable pageable);
    
    // Date range queries
    List<ExpenseResponse> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    Page<ExpenseResponse> getExpensesByDateRangePaginated(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    // Amount range queries
    List<ExpenseResponse> getExpensesByAmountRange(Long userId, BigDecimal minAmount, BigDecimal maxAmount);
    
    // Search functionality
    Page<ExpenseResponse> searchExpenses(Long userId, String keyword, Pageable pageable);
    
    // Team expenses
    List<ExpenseResponse> getTeamExpenses(Long teamId);
    Page<ExpenseResponse> getTeamExpensesPaginated(Long teamId, Pageable pageable);
    
    // Approval workflow
    ExpenseResponse approveExpense(Long expenseId, Long approvedBy);
    ExpenseResponse rejectExpense(Long expenseId, Long rejectedBy, String rejectionReason);
    List<ExpenseResponse> getPendingExpensesForApproval();
    
    // Reimbursement
    List<ExpenseResponse> getReimbursableExpenses(Long userId);
    List<ExpenseResponse> getUnreimbursedExpenses(Long userId);
    ExpenseResponse markAsReimbursed(Long expenseId, Long userId);
    
    // Statistics and summaries
    BigDecimal getTotalExpenseAmount(Long userId);
    BigDecimal getTotalExpenseAmountByDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    BigDecimal getTotalExpenseAmountByCategory(Long userId, Long categoryId);
    long getExpenseCount(Long userId);
    long getExpenseCountByDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    
    // Monthly and category summaries
    List<Map<String, Object>> getMonthlySummary(Long userId);
    List<Map<String, Object>> getCategorySummary(Long userId);
    
    // Recent expenses
    List<ExpenseResponse> getRecentExpenses(Long userId, int limit);
    
    // Recurring expenses
    List<ExpenseResponse> getRecurringExpenses(Long userId);
    void createRecurringExpense(ExpenseRequest expenseRequest, Long userId);
    
    // Validation and utility
    void validateExpenseAccess(Long expenseId, Long userId);
    boolean canUserEditExpense(Long expenseId, Long userId);
    boolean canUserDeleteExpense(Long expenseId, Long userId);
    
    // Bulk operations
    void deleteMultipleExpenses(List<Long> expenseIds, Long userId);
    void updateMultipleExpenseStatus(List<Long> expenseIds, ExpenseStatus status, Long userId);
    
    // Export functionality
    byte[] exportExpensesToCsv(Long userId, LocalDate startDate, LocalDate endDate);
    byte[] exportExpensesToPdf(Long userId, LocalDate startDate, LocalDate endDate);
}