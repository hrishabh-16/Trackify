package com.trackify.repository;

import com.trackify.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Find by expense
    List<Comment> findByExpenseIdAndIsDeletedFalseOrderByCreatedAtAsc(Long expenseId);
    List<Comment> findByExpenseIdOrderByCreatedAtDesc(Long expenseId);
    Page<Comment> findByExpenseIdAndIsDeletedFalse(Long expenseId, Pageable pageable);

    // Find by workflow
    List<Comment> findByWorkflowIdAndIsDeletedFalseOrderByCreatedAtAsc(Long workflowId);
    List<Comment> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId);

    // Find by user
    List<Comment> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
    Page<Comment> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    // Find by comment type
    List<Comment> findByCommentTypeAndIsDeletedFalse(String commentType);
    List<Comment> findByExpenseIdAndCommentTypeAndIsDeletedFalse(Long expenseId, String commentType);

    // Find system comments
    List<Comment> findByIsSystemGeneratedTrueAndExpenseIdOrderByCreatedAtAsc(Long expenseId);
    List<Comment> findByIsSystemGeneratedTrueOrderByCreatedAtDesc();

    // Find replies to a comment
    List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentCommentId);

    // Find top-level comments (no parent)
    List<Comment> findByExpenseIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(Long expenseId);

    // Find internal comments
    List<Comment> findByExpenseIdAndIsInternalTrueAndIsDeletedFalseOrderByCreatedAtAsc(Long expenseId);
    List<Comment> findByIsInternalTrueAndIsDeletedFalseOrderByCreatedAtDesc();

    // Find by visibility
    List<Comment> findByExpenseIdAndVisibilityAndIsDeletedFalseOrderByCreatedAtAsc(Long expenseId, String visibility);

    // Find comments with attachments
    List<Comment> findByAttachmentUrlIsNotNullAndIsDeletedFalseOrderByCreatedAtDesc();
    List<Comment> findByExpenseIdAndAttachmentUrlIsNotNullAndIsDeletedFalse(Long expenseId);

    // Find edited comments
    List<Comment> findByIsEditedTrueAndIsDeletedFalseOrderByEditedAtDesc();
    List<Comment> findByExpenseIdAndIsEditedTrueAndIsDeletedFalse(Long expenseId);

    // Find comments with mentions
    @Query("SELECT c FROM Comment c WHERE c.mentionedUsers IS NOT NULL AND c.mentionedUsers != '' AND c.isDeleted = false")
    List<Comment> findCommentsWithMentions();

    @Query("SELECT c FROM Comment c WHERE c.mentionedUsers LIKE CONCAT('%', :userId, '%') AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> findCommentsMentioningUser(@Param("userId") String userId);

    // Find recent comments
    @Query("SELECT c FROM Comment c WHERE c.createdAt >= :since AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments(@Param("since") LocalDateTime since);

    @Query("SELECT c FROM Comment c WHERE c.expenseId = :expenseId AND c.createdAt >= :since AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> findRecentCommentsByExpense(@Param("expenseId") Long expenseId, @Param("since") LocalDateTime since);

    // Find comments by date range
    List<Comment> findByCreatedAtBetweenAndIsDeletedFalseOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    List<Comment> findByExpenseIdAndCreatedAtBetweenAndIsDeletedFalseOrderByCreatedAtDesc(Long expenseId, LocalDateTime startDate, LocalDateTime endDate);

    // Count comments
    long countByExpenseIdAndIsDeletedFalse(Long expenseId);
    long countByWorkflowIdAndIsDeletedFalse(Long workflowId);
    long countByUserIdAndIsDeletedFalse(Long userId);
    long countByCommentTypeAndIsDeletedFalse(String commentType);

    // Count replies
    long countByParentCommentIdAndIsDeletedFalse(Long parentCommentId);

    // Search comments
    @Query("SELECT c FROM Comment c WHERE " +
           "LOWER(c.commentText) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND " +
           "c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> searchComments(@Param("searchTerm") String searchTerm);

    @Query("SELECT c FROM Comment c WHERE c.expenseId = :expenseId AND " +
           "LOWER(c.commentText) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND " +
           "c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> searchCommentsByExpense(@Param("expenseId") Long expenseId, @Param("searchTerm") String searchTerm);

    // Statistics
    @Query("SELECT c.commentType, COUNT(c) FROM Comment c WHERE c.isDeleted = false GROUP BY c.commentType")
    List<Object[]> getCommentTypeStatistics();

    @Query("SELECT DATE(c.createdAt), COUNT(c) FROM Comment c WHERE c.isDeleted = false AND c.createdAt >= :since GROUP BY DATE(c.createdAt) ORDER BY DATE(c.createdAt)")
    List<Object[]> getDailyCommentStatistics(@Param("since") LocalDateTime since);

    // Find comments requiring attention
    @Query("SELECT c FROM Comment c WHERE c.expenseId IN :expenseIds AND c.isDeleted = false AND c.createdAt >= :since ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByExpenseIds(@Param("expenseIds") List<Long> expenseIds, @Param("since") LocalDateTime since);

    // Find unanswered comments (no replies)
    @Query("SELECT c FROM Comment c WHERE c.parentCommentId IS NULL AND c.isDeleted = false AND " +
           "NOT EXISTS (SELECT r FROM Comment r WHERE r.parentCommentId = c.id AND r.isDeleted = false) AND " +
           "c.commentType != 'SYSTEM' ORDER BY c.createdAt ASC")
    List<Comment> findUnansweredComments();

    // Find approval/rejection comments
    @Query("SELECT c FROM Comment c WHERE c.expenseId = :expenseId AND " +
           "c.commentType IN ('APPROVAL', 'REJECTION') AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> findApprovalCommentsByExpense(@Param("expenseId") Long expenseId);

    // Delete old comments (for cleanup)
    @Query("SELECT c FROM Comment c WHERE c.createdAt < :cutoffDate AND c.isDeleted = false")
    List<Comment> findOldComments(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find comments by multiple criteria
    @Query("SELECT c FROM Comment c WHERE " +
           "(:expenseId IS NULL OR c.expenseId = :expenseId) AND " +
           "(:userId IS NULL OR c.userId = :userId) AND " +
           "(:commentType IS NULL OR c.commentType = :commentType) AND " +
           "(:isInternal IS NULL OR c.isInternal = :isInternal) AND " +
           "(:visibility IS NULL OR c.visibility = :visibility) AND " +
           "c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByCriteria(@Param("expenseId") Long expenseId,
                                 @Param("userId") Long userId,
                                 @Param("commentType") String commentType,
                                 @Param("isInternal") Boolean isInternal,
                                 @Param("visibility") String visibility,
                                 Pageable pageable);
}