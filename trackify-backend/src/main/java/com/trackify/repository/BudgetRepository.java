package com.trackify.repository;

import com.trackify.entity.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // Find by user
    List<Budget> findByUserId(Long userId);
    List<Budget> findByUserIdAndIsActiveTrue(Long userId);
    Page<Budget> findByUserId(Long userId, Pageable pageable);
    Page<Budget> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    // Find by category
    List<Budget> findByCategoryId(Long categoryId);
    List<Budget> findByUserIdAndCategoryId(Long userId, Long categoryId);
    Optional<Budget> findByUserIdAndCategoryIdAndIsActiveTrue(Long userId, Long categoryId);

    // Find by team
    List<Budget> findByTeamId(Long teamId);
    List<Budget> findByTeamIdAndIsActiveTrue(Long teamId);

    // Find by date range
    List<Budget> findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, LocalDate endDate, LocalDate startDate);
    
 // Find expired inactive budgets
    List<Budget> findByEndDateBeforeAndIsActiveFalse(LocalDate cutoffDate);

    // Find by period between dates
//    List<Budget> findByUserIdAndPeriodBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // Custom query for period overlap
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND " +
           "((b.startDate <= :endDate AND b.endDate >= :startDate))")
    List<Budget> findByUserIdAndPeriodOverlap(@Param("userId") Long userId, 
                                              @Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);

    // Find active budgets
    @Query("SELECT b FROM Budget b WHERE b.isActive = true AND b.startDate <= :currentDate AND b.endDate >= :currentDate")
    List<Budget> findActiveBudgets(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.isActive = true AND " +
           "b.startDate <= :currentDate AND b.endDate >= :currentDate")
    List<Budget> findActiveBudgetsByUser(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);

    // Find expired budgets
    @Query("SELECT b FROM Budget b WHERE b.endDate < :currentDate")
    List<Budget> findExpiredBudgets(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.endDate < :currentDate")
    List<Budget> findExpiredBudgetsByUser(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);

    // Find budgets near threshold
    @Query("SELECT b FROM Budget b WHERE b.isActive = true AND " +
           "(b.spentAmount / b.totalAmount * 100) >= b.alertThreshold")
    List<Budget> findBudgetsNearThreshold();

    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.isActive = true AND " +
           "(b.spentAmount / b.totalAmount * 100) >= b.alertThreshold")
    List<Budget> findBudgetsNearThresholdByUser(@Param("userId") Long userId);

    // Find over-budget budgets
    @Query("SELECT b FROM Budget b WHERE b.isActive = true AND b.spentAmount > b.totalAmount")
    List<Budget> findOverBudgets();

    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.isActive = true AND b.spentAmount > b.totalAmount")
    List<Budget> findOverBudgetsByUser(@Param("userId") Long userId);

    // Find recurring budgets
    List<Budget> findByIsRecurringTrue();
    List<Budget> findByUserIdAndIsRecurringTrue(Long userId);

    // Search budgets
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND " +
           "(LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Budget> searchBudgets(@Param("userId") Long userId, @Param("keyword") String keyword);

    // Statistics
    @Query("SELECT SUM(b.totalAmount) FROM Budget b WHERE b.userId = :userId AND b.isActive = true")
    BigDecimal getTotalBudgetAmountByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(b.spentAmount) FROM Budget b WHERE b.userId = :userId AND b.isActive = true")
    BigDecimal getTotalSpentAmountByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) FROM Budget b WHERE b.userId = :userId AND b.isActive = true")
    long countActiveBudgetsByUser(@Param("userId") Long userId);

    // Find budgets by amount range
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.totalAmount BETWEEN :minAmount AND :maxAmount")
    List<Budget> findByUserIdAndAmountRange(@Param("userId") Long userId, 
                                           @Param("minAmount") BigDecimal minAmount, 
                                           @Param("maxAmount") BigDecimal maxAmount);

    // Find budgets created in date range
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.createdAt BETWEEN :startDate AND :endDate")
    List<Budget> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId, 
                                                @Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate);

    // Check for overlapping budgets for same category
    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.userId = :userId AND b.categoryId = :categoryId AND " +
           "b.isActive = true AND b.id != :excludeId AND " +
           "((b.startDate <= :endDate AND b.endDate >= :startDate))")
    boolean hasOverlappingBudget(@Param("userId") Long userId, 
                                @Param("categoryId") Long categoryId,
                                @Param("startDate") LocalDate startDate, 
                                @Param("endDate") LocalDate endDate,
                                @Param("excludeId") Long excludeId);

    // Find budgets for dashboard
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND " +
           "((b.startDate <= :currentDate AND b.endDate >= :currentDate) OR " +
           "(b.endDate >= :monthStart)) ORDER BY b.endDate ASC")
    List<Budget> findBudgetsForDashboard(@Param("userId") Long userId, 
                                        @Param("currentDate") LocalDate currentDate,
                                        @Param("monthStart") LocalDate monthStart);
}