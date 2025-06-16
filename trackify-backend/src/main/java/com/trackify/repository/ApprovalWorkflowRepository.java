package com.trackify.repository;

import com.trackify.entity.ApprovalWorkflow;
import com.trackify.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {

    // Find by expense
    Optional<ApprovalWorkflow> findByExpenseId(Long expenseId);
    List<ApprovalWorkflow> findByExpenseIdIn(List<Long> expenseIds);

    // Find by status
    List<ApprovalWorkflow> findByStatus(ApprovalStatus status);
    Page<ApprovalWorkflow> findByStatus(ApprovalStatus status, Pageable pageable);

    // Find by current approver
    List<ApprovalWorkflow> findByCurrentApproverId(Long approverId);
    List<ApprovalWorkflow> findByCurrentApproverIdAndStatus(Long approverId, ApprovalStatus status);
    Page<ApprovalWorkflow> findByCurrentApproverIdAndStatus(Long approverId, ApprovalStatus status, Pageable pageable);

    // Find by submitter
    List<ApprovalWorkflow> findBySubmittedBy(Long submittedBy);
    List<ApprovalWorkflow> findBySubmittedByAndStatus(Long submittedBy, ApprovalStatus status);
    Page<ApprovalWorkflow> findBySubmittedByAndStatus(Long submittedBy, ApprovalStatus status, Pageable pageable);

    // Find by team
    List<ApprovalWorkflow> findByTeamId(Long teamId);
    List<ApprovalWorkflow> findByTeamIdAndStatus(Long teamId, ApprovalStatus status);

    // Find pending approvals
    @Query("SELECT aw FROM ApprovalWorkflow aw WHERE aw.status = 'PENDING' ORDER BY aw.submittedAt ASC")
    List<ApprovalWorkflow> findPendingApprovals();

    @Query("SELECT aw FROM ApprovalWorkflow aw WHERE aw.currentApproverId = :approverId AND aw.status = 'PENDING' ORDER BY aw.submittedAt ASC")
    List<ApprovalWorkflow> findPendingApprovalsByApprover(@Param("approverId") Long approverId);

    // Find overdue approvals
    @Query("SELECT aw FROM ApprovalWorkflow aw WHERE aw.deadline < :currentTime AND aw.status = 'PENDING'")
    List<ApprovalWorkflow> findOverdueApprovals(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT aw FROM ApprovalWorkflow aw WHERE aw.currentApproverId = :approverId AND aw.deadline < :currentTime AND aw.status = 'PENDING'")
    List<ApprovalWorkflow> findOverdueApprovalsByApprover(@Param("approverId") Long approverId, @Param("currentTime") LocalDateTime currentTime);

    // Find escalated workflows
    List<ApprovalWorkflow> findByStatusAndEscalationLevelGreaterThan(ApprovalStatus status, Integer escalationLevel);
    List<ApprovalWorkflow> findByEscalatedTo(Long escalatedTo);

    // Find by date range
    List<ApprovalWorkflow> findBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<ApprovalWorkflow> findByApprovedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Find by amount range
    @Query("SELECT aw FROM ApprovalWorkflow aw WHERE aw.expenseAmount BETWEEN :minAmount AND :maxAmount")
    List<ApprovalWorkflow> findByExpenseAmountBetween(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);

    // Find high-value approvals
    @Query("SELECT aw FROM ApprovalWorkflow aw WHERE aw.expenseAmount >= :threshold AND aw.status = 'PENDING'")
    List<ApprovalWorkflow> findHighValuePendingApprovals(@Param("threshold") BigDecimal threshold);

    // Find by priority
    List<ApprovalWorkflow> findByPriorityAndStatus(String priority, ApprovalStatus status);
    List<ApprovalWorkflow> findByPriorityOrderBySubmittedAtAsc(String priority);

    // Statistics queries
    @Query("SELECT COUNT(aw) FROM ApprovalWorkflow aw WHERE aw.status = :status")
    long countByStatus(@Param("status") ApprovalStatus status);

    @Query("SELECT COUNT(aw) FROM ApprovalWorkflow aw WHERE aw.currentApproverId = :approverId AND aw.status = 'PENDING'")
    long countPendingByApprover(@Param("approverId") Long approverId);

    @Query("SELECT COUNT(aw) FROM ApprovalWorkflow aw WHERE aw.submittedBy = :submittedBy AND aw.status = :status")
    long countBySubmitterAndStatus(@Param("submittedBy") Long submittedBy, @Param("status") ApprovalStatus status);

    // Average approval time
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, aw.submittedAt, aw.approvedAt)) FROM ApprovalWorkflow aw WHERE aw.status = 'APPROVED'")
    Double getAverageApprovalTimeInHours();

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, aw.submittedAt, aw.approvedAt)) FROM ApprovalWorkflow aw WHERE aw.finalApproverId = :approverId AND aw.status = 'APPROVED'")
    Double getAverageApprovalTimeByApprover(@Param("approverId") Long approverId);

    // Find workflows requiring escalation
    @Query("SELECT aw FROM ApprovalWorkflow aw WHERE aw.escalationEnabled = true AND aw.status = 'PENDING' AND " +
           "TIMESTAMPDIFF(HOUR, aw.submittedAt, :currentTime) >= 24")
    List<ApprovalWorkflow> findWorkflowsRequiringEscalation(@Param("currentTime") LocalDateTime currentTime);

    // Monthly approval statistics
    @Query("SELECT YEAR(aw.approvedAt), MONTH(aw.approvedAt), COUNT(aw) " +
           "FROM ApprovalWorkflow aw WHERE aw.status = 'APPROVED' " +
           "GROUP BY YEAR(aw.approvedAt), MONTH(aw.approvedAt) " +
           "ORDER BY YEAR(aw.approvedAt) DESC, MONTH(aw.approvedAt) DESC")
    List<Object[]> getMonthlyApprovalStatistics();

    // Find by category
    List<ApprovalWorkflow> findByCategoryIdAndStatus(Long categoryId, ApprovalStatus status);

    // Find recent workflows
    @Query("SELECT aw FROM ApprovalWorkflow aw ORDER BY aw.createdAt DESC")
    List<ApprovalWorkflow> findRecentWorkflows(Pageable pageable);

    // Find workflows by approval level
    List<ApprovalWorkflow> findByApprovalLevelAndStatus(Integer approvalLevel, ApprovalStatus status);

    // Complex search query
    @Query("SELECT aw FROM ApprovalWorkflow aw WHERE " +
           "(:status IS NULL OR aw.status = :status) AND " +
           "(:approverId IS NULL OR aw.currentApproverId = :approverId) AND " +
           "(:submittedBy IS NULL OR aw.submittedBy = :submittedBy) AND " +
           "(:teamId IS NULL OR aw.teamId = :teamId) AND " +
           "(:minAmount IS NULL OR aw.expenseAmount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR aw.expenseAmount <= :maxAmount) AND " +
           "(:startDate IS NULL OR aw.submittedAt >= :startDate) AND " +
           "(:endDate IS NULL OR aw.submittedAt <= :endDate)")
    Page<ApprovalWorkflow> findByCriteria(@Param("status") ApprovalStatus status,
                                         @Param("approverId") Long approverId,
                                         @Param("submittedBy") Long submittedBy,
                                         @Param("teamId") Long teamId,
                                         @Param("minAmount") BigDecimal minAmount,
                                         @Param("maxAmount") BigDecimal maxAmount,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         Pageable pageable);

    // Auto-approval candidates
    @Query("SELECT aw FROM ApprovalWorkflow aw WHERE aw.autoApproveEnabled = true AND aw.status = 'PENDING' AND " +
           "aw.expenseAmount <= :autoApprovalThreshold")
    List<ApprovalWorkflow> findAutoApprovalCandidates(@Param("autoApprovalThreshold") BigDecimal autoApprovalThreshold);

    // Dashboard queries
    @Query("SELECT aw.status, COUNT(aw) FROM ApprovalWorkflow aw GROUP BY aw.status")
    List<Object[]> getApprovalStatusSummary();

    @Query("SELECT aw.priority, COUNT(aw) FROM ApprovalWorkflow aw WHERE aw.status = 'PENDING' GROUP BY aw.priority")
    List<Object[]> getPendingApprovalsByPriority();
}