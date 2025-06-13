package com.trackify.service;

import com.trackify.dto.response.DashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DashboardService {
    
    // Main dashboard data
    DashboardResponse getDashboardData(String username);
    DashboardResponse getTeamDashboardData(Long teamId, String username);
    
    // Summary statistics
    DashboardResponse.ExpenseSummary getExpenseSummary(String username, LocalDate startDate, LocalDate endDate);
    DashboardResponse.BudgetSummary getBudgetSummary(String username, LocalDate startDate, LocalDate endDate);
    DashboardResponse.CategorySummary getCategorySummary(String username, LocalDate startDate, LocalDate endDate);
    
    // Chart data
    List<DashboardResponse.MonthlyExpense> getMonthlyExpenseData(String username, int months);
    List<DashboardResponse.CategoryExpense> getCategoryExpenseData(String username, LocalDate startDate, LocalDate endDate);
    List<DashboardResponse.DailyExpense> getDailyExpenseData(String username, LocalDate startDate, LocalDate endDate);
    
    // Trends and analytics
    DashboardResponse.ExpenseTrend getExpenseTrend(String username, int months);
    List<DashboardResponse.TopCategory> getTopCategories(String username, LocalDate startDate, LocalDate endDate, int limit);
    List<DashboardResponse.RecentExpense> getRecentExpenses(String username, int limit);
    
    // Budget tracking
    List<DashboardResponse.BudgetStatus> getBudgetStatusList(String username);
    DashboardResponse.BudgetAlert getBudgetAlert(String username);
    
    // Team-specific data
    DashboardResponse.TeamSummary getTeamSummary(Long teamId, String username);
    List<DashboardResponse.TeamMemberExpense> getTeamMemberExpenses(Long teamId, LocalDate startDate, LocalDate endDate);
    
    // Real-time updates
    void updateDashboardCache(String username);
    void invalidateDashboardCache(String username);
    
    // Export functionality
    byte[] exportDashboardData(String username, String format, LocalDate startDate, LocalDate endDate);
    
    // Comparison data
    DashboardResponse.ComparisonData getMonthComparison(String username, LocalDate currentMonth, LocalDate previousMonth);
    DashboardResponse.YearlyComparison getYearlyComparison(String username, int currentYear, int previousYear);
}