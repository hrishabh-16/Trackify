package com.trackify.repository;

import com.trackify.entity.Expense;
import com.trackify.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    // Find by user
    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);
    Page<Expense> findByUserId(Long userId, Pageable pageable);
    
    
    
    // Find by category
    List<Expense> findByCategoryId(Long categoryId);
    Page<Expense> findByCategoryId(Long categoryId, Pageable pageable);
    
    // Find by user and category
    List<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId);
    Page<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable);
    
    // Find by status
    List<Expense> findByStatus(ExpenseStatus status);
    Page<Expense> findByStatus(ExpenseStatus status, Pageable pageable);
    List<Expense> findByUserIdAndStatus(Long userId, ExpenseStatus status);
    Page<Expense> findByUserIdAndStatus(Long userId, ExpenseStatus status, Pageable pageable);
    
    //  Find by date range with LocalDate 
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdAndExpenseDateBetween(@Param("userId") Long userId, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    Page<Expense> findByUserIdAndExpenseDateBetween(@Param("userId") Long userId, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate, 
                                                   Pageable pageable);

 // Find by user ID and created at between - ADDED MISSING METHOD
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :startTime AND :endTime ORDER BY e.createdAt DESC")
    List<Expense> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId, 
                                                 @Param("startTime") LocalDateTime startTime, 
                                                 @Param("endTime") LocalDateTime endTime);
    
    // Find by date range with LocalDateTime
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.expenseDate BETWEEN DATE(:startDate) AND DATE(:endDate) ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdAndExpenseDateBetween(@Param("userId") Long userId, 
                                                  @Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);
    
    

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.expenseDate BETWEEN DATE(:startDate) AND DATE(:endDate) ORDER BY e.expenseDate DESC")
    Page<Expense> findByUserIdAndExpenseDateBetween(@Param("userId") Long userId, 
                                                  @Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate, 
                                                  Pageable pageable);

    // Find by user IDs (for team queries) with LocalDate
    @Query("SELECT e FROM Expense e WHERE e.userId IN :userIds AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdInAndExpenseDateBetween(@Param("userIds") List<Long> userIds, 
                                                     @Param("startDate") LocalDate startDate, 
                                                     @Param("endDate") LocalDate endDate);

    // Find by user IDs (for team queries) with LocalDateTime
    @Query("SELECT e FROM Expense e WHERE e.userId IN :userIds AND e.expenseDate BETWEEN DATE(:startDate) AND DATE(:endDate) ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdInAndExpenseDateBetween(@Param("userIds") List<Long> userIds, 
                                                     @Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
   
    
 // Find by user and category and date range with LocalDate
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.categoryId = :categoryId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdAndCategoryIdAndExpenseDateBetween(@Param("userId") Long userId, 
                                                                @Param("categoryId") Long categoryId, 
                                                                @Param("startDate") LocalDate startDate, 
                                                                @Param("endDate") LocalDate endDate);

    // Find by user and category and date range with LocalDateTime
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.categoryId = :categoryId AND e.expenseDate BETWEEN DATE(:startDate) AND DATE(:endDate) ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdAndCategoryIdAndExpenseDateBetween(@Param("userId") Long userId, 
                                                                @Param("categoryId") Long categoryId, 
                                                                @Param("startDate") LocalDateTime startDate, 
                                                                @Param("endDate") LocalDateTime endDate);
    
    // Find by date range (original with LocalDate)
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                          @Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    Page<Expense> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                          @Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate, 
                                          Pageable pageable);
    
    // Find by amount range
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.amount BETWEEN :minAmount AND :maxAmount ORDER BY e.expenseDate DESC")
    List<Expense> findByUserIdAndAmountRange(@Param("userId") Long userId, 
                                            @Param("minAmount") BigDecimal minAmount, 
                                            @Param("maxAmount") BigDecimal maxAmount);
    
    // Search expenses
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND " +
           "(LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.merchantName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY e.expenseDate DESC")
    Page<Expense> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);
    
    // Find pending expenses for approval
    @Query("SELECT e FROM Expense e WHERE e.status = 'PENDING' AND e.teamId IS NOT NULL ORDER BY e.createdAt ASC")
    List<Expense> findPendingExpensesForApproval();
    
    // Find team expenses
    List<Expense> findByTeamId(Long teamId);
    Page<Expense> findByTeamId(Long teamId, Pageable pageable);
    
    // Find reimbursable expenses
    List<Expense> findByUserIdAndIsReimbursableTrue(Long userId);
    List<Expense> findByUserIdAndIsReimbursableTrueAndReimbursedFalse(Long userId);
    
    // Recent expenses - Top 10
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId ORDER BY e.createdAt DESC")
    List<Expense> findTop10ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // Statistics queries
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId")
    BigDecimal getTotalAmountByUser(@Param("userId") Long userId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalAmountByUserAndDateRange(@Param("userId") Long userId, 
                                               @Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND e.categoryId = :categoryId")
    BigDecimal getTotalAmountByUserAndCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId);
    
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userId = :userId AND e.expenseDate BETWEEN :startDate AND :endDate")
    long countByUserIdAndDateRange(@Param("userId") Long userId, 
                                  @Param("startDate") LocalDate startDate, 
                                  @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.categoryId = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);
    
    // Monthly summary
    @Query("SELECT YEAR(e.expenseDate), MONTH(e.expenseDate), SUM(e.amount), COUNT(e) " +
           "FROM Expense e WHERE e.userId = :userId " +
           "GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate) " +
           "ORDER BY YEAR(e.expenseDate) DESC, MONTH(e.expenseDate) DESC")
    List<Object[]> getMonthlySummary(@Param("userId") Long userId);
    
    // Category summary
    @Query("SELECT c.name, SUM(e.amount), COUNT(e) " +
           "FROM Expense e JOIN Category c ON e.categoryId = c.id " +
           "WHERE e.userId = :userId " +
           "GROUP BY c.id, c.name " +
           "ORDER BY SUM(e.amount) DESC")
    List<Object[]> getCategorySummary(@Param("userId") Long userId);
    
    // Recent expenses
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId ORDER BY e.createdAt DESC")
    List<Expense> findRecentExpenses(@Param("userId") Long userId, Pageable pageable);
    
    // Update status
    @Modifying
    @Query("UPDATE Expense e SET e.status = :status WHERE e.id = :expenseId")
    void updateStatus(@Param("expenseId") Long expenseId, @Param("status") ExpenseStatus status);
    
    // Approve expense
    @Modifying
    @Query("UPDATE Expense e SET e.status = 'APPROVED', e.approvedBy = :approvedBy, e.approvedAt = :approvedAt WHERE e.id = :expenseId")
    void approveExpense(@Param("expenseId") Long expenseId, @Param("approvedBy") Long approvedBy, @Param("approvedAt") LocalDateTime approvedAt);
    
    // Reject expense
    @Modifying
    @Query("UPDATE Expense e SET e.status = 'REJECTED', e.rejectedBy = :rejectedBy, e.rejectedAt = :rejectedAt, e.rejectionReason = :reason WHERE e.id = :expenseId")
    void rejectExpense(@Param("expenseId") Long expenseId, @Param("rejectedBy") Long rejectedBy, @Param("rejectedAt") LocalDateTime rejectedAt, @Param("reason") String reason);
}