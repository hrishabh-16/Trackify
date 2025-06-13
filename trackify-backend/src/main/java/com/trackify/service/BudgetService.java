package com.trackify.service;

import com.trackify.dto.request.BudgetRequest;
import com.trackify.dto.response.BudgetResponse;
import com.trackify.entity.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BudgetService {
    
    // CRUD operations
    BudgetResponse createBudget(BudgetRequest budgetRequest, String username);
    BudgetResponse updateBudget(Long budgetId, BudgetRequest.UpdateBudgetRequest updateRequest, String username);
    BudgetResponse getBudgetById(Long budgetId, String username);
    List<BudgetResponse> getUserBudgets(String username);
    Page<BudgetResponse> getUserBudgets(String username, Pageable pageable);
    void deleteBudget(Long budgetId, String username);
    
    // Budget status operations
    BudgetResponse activateBudget(Long budgetId, String username);
    BudgetResponse deactivateBudget(Long budgetId, String username);
    
    // Budget filtering and search
    List<BudgetResponse> getActiveBudgets(String username);
    List<BudgetResponse> getBudgetsByCategory(Long categoryId, String username);
    List<BudgetResponse> getBudgetsByTeam(Long teamId, String username);
    List<BudgetResponse> getBudgetsByDateRange(String username, LocalDate startDate, LocalDate endDate);
    List<BudgetResponse> searchBudgets(String username, String keyword);
    
    // Budget analytics and reporting
    BudgetResponse.BudgetAnalytics getBudgetAnalytics(String username);
    List<BudgetResponse> getBudgetsNearThreshold(String username);
    List<BudgetResponse> getOverBudgets(String username);
    List<BudgetResponse> getExpiredBudgets(String username);
    
    // Budget alerts and notifications
    void checkBudgetAlerts(String username);
    void sendBudgetAlert(Long budgetId, String username);
    BudgetResponse.BudgetAlert getBudgetAlert(Long budgetId, String username);
    
    // Recurring budget operations
    BudgetResponse createRecurringBudget(BudgetRequest.RecurringBudgetRequest request, String username);
    void processRecurringBudgets();
    List<BudgetResponse> getRecurringBudgets(String username);
    
    // Budget transfers and adjustments
    void transferBudgetAmount(BudgetRequest.BudgetTransferRequest request, String username);
    BudgetResponse adjustBudgetAmount(Long budgetId, BigDecimal newAmount, String username);
    
    // Expense tracking integration
    void updateBudgetSpending(Long budgetId, BigDecimal expenseAmount);
    void removeBudgetSpending(Long budgetId, BigDecimal expenseAmount);
    void recalculateBudgetSpending(Long budgetId);
    
    // Team budget operations
    List<BudgetResponse> getTeamBudgets(Long teamId, String username);
    BudgetResponse createTeamBudget(Long teamId, BudgetRequest budgetRequest, String username);
    
    // Validation and utility methods
    boolean validateBudgetOverlap(String username, Long categoryId, LocalDate startDate, LocalDate endDate, Long excludeId);
    void validateBudgetAccess(Long budgetId, String username);
    Budget getBudgetEntity(Long budgetId);
    
    // Export and import
    byte[] exportBudgets(String username, String format, LocalDate startDate, LocalDate endDate);
    List<BudgetResponse> importBudgets(byte[] data, String username);
    
    // Dashboard integration
    List<BudgetResponse.BudgetSummary> getBudgetSummariesForDashboard(String username);
    BudgetResponse.BudgetAnalytics getBudgetAnalyticsForDashboard(String username, LocalDate startDate, LocalDate endDate);
}