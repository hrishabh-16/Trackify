package com.trackify.repository;

import com.trackify.entity.Notification;
import com.trackify.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find by user
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Find unread notifications
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Find by type
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type);
    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);

    // Find by category
    List<Notification> findByUserIdAndCategoryOrderByCreatedAtDesc(Long userId, String category);
    List<Notification> findByCategoryOrderByCreatedAtDesc(String category);

    // Find by priority
    List<Notification> findByUserIdAndPriorityOrderByCreatedAtDesc(Long userId, String priority);
    List<Notification> findByPriorityAndIsReadFalseOrderByCreatedAtDesc(String priority);

    // Find by related entity
    List<Notification> findByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
    List<Notification> findByUserIdAndRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc(
            Long userId, String entityType, Long entityId);

    // Find by date range
    List<Notification> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime startDate, LocalDateTime endDate);

    // Count notifications
    long countByUserIdAndIsReadFalse(Long userId);
    long countByUserIdAndTypeAndIsReadFalse(Long userId, NotificationType type);
    long countByUserIdAndCategoryAndIsReadFalse(Long userId, String category);
    long countByUserIdAndPriorityAndIsReadFalse(Long userId, String priority);
    
    // Count old read notifications
    long countByCreatedAtBeforeAndIsReadTrue(LocalDateTime cutoffTime);
    
    // Find high priority unread notifications
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false AND " +
           "n.priority IN ('HIGH', 'URGENT') ORDER BY n.createdAt DESC")
    List<Notification> findHighPriorityUnreadByUser(@Param("userId") Long userId);

    // Find recent notifications
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND " +
           "n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotificationsByUser(@Param("userId") Long userId, 
                                                     @Param("since") LocalDateTime since);

    // Find expired notifications
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :currentTime")
    List<Notification> findExpiredNotifications(@Param("currentTime") LocalDateTime currentTime);

    // Find notifications needing email
    @Query("SELECT n FROM Notification n WHERE n.isEmailSent = false AND " +
           "(n.expiresAt IS NULL OR n.expiresAt > :currentTime) AND n.retryCount < 3")
    List<Notification> findNotificationsNeedingEmail(@Param("currentTime") LocalDateTime currentTime);

    // Find notifications needing push
    @Query("SELECT n FROM Notification n WHERE n.isPushSent = false AND " +
           "(n.expiresAt IS NULL OR n.expiresAt > :currentTime) AND n.retryCount < 3")
    List<Notification> findNotificationsNeedingPush(@Param("currentTime") LocalDateTime currentTime);

    // Find by group key
    List<Notification> findByGroupKeyOrderByCreatedAtDesc(String groupKey);
    List<Notification> findByUserIdAndGroupKeyOrderByCreatedAtDesc(Long userId, String groupKey);

    // Mark notifications as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUser(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id IN :ids")
    int markAsReadByIds(@Param("ids") List<Long> ids, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.category = :category AND n.isRead = false")
    int markCategoryAsReadByUser(@Param("userId") Long userId, @Param("category") String category, @Param("readAt") LocalDateTime readAt);

    // Mark email/push as sent
    @Modifying
    @Query("UPDATE Notification n SET n.isEmailSent = true, n.emailSentAt = :sentAt WHERE n.id = :id")
    int markEmailSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    @Modifying
    @Query("UPDATE Notification n SET n.isPushSent = true, n.pushSentAt = :sentAt WHERE n.id = :id")
    int markPushSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    // Delete expired notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :currentTime")
    int deleteExpiredNotifications(@Param("currentTime") LocalDateTime currentTime);

    // Delete old read notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.readAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffTime")
    int deleteByCreatedAtBeforeAndIsReadTrue(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Statistics queries
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.userId = :userId AND " +
           "n.createdAt >= :since GROUP BY n.type")
    List<Object[]> getNotificationTypeStatsByUser(@Param("userId") Long userId, 
                                                  @Param("since") LocalDateTime since);

    @Query("SELECT n.category, COUNT(n) FROM Notification n WHERE n.userId = :userId AND " +
           "n.createdAt >= :since GROUP BY n.category")
    List<Object[]> getNotificationCategoryStatsByUser(@Param("userId") Long userId, 
                                                      @Param("since") LocalDateTime since);

    @Query("SELECT DATE(n.createdAt), COUNT(n) FROM Notification n WHERE n.userId = :userId AND " +
           "n.createdAt >= :since GROUP BY DATE(n.createdAt) ORDER BY DATE(n.createdAt)")
    List<Object[]> getDailyNotificationStatsByUser(@Param("userId") Long userId, 
                                                   @Param("since") LocalDateTime since);

    // Find notifications for bulk operations
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false AND " +
           "n.createdAt < :before ORDER BY n.createdAt ASC")
    List<Notification> findOldUnreadNotifications(@Param("userId") Long userId, 
                                                  @Param("before") LocalDateTime before, 
                                                  Pageable pageable);

    // Find similar notifications for grouping
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.type = :type AND " +
           "n.relatedEntityType = :entityType AND n.relatedEntityId = :entityId AND " +
           "n.createdAt >= :since")
    List<Notification> findSimilarNotifications(@Param("userId") Long userId,
                                               @Param("type") NotificationType type,
                                               @Param("entityType") String entityType,
                                               @Param("entityId") Long entityId,
                                               @Param("since") LocalDateTime since);

    // Complex search query
    @Query("SELECT n FROM Notification n WHERE " +
           "(:userId IS NULL OR n.userId = :userId) AND " +
           "(:type IS NULL OR n.type = :type) AND " +
           "(:category IS NULL OR n.category = :category) AND " +
           "(:priority IS NULL OR n.priority = :priority) AND " +
           "(:isRead IS NULL OR n.isRead = :isRead) AND " +
           "(:startDate IS NULL OR n.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR n.createdAt <= :endDate) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByCriteria(@Param("userId") Long userId,
                                     @Param("type") NotificationType type,
                                     @Param("category") String category,
                                     @Param("priority") String priority,
                                     @Param("isRead") Boolean isRead,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);

    // Find notifications for retry
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.isEmailSent = false OR n.isPushSent = false) AND " +
           "n.retryCount < 3 AND " +
           "(n.lastRetryAt IS NULL OR n.lastRetryAt < :retryBefore) AND " +
           "(n.expiresAt IS NULL OR n.expiresAt > :currentTime)")
    List<Notification> findNotificationsForRetry(@Param("retryBefore") LocalDateTime retryBefore,
                                                 @Param("currentTime") LocalDateTime currentTime);

    // Update retry information
    @Modifying
    @Query("UPDATE Notification n SET n.retryCount = n.retryCount + 1, n.lastRetryAt = :retryTime WHERE n.id = :id")
    int incrementRetryCount(@Param("id") Long id, @Param("retryTime") LocalDateTime retryTime);
}