package com.trackify.service.impl;

import com.trackify.dto.response.DashboardResponse;
import com.trackify.entity.Budget;
import com.trackify.entity.Expense;
import com.trackify.entity.TeamMember;
import com.trackify.entity.User;
import com.trackify.enums.ExpenseStatus;
import com.trackify.repository.BudgetRepository;
import com.trackify.repository.ExpenseRepository;
import com.trackify.repository.TeamMemberRepository;
import com.trackify.repository.UserRepository;
import com.trackify.service.DashboardService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Override
    @Cacheable(value = "dashboardData", key = "#username")
    public DashboardResponse getDashboardData(String username) {
        try {
            logger.info("Generating dashboard data for user: {}", username);
            
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            LocalDate now = LocalDate.now();
            LocalDate startOfMonth = now.withDayOfMonth(1);
            LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
            
            // Get summary data
            DashboardResponse.ExpenseSummary expenseSummary = getExpenseSummary(username, startOfMonth, endOfMonth);
            DashboardResponse.BudgetSummary budgetSummary = getBudgetSummary(username, startOfMonth, endOfMonth);
            DashboardResponse.CategorySummary categorySummary = getCategorySummary(username, startOfMonth, endOfMonth);
            
            // Get chart data
            List<DashboardResponse.MonthlyExpense> monthlyData = getMonthlyExpenseData(username, 12);
            List<DashboardResponse.CategoryExpense> categoryData = getCategoryExpenseData(username, startOfMonth, endOfMonth);
            List<DashboardResponse.DailyExpense> dailyData = getDailyExpenseData(username, startOfMonth, endOfMonth);
            
            // Get trends and analytics
            DashboardResponse.ExpenseTrend expenseTrend = getExpenseTrend(username, 12);
            List<DashboardResponse.TopCategory> topCategories = getTopCategories(username, startOfMonth, endOfMonth, 5);
            List<DashboardResponse.RecentExpense> recentExpenses = getRecentExpenses(username, 10);
            
            // Get budget data
            List<DashboardResponse.BudgetStatus> budgetStatusList = getBudgetStatusList(username);
            DashboardResponse.BudgetAlert budgetAlert = getBudgetAlert(username);
            
            // Build dashboard response
            DashboardResponse dashboard = new DashboardResponse();
            dashboard.setExpenseSummary(expenseSummary);
            dashboard.setBudgetSummary(budgetSummary);
            dashboard.setCategorySummary(categorySummary);
            dashboard.setMonthlyExpenses(monthlyData);
            dashboard.setCategoryExpenses(categoryData);
            dashboard.setDailyExpenses(dailyData);
            dashboard.setExpenseTrend(expenseTrend);
            dashboard.setTopCategories(topCategories);
            dashboard.setRecentExpenses(recentExpenses);
            dashboard.setBudgetStatusList(budgetStatusList);
            dashboard.setBudgetAlert(budgetAlert);
            dashboard.setGeneratedAt(LocalDateTime.now());
            
            logger.info("Successfully generated dashboard data for user: {}", username);
            return dashboard;
            
        } catch (Exception e) {
            logger.error("Error generating dashboard data for user: {}", username, e);
            throw new RuntimeException("Failed to generate dashboard data", e);
        }
    }

    @Override
    public DashboardResponse getTeamDashboardData(Long teamId, String username) {
        try {
            logger.info("Generating team dashboard data for team: {} by user: {}", teamId, username);
            
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            // Verify user is part of the team
            TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                    .orElseThrow(() -> new RuntimeException("User not authorized for this team"));
            
            LocalDate now = LocalDate.now();
            LocalDate startOfMonth = now.withDayOfMonth(1);
            LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
            
            // Get team-specific data
            DashboardResponse.TeamSummary teamSummary = getTeamSummary(teamId, username);
            List<DashboardResponse.TeamMemberExpense> teamMemberExpenses = getTeamMemberExpenses(teamId, startOfMonth, endOfMonth);
            
            // Get regular dashboard data filtered for team
            DashboardResponse dashboard = getDashboardData(username);
            dashboard.setTeamSummary(teamSummary);
            dashboard.setTeamMemberExpenses(teamMemberExpenses);
            
            return dashboard;
            
        } catch (Exception e) {
            logger.error("Error generating team dashboard data for team: {} by user: {}", teamId, username, e);
            throw new RuntimeException("Failed to generate team dashboard data", e);
        }
    }

    @Override
    public DashboardResponse.ExpenseSummary getExpenseSummary(String username, LocalDate startDate, LocalDate endDate) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            
            // Get expense statistics
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    user.getId(), startDateTime, endDateTime);
            
            BigDecimal totalAmount = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long totalCount = expenses.size();
            
            long approvedCount = expenses.stream()
                    .mapToLong(e -> ExpenseStatus.APPROVED.equals(e.getStatus()) ? 1 : 0)
                    .sum();
            
            long pendingCount = expenses.stream()
                    .mapToLong(e -> ExpenseStatus.PENDING.equals(e.getStatus()) ? 1 : 0)
                    .sum();
            
            long rejectedCount = expenses.stream()
                    .mapToLong(e -> ExpenseStatus.REJECTED.equals(e.getStatus()) ? 1 : 0)
                    .sum();
            
            BigDecimal averageAmount = totalCount > 0 ? 
                    totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : 
                    BigDecimal.ZERO;
            
            // Calculate previous period for comparison
            LocalDate prevStartDate = startDate.minusMonths(1);
            LocalDate prevEndDate = endDate.minusMonths(1);
            LocalDateTime prevStartDateTime = prevStartDate.atStartOfDay();
            LocalDateTime prevEndDateTime = prevEndDate.plusDays(1).atStartOfDay();
            
            List<Expense> prevExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    user.getId(), prevStartDateTime, prevEndDateTime);
            
            BigDecimal prevTotalAmount = prevExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal changePercentage = BigDecimal.ZERO;
            if (prevTotalAmount.compareTo(BigDecimal.ZERO) > 0) {
                changePercentage = totalAmount.subtract(prevTotalAmount)
                        .divide(prevTotalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            return new DashboardResponse.ExpenseSummary(
                    totalAmount,
                    totalCount,
                    approvedCount,
                    pendingCount,
                    rejectedCount,
                    averageAmount,
                    changePercentage
            );
            
        } catch (Exception e) {
            logger.error("Error getting expense summary for user: {}", username, e);
            throw new RuntimeException("Failed to get expense summary", e);
        }
    }

    @Override
    public DashboardResponse.BudgetSummary getBudgetSummary(String username, LocalDate startDate, LocalDate endDate) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            List<Budget> budgets = budgetRepository.findByUserIdAndPeriodOverlap(
                    user.getId(), startDate, endDate);
            
            BigDecimal totalBudget = budgets.stream()
                    .map(Budget::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate spent amount from expenses
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    user.getId(), startDateTime, endDateTime);
            
            BigDecimal totalSpent = expenses.stream()
                    .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal remainingBudget = totalBudget.subtract(totalSpent);
            
            BigDecimal usedPercentage = totalBudget.compareTo(BigDecimal.ZERO) > 0 ?
                    totalSpent.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                    BigDecimal.ZERO;
            
            long activeBudgets = budgets.stream()
                    .mapToLong(b -> b.getIsActive() ? 1 : 0)
                    .sum();
            
            long exceededBudgets = budgets.stream()
                    .mapToLong(b -> {
                        BigDecimal budgetSpent = expenses.stream()
                                .filter(e -> e.getCategory().getId().equals(b.getCategory().getId()))
                                .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        return budgetSpent.compareTo(b.getTotalAmount()) > 0 ? 1 : 0;
                    })
                    .sum();
            
            return new DashboardResponse.BudgetSummary(
                    totalBudget,
                    totalSpent,
                    remainingBudget,
                    usedPercentage,
                    activeBudgets,
                    exceededBudgets
            );
            
        } catch (Exception e) {
            logger.error("Error getting budget summary for user: {}", username, e);
            throw new RuntimeException("Failed to get budget summary", e);
        }
    }

    @Override
    public DashboardResponse.CategorySummary getCategorySummary(String username, LocalDate startDate, LocalDate endDate) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    user.getId(), startDateTime, endDateTime);
            
            Map<String, BigDecimal> categoryTotals = expenses.stream()
                    .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                    .collect(Collectors.groupingBy(
                            e -> e.getCategory().getName(),
                            Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                    ));
            
            long totalCategories = categoryTotals.size();
            
            String topCategory = categoryTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");
            
            BigDecimal topCategoryAmount = categoryTotals.getOrDefault(topCategory, BigDecimal.ZERO);
            
            return new DashboardResponse.CategorySummary(
                    totalCategories,
                    topCategory,
                    topCategoryAmount,
                    new ArrayList<>(categoryTotals.entrySet())
            );
            
        } catch (Exception e) {
            logger.error("Error getting category summary for user: {}", username, e);
            throw new RuntimeException("Failed to get category summary", e);
        }
    }

    @Override
    public List<DashboardResponse.MonthlyExpense> getMonthlyExpenseData(String username, int months) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            List<DashboardResponse.MonthlyExpense> monthlyData = new ArrayList<>();
            LocalDate currentDate = LocalDate.now();
            
            for (int i = months - 1; i >= 0; i--) {
                LocalDate targetDate = currentDate.minusMonths(i);
                LocalDate startOfMonth = targetDate.withDayOfMonth(1);
                LocalDate endOfMonth = targetDate.withDayOfMonth(targetDate.lengthOfMonth());
                
                LocalDateTime startDateTime = startOfMonth.atStartOfDay();
                LocalDateTime endDateTime = endOfMonth.plusDays(1).atStartOfDay();
                
                List<Expense> monthExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                        user.getId(), startDateTime, endDateTime);
                
                BigDecimal totalAmount = monthExpenses.stream()
                        .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                long expenseCount = monthExpenses.stream()
                        .mapToLong(e -> ExpenseStatus.APPROVED.equals(e.getStatus()) ? 1 : 0)
                        .sum();
                
                monthlyData.add(new DashboardResponse.MonthlyExpense(
                        targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        targetDate.getMonth().name(),
                        targetDate.getYear(),
                        totalAmount,
                        expenseCount
                ));
            }
            
            return monthlyData;
            
        } catch (Exception e) {
            logger.error("Error getting monthly expense data for user: {}", username, e);
            throw new RuntimeException("Failed to get monthly expense data", e);
        }
    }

    @Override
    public List<DashboardResponse.CategoryExpense> getCategoryExpenseData(String username, LocalDate startDate, LocalDate endDate) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    user.getId(), startDateTime, endDateTime);
            
            Map<String, List<Expense>> categoryGroups = expenses.stream()
                    .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                    .collect(Collectors.groupingBy(e -> e.getCategory().getName()));
            
            return categoryGroups.entrySet().stream()
                    .map(entry -> {
                        String categoryName = entry.getKey();
                        List<Expense> categoryExpenses = entry.getValue();
                        
                        BigDecimal totalAmount = categoryExpenses.stream()
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        long expenseCount = categoryExpenses.size();
                        
                        return new DashboardResponse.CategoryExpense(
                                categoryName,
                                totalAmount,
                                expenseCount,
                                "#" + Integer.toHexString(categoryName.hashCode()).substring(0, 6)
                        );
                    })
                    .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error getting category expense data for user: {}", username, e);
            throw new RuntimeException("Failed to get category expense data", e);
        }
    }

    @Override
    public List<DashboardResponse.DailyExpense> getDailyExpenseData(String username, LocalDate startDate, LocalDate endDate) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            List<DashboardResponse.DailyExpense> dailyData = new ArrayList<>();
            LocalDate currentDate = startDate;
            
            while (!currentDate.isAfter(endDate)) {
                LocalDateTime startOfDay = currentDate.atStartOfDay();
                LocalDateTime endOfDay = currentDate.plusDays(1).atStartOfDay();
                
                List<Expense> dayExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                        user.getId(), startOfDay, endOfDay);
                
                BigDecimal totalAmount = dayExpenses.stream()
                        .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                long expenseCount = dayExpenses.stream()
                        .mapToLong(e -> ExpenseStatus.APPROVED.equals(e.getStatus()) ? 1 : 0)
                        .sum();
                
                dailyData.add(new DashboardResponse.DailyExpense(
                        currentDate,
                        currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        totalAmount,
                        expenseCount
                ));
                
                currentDate = currentDate.plusDays(1);
            }
            
            return dailyData;
            
        } catch (Exception e) {
            logger.error("Error getting daily expense data for user: {}", username, e);
            throw new RuntimeException("Failed to get daily expense data", e);
        }
    }

    @Override
    public DashboardResponse.ExpenseTrend getExpenseTrend(String username, int months) {
        try {
            List<DashboardResponse.MonthlyExpense> monthlyData = getMonthlyExpenseData(username, months);
            
            if (monthlyData.size() < 2) {
                return new DashboardResponse.ExpenseTrend("STABLE", BigDecimal.ZERO, "Insufficient data");
            }
            
            BigDecimal currentMonth = monthlyData.get(monthlyData.size() - 1).getAmount();
            BigDecimal previousMonth = monthlyData.get(monthlyData.size() - 2).getAmount();
            
            BigDecimal changePercentage = BigDecimal.ZERO;
            String trend = "STABLE";
            
            if (previousMonth.compareTo(BigDecimal.ZERO) > 0) {
                changePercentage = currentMonth.subtract(previousMonth)
                        .divide(previousMonth, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                
                if (changePercentage.compareTo(BigDecimal.valueOf(5)) > 0) {
                    trend = "INCREASING";
                } else if (changePercentage.compareTo(BigDecimal.valueOf(-5)) < 0) {
                    trend = "DECREASING";
                }
            }
            
            String description = String.format("Expenses have %s by %.2f%% compared to last month",
                    trend.toLowerCase(), Math.abs(changePercentage.doubleValue()));
            
            return new DashboardResponse.ExpenseTrend(trend, changePercentage, description);
            
        } catch (Exception e) {
            logger.error("Error getting expense trend for user: {}", username, e);
            throw new RuntimeException("Failed to get expense trend", e);
        }
    }

    @Override
    public List<DashboardResponse.TopCategory> getTopCategories(String username, LocalDate startDate, LocalDate endDate, int limit) {
        try {
            List<DashboardResponse.CategoryExpense> categoryData = getCategoryExpenseData(username, startDate, endDate);
            
            return categoryData.stream()
                    .limit(limit)
                    .map(ce -> new DashboardResponse.TopCategory(
                            ce.getCategoryName(),
                            ce.getAmount(),
                            ce.getExpenseCount()
                    ))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error getting top categories for user: {}", username, e);
            throw new RuntimeException("Failed to get top categories", e);
        }
    }

    @Override
    public List<DashboardResponse.RecentExpense> getRecentExpenses(String username, int limit) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            List<Expense> recentExpenses = expenseRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
            
            return recentExpenses.stream()
                    .limit(limit)
                    .map(expense -> new DashboardResponse.RecentExpense(
                            expense.getId(),
                            expense.getTitle(),
                            expense.getDescription(),
                            expense.getAmount(),
                            expense.getCategory().getName(),
                            expense.getStatus().name(),
                            expense.getExpenseDate().atStartOfDay(),
                            expense.getCreatedAt()
                    ))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error getting recent expenses for user: {}", username, e);
            throw new RuntimeException("Failed to get recent expenses", e);
        }
    }

    @Override
    public List<DashboardResponse.BudgetStatus> getBudgetStatusList(String username) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            LocalDate now = LocalDate.now();
            List<Budget> budgets = budgetRepository.findByUserIdAndIsActiveTrue(user.getId());
            
            return budgets.stream()
                    .map(budget -> {
                        // Calculate spent amount for this budget's category
                        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
                        LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).plusDays(1).atStartOfDay();
                        
                        List<Expense> categoryExpenses = expenseRepository.findByUserIdAndCategoryIdAndExpenseDateBetween(
                                user.getId(), budget.getCategory().getId(), startOfMonth, endOfMonth);
                        
                        BigDecimal spentAmount = categoryExpenses.stream()
                                .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        BigDecimal remainingAmount = budget.getTotalAmount().subtract(spentAmount);
                        BigDecimal usedPercentage = budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0 ?
                                spentAmount.divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                                BigDecimal.ZERO;
                        
                        String status = "ON_TRACK";
                        if (usedPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
                            status = "EXCEEDED";
                        } else if (usedPercentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
                            status = "WARNING";
                        }
                        
                        return new DashboardResponse.BudgetStatus(
                                budget.getId(),
                                budget.getCategory().getName(),
                                budget.getTotalAmount(),
                                spentAmount,
                                remainingAmount,
                                usedPercentage,
                                status
                        );
                    })
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error getting budget status list for user: {}", username, e);
            throw new RuntimeException("Failed to get budget status list", e);
        }
    }

    @Override
    public DashboardResponse.BudgetAlert getBudgetAlert(String username) {
        try {
            List<DashboardResponse.BudgetStatus> budgetStatuses = getBudgetStatusList(username);
            
            long exceededBudgets = budgetStatuses.stream()
                    .mapToLong(bs -> "EXCEEDED".equals(bs.getStatus()) ? 1 : 0)
                    .sum();
            
            long warningBudgets = budgetStatuses.stream()
                    .mapToLong(bs -> "WARNING".equals(bs.getStatus()) ? 1 : 0)
                    .sum();
            
            String alertType = "SUCCESS";
            String message = "All budgets are on track";
            
            if (exceededBudgets > 0) {
                alertType = "ERROR";
                message = String.format("%d budget(s) exceeded", exceededBudgets);
            } else if (warningBudgets > 0) {
                alertType = "WARNING";
                message = String.format("%d budget(s) approaching limit", warningBudgets);
            }
            
            return new DashboardResponse.BudgetAlert(
                    alertType,
                    message,
                    exceededBudgets,
                    warningBudgets,
                    LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error getting budget alert for user: {}", username, e);
            throw new RuntimeException("Failed to get budget alert", e);
        }
    }

    @Override
    public DashboardResponse.TeamSummary getTeamSummary(Long teamId, String username) {
        try {
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(teamId);
            
            LocalDate now = LocalDate.now();
            LocalDate startOfMonth = now.withDayOfMonth(1);
            LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
            LocalDateTime startDateTime = startOfMonth.atStartOfDay();
            LocalDateTime endDateTime = endOfMonth.plusDays(1).atStartOfDay();
            
            List<Long> memberIds = teamMembers.stream()
                    .map(tm -> tm.getUser().getId())
                    .collect(Collectors.toList());
            
            List<Expense> teamExpenses = expenseRepository.findByUserIdInAndExpenseDateBetween(
                    memberIds, startDateTime, endDateTime);
            
            BigDecimal totalTeamExpenses = teamExpenses.stream()
                    .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long totalExpenseCount = teamExpenses.stream()
                    .mapToLong(e -> ExpenseStatus.APPROVED.equals(e.getStatus()) ? 1 : 0)
                    .sum();
            
            long activeMembersCount = teamMembers.size();
            
            // Find top spender
            Map<String, BigDecimal> memberTotals = teamExpenses.stream()
                    .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                    .collect(Collectors.groupingBy(
                            e -> e.getUser().getUsername(),
                            Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                    ));
            
            String topSpender = memberTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");
            
            return new DashboardResponse.TeamSummary(
                    teamId,
                    totalTeamExpenses,
                    totalExpenseCount,
                    activeMembersCount,
                    topSpender
            );
            
        } catch (Exception e) {
            logger.error("Error getting team summary for team: {} by user: {}", teamId, username, e);
            throw new RuntimeException("Failed to get team summary", e);
        }
    }

    @Override
    public List<DashboardResponse.TeamMemberExpense> getTeamMemberExpenses(Long teamId, LocalDate startDate, LocalDate endDate) {
        try {
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(teamId);
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            
            return teamMembers.stream()
                    .map(member -> {
                        List<Expense> memberExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                                member.getUser().getId(), startDateTime, endDateTime);
                        
                        BigDecimal totalAmount = memberExpenses.stream()
                                .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        long expenseCount = memberExpenses.stream()
                                .mapToLong(e -> ExpenseStatus.APPROVED.equals(e.getStatus()) ? 1 : 0)
                                .sum();
                        
                        return new DashboardResponse.TeamMemberExpense(
                                member.getUser().getId(),
                                member.getUser().getUsername(),
                                member.getUser().getFirstName() + " " + member.getUser().getLastName(),
                                totalAmount,
                                expenseCount,
                                member.getRole().name()
                        );
                    })
                    .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error getting team member expenses for team: {}", teamId, e);
            throw new RuntimeException("Failed to get team member expenses", e);
        }
    }

    @Override
    @CacheEvict(value = "dashboardData", key = "#username")
    public void updateDashboardCache(String username) {
        logger.info("Updating dashboard cache for user: {}", username);
        // Cache will be refreshed on next access
    }

    @Override
    @CacheEvict(value = "dashboardData", key = "#username")
    public void invalidateDashboardCache(String username) {
        logger.info("Invalidating dashboard cache for user: {}", username);
    }

    @Override
    public byte[] exportDashboardData(String username, String format, LocalDate startDate, LocalDate endDate) {
        try {
            logger.info("Exporting dashboard data for user: {} in format: {}", username, format);
            
            DashboardResponse dashboardData = getDashboardData(username);
            
            // This is a placeholder - you would implement actual export logic here
            // based on the format (PDF, CSV, XLSX)
            String exportContent = String.format(
                    "Dashboard Export for %s\nPeriod: %s to %s\nTotal Expenses: %s\nGenerated: %s",
                    username, startDate, endDate, 
                    dashboardData.getExpenseSummary().getTotalAmount(),
                    LocalDateTime.now()
            );
            
            return exportContent.getBytes();
            
        } catch (Exception e) {
            logger.error("Error exporting dashboard data for user: {}", username, e);
            throw new RuntimeException("Failed to export dashboard data", e);
        }
    }

    @Override
    public DashboardResponse.ComparisonData getMonthComparison(String username, LocalDate currentMonth, LocalDate previousMonth) {
        try {
            LocalDate currentStart = currentMonth.withDayOfMonth(1);
            LocalDate currentEnd = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth());
            
            LocalDate previousStart = previousMonth.withDayOfMonth(1);
            LocalDate previousEnd = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());
            
            DashboardResponse.ExpenseSummary currentSummary = getExpenseSummary(username, currentStart, currentEnd);
            DashboardResponse.ExpenseSummary previousSummary = getExpenseSummary(username, previousStart, previousEnd);
            
            BigDecimal amountDifference = currentSummary.getTotalAmount().subtract(previousSummary.getTotalAmount());
            long countDifference = currentSummary.getTotalCount() - previousSummary.getTotalCount();
            
            BigDecimal percentageChange = BigDecimal.ZERO;
            if (previousSummary.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                percentageChange = amountDifference
                        .divide(previousSummary.getTotalAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            return new DashboardResponse.ComparisonData(
                    currentSummary.getTotalAmount(),
                    previousSummary.getTotalAmount(),
                    amountDifference,
                    percentageChange,
                    countDifference
            );
            
        } catch (Exception e) {
            logger.error("Error getting month comparison for user: {}", username, e);
            throw new RuntimeException("Failed to get month comparison", e);
        }
    }

    @Override
    public DashboardResponse.YearlyComparison getYearlyComparison(String username, int currentYear, int previousYear) {
        try {
            LocalDate currentStart = LocalDate.of(currentYear, 1, 1);
            LocalDate currentEnd = LocalDate.of(currentYear, 12, 31);
            
            LocalDate previousStart = LocalDate.of(previousYear, 1, 1);
            LocalDate previousEnd = LocalDate.of(previousYear, 12, 31);
            
            DashboardResponse.ExpenseSummary currentSummary = getExpenseSummary(username, currentStart, currentEnd);
            DashboardResponse.ExpenseSummary previousSummary = getExpenseSummary(username, previousStart, previousEnd);
            
            BigDecimal amountDifference = currentSummary.getTotalAmount().subtract(previousSummary.getTotalAmount());
            long countDifference = currentSummary.getTotalCount() - previousSummary.getTotalCount();
            
            BigDecimal percentageChange = BigDecimal.ZERO;
            if (previousSummary.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                percentageChange = amountDifference
                        .divide(previousSummary.getTotalAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            return new DashboardResponse.YearlyComparison(
                    currentYear,
                    previousYear,
                    currentSummary.getTotalAmount(),
                    previousSummary.getTotalAmount(),
                    amountDifference,
                    percentageChange,
                    countDifference
            );
            
        } catch (Exception e) {
            logger.error("Error getting yearly comparison for user: {}", username, e);
            throw new RuntimeException("Failed to get yearly comparison", e);
        }
    }
}