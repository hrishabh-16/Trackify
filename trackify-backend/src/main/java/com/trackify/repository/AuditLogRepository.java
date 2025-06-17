package com.trackify.repository;

import com.trackify.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find by user
    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    List<AuditLog> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
            Long userId, LocalDateTime startDate, LocalDateTime endDate);

    // Find by action
    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    List<AuditLog> findByActionInOrderByTimestampDesc(List<String> actions);

    // Find by entity
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType, Long entityId);

    // Find by team
    Page<AuditLog> findByTeamIdOrderByTimestampDesc(Long teamId, Pageable pageable);

    List<AuditLog> findByTeamIdAndTimestampBetweenOrderByTimestampDesc(
            Long teamId, LocalDateTime startDate, LocalDateTime endDate);

    // Find by success status
    Page<AuditLog> findBySuccessOrderByTimestampDesc(Boolean success, Pageable pageable);

    List<AuditLog> findBySuccessFalseAndTimestampBetweenOrderByTimestampDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    // Find by timestamp range
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Find by user and action
    List<AuditLog> findByUserIdAndActionOrderByTimestampDesc(Long userId, String action);

    // Find by user and entity
    List<AuditLog> findByUserIdAndEntityTypeOrderByTimestampDesc(Long userId, String entityType);

    // Find by user, action and date range
    List<AuditLog> findByUserIdAndActionAndTimestampBetweenOrderByTimestampDesc(
            Long userId, String action, LocalDateTime startDate, LocalDateTime endDate);

    // Find by session
    List<AuditLog> findBySessionIdOrderByTimestampDesc(String sessionId);

    // Find by request ID
    Optional<AuditLog> findByRequestId(String requestId);

    // Find by IP address
    List<AuditLog> findByIpAddressAndTimestampBetweenOrderByTimestampDesc(
            String ipAddress, LocalDateTime startDate, LocalDateTime endDate);

    // Complex queries using @Query annotation
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.entityType = :entityType " +
           "AND al.entityId = :entityId ORDER BY al.timestamp DESC")
    List<AuditLog> findUserActivityForEntity(@Param("userId") Long userId, 
                                           @Param("entityType") String entityType,
                                           @Param("entityId") Long entityId);

    @Query("SELECT al FROM AuditLog al WHERE al.teamId = :teamId AND al.action IN :actions " +
           "AND al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    List<AuditLog> findTeamActivityByActions(@Param("teamId") Long teamId,
                                           @Param("actions") List<String> actions,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DISTINCT al.userId FROM AuditLog al WHERE al.teamId = :teamId " +
           "AND al.timestamp BETWEEN :startDate AND :endDate")
    List<Long> findActiveUsersByTeam(@Param("teamId") Long teamId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al.action, COUNT(al) FROM AuditLog al WHERE al.userId = :userId " +
           "AND al.timestamp BETWEEN :startDate AND :endDate GROUP BY al.action")
    List<Object[]> getUserActivitySummary(@Param("userId") Long userId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al.entityType, COUNT(al) FROM AuditLog al WHERE al.teamId = :teamId " +
           "AND al.timestamp BETWEEN :startDate AND :endDate GROUP BY al.entityType")
    List<Object[]> getTeamActivitySummary(@Param("teamId") Long teamId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.success = false " +
           "AND al.timestamp BETWEEN :startDate AND :endDate")
    Long countFailedOperations(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al FROM AuditLog al WHERE al.success = false AND al.userId = :userId " +
           "AND al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    List<AuditLog> findUserFailedOperations(@Param("userId") Long userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Find high-risk activities
    @Query("SELECT al FROM AuditLog al WHERE al.action IN ('DELETE', 'PASSWORD_CHANGE', 'SETTINGS_UPDATE') " +
           "AND al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    List<AuditLog> findHighRiskActivities(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Find suspicious activities (multiple failed logins)
    @Query("SELECT al.ipAddress, COUNT(al) FROM AuditLog al WHERE al.action = 'LOGIN_FAILED' " +
           "AND al.timestamp BETWEEN :startDate AND :endDate GROUP BY al.ipAddress " +
           "HAVING COUNT(al) > :threshold")
    List<Object[]> findSuspiciousLoginAttempts(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate,
                                             @Param("threshold") Long threshold);

    // Performance monitoring
    @Query("SELECT AVG(al.executionTimeMs) FROM AuditLog al WHERE al.action = :action " +
           "AND al.executionTimeMs IS NOT NULL AND al.timestamp BETWEEN :startDate AND :endDate")
    Double getAverageExecutionTime(@Param("action") String action,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    // Data cleanup methods
    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.timestamp < :cutoffDate")
    int deleteOldAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.success = true AND al.action = 'VIEW' " +
           "AND al.timestamp < :cutoffDate")
    int deleteOldViewLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Count methods
    Long countByUserIdAndTimestampBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    Long countByTeamIdAndTimestampBetween(Long teamId, LocalDateTime startDate, LocalDateTime endDate);

    Long countByActionAndTimestampBetween(String action, LocalDateTime startDate, LocalDateTime endDate);

    Long countBySuccessAndTimestampBetween(Boolean success, LocalDateTime startDate, LocalDateTime endDate);

    // Recent activity
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId " +
           "AND al.action NOT IN ('VIEW', 'LOGIN') ORDER BY al.timestamp DESC")
    Page<AuditLog> findRecentUserActivity(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.teamId = :teamId " +
           "AND al.action NOT IN ('VIEW') ORDER BY al.timestamp DESC")
    Page<AuditLog> findRecentTeamActivity(@Param("teamId") Long teamId, Pageable pageable);

    // Most active users
    @Query("SELECT al.userId, al.username, COUNT(al) as activityCount FROM AuditLog al " +
           "WHERE al.teamId = :teamId AND al.timestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY al.userId, al.username ORDER BY activityCount DESC")
    List<Object[]> findMostActiveUsers(@Param("teamId") Long teamId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);
}