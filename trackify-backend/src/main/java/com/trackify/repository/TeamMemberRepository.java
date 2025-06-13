package com.trackify.repository;

import com.trackify.entity.TeamMember;
import com.trackify.enums.TeamRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    // Find by team
    List<TeamMember> findByTeamId(Long teamId);
    List<TeamMember> findByTeamIdAndIsActiveTrue(Long teamId);
    Page<TeamMember> findByTeamIdAndIsActiveTrue(Long teamId, Pageable pageable);
    
    // Find by user
    List<TeamMember> findByUserId(Long userId);
    List<TeamMember> findByUserIdAndIsActiveTrue(Long userId);
    
    // Find by team and user
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);
    Optional<TeamMember> findByTeamIdAndUserIdAndIsActiveTrue(Long teamId, Long userId);
    
    // Find by role
    List<TeamMember> findByRole(TeamRole role);
    List<TeamMember> findByTeamIdAndRole(Long teamId, TeamRole role);
    List<TeamMember> findByTeamIdAndRoleAndIsActiveTrue(Long teamId, TeamRole role);
    
    // Check membership
    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.user.id = :userId AND tm.isActive = true")
    boolean existsByTeamIdAndUserIdAndIsActiveTrue(@Param("teamId") Long teamId, @Param("userId") Long userId);
    
    // Find team owners
    @Query("SELECT tm FROM TeamMember tm WHERE tm.role = 'OWNER' AND tm.isActive = true")
    List<TeamMember> findAllOwners();
    
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role = 'OWNER' AND tm.isActive = true")
    Optional<TeamMember> findOwnerByTeamId(@Param("teamId") Long teamId);
    
    // Find team admins and managers
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role IN ('ADMIN', 'MANAGER') AND tm.isActive = true")
    List<TeamMember> findAdminsAndManagersByTeamId(@Param("teamId") Long teamId);
    
    // Find members with management permissions
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role IN ('OWNER', 'ADMIN', 'MANAGER') AND tm.isActive = true")
    List<TeamMember> findMembersWithManagementPermissions(@Param("teamId") Long teamId);
    
    // Find pending invitations
    List<TeamMember> findByIsActiveFalseAndInvitationExpiresAtAfter(LocalDateTime currentTime);
    List<TeamMember> findByTeamIdAndIsActiveFalseAndInvitationExpiresAtAfter(Long teamId, LocalDateTime currentTime);
    List<TeamMember> findByUserIdAndIsActiveFalseAndInvitationExpiresAtAfter(Long userId, LocalDateTime currentTime);
    
    // Find expired invitations
    List<TeamMember> findByIsActiveFalseAndInvitationExpiresAtBefore(LocalDateTime currentTime);
    List<TeamMember> findByTeamIdAndIsActiveFalseAndInvitationExpiresAtBefore(Long teamId, LocalDateTime currentTime);
    
    // Find by invited by
    List<TeamMember> findByInvitedBy(Long invitedBy);
    List<TeamMember> findByTeamIdAndInvitedBy(Long teamId, Long invitedBy);
    
    // Find recently joined members
    List<TeamMember> findByTeamIdAndJoinedAtAfterAndIsActiveTrue(Long teamId, LocalDateTime afterDate);
    
    // Find inactive members
    List<TeamMember> findByTeamIdAndIsActiveFalse(Long teamId);
    List<TeamMember> findByTeamIdAndLastActiveAtBeforeAndIsActiveTrue(Long teamId, LocalDateTime beforeDate);
    
    // Count members
    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.isActive = true")
    long countActiveByTeamId(@Param("teamId") Long teamId);
    
    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role = :role AND tm.isActive = true")
    long countByTeamIdAndRoleAndIsActiveTrue(@Param("teamId") Long teamId, @Param("role") TeamRole role);
    
    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.user.id = :userId AND tm.isActive = true")
    long countActiveByUserId(@Param("userId") Long userId);
    
    // Find team members by multiple criteria
    @Query("SELECT tm FROM TeamMember tm WHERE " +
           "tm.team.id = :teamId AND " +
           "(:role IS NULL OR tm.role = :role) AND " +
           "(:isActive IS NULL OR tm.isActive = :isActive) AND " +
           "(:invitedBy IS NULL OR tm.invitedBy = :invitedBy)")
    List<TeamMember> findByTeamIdAndCriteria(@Param("teamId") Long teamId,
                                           @Param("role") TeamRole role,
                                           @Param("isActive") Boolean isActive,
                                           @Param("invitedBy") Long invitedBy);
    
    // Search team members
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND " +
           "(LOWER(tm.user.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(tm.user.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(tm.user.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(tm.user.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND tm.isActive = true")
    List<TeamMember> searchTeamMembers(@Param("teamId") Long teamId, @Param("searchTerm") String searchTerm);
    
    // Find members joined in date range
    List<TeamMember> findByTeamIdAndJoinedAtBetweenAndIsActiveTrue(Long teamId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Get team member statistics
    @Query("SELECT tm.role, COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.isActive = true GROUP BY tm.role")
    List<Object[]> getTeamMemberRoleStatistics(@Param("teamId") Long teamId);
    
    // Find members who can approve expenses
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role IN ('OWNER', 'ADMIN', 'MANAGER') AND tm.isActive = true")
    List<TeamMember> findMembersWhoCanApproveExpenses(@Param("teamId") Long teamId);
    
    // Find members who can manage team
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role IN ('OWNER', 'ADMIN') AND tm.isActive = true")
    List<TeamMember> findMembersWhoCanManageTeam(@Param("teamId") Long teamId);
    
    // Delete expired invitations (for cleanup)
    void deleteByIsActiveFalseAndInvitationExpiresAtBefore(LocalDateTime currentTime);
    
    // Find all active memberships for user
    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.isActive = true AND tm.team.isActive = true")
    List<TeamMember> findActiveMembershipsByUserId(@Param("userId") Long userId);
    
    // Find teams where user has specific minimum role
    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.isActive = true AND tm.team.isActive = true AND " +
           "tm.role IN ('OWNER', 'ADMIN', 'MANAGER')")
    List<TeamMember> findTeamsWhereUserCanManage(@Param("userId") Long userId);
    
    // Get member activity summary
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.lastActiveAt < :inactiveThreshold AND tm.isActive = true")
    List<TeamMember> findInactiveMembers(@Param("teamId") Long teamId, @Param("inactiveThreshold") LocalDateTime inactiveThreshold);
}