package com.trackify.repository;

import com.trackify.entity.Team;
import com.trackify.entity.User;
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
public interface TeamRepository extends JpaRepository<Team, Long> {

    // Find teams by owner
    List<Team> findByOwnerAndIsActiveTrue(User owner);
    List<Team> findByOwnerId(Long ownerId);
    List<Team> findByOwnerIdAndIsActiveTrue(Long ownerId);
    
    // Find teams by name
    Optional<Team> findByNameAndOwnerId(String name, Long ownerId);
    List<Team> findByNameContainingIgnoreCase(String name);
    List<Team> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    
    // Find by invite code
    Optional<Team> findByInviteCode(String inviteCode);
    Optional<Team> findByInviteCodeAndIsActiveTrue(String inviteCode);
    
    // Find active teams
    List<Team> findByIsActiveTrue();
    Page<Team> findByIsActiveTrue(Pageable pageable);
    
    // Find teams where user is a member
    @Query("SELECT DISTINCT t FROM Team t JOIN t.members tm WHERE tm.user.id = :userId AND tm.isActive = true AND t.isActive = true")
    List<Team> findTeamsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT t FROM Team t JOIN t.members tm WHERE tm.user.id = :userId AND tm.isActive = true AND t.isActive = true")
    Page<Team> findTeamsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // Find teams where user has specific role
    @Query("SELECT DISTINCT t FROM Team t JOIN t.members tm WHERE tm.user.id = :userId AND tm.role = :role AND tm.isActive = true AND t.isActive = true")
    List<Team> findTeamsByUserIdAndRole(@Param("userId") Long userId, @Param("role") String role);
    
    // Check if user is member of team
    @Query("SELECT COUNT(tm) > 0 FROM Team t JOIN t.members tm WHERE t.id = :teamId AND tm.user.id = :userId AND tm.isActive = true")
    boolean isUserMemberOfTeam(@Param("teamId") Long teamId, @Param("userId") Long userId);
    
    // Check if user is owner of team
    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.id = :teamId AND t.owner.id = :userId")
    boolean isUserOwnerOfTeam(@Param("teamId") Long teamId, @Param("userId") Long userId);
    
    // Get teams with member count
    @Query("SELECT t, COUNT(tm) as memberCount FROM Team t LEFT JOIN t.members tm WHERE t.isActive = true GROUP BY t")
    List<Object[]> findTeamsWithMemberCount();
    
    // Find teams created in date range
    List<Team> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Team> findByCreatedAtBetweenAndIsActiveTrue(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find teams by currency
    List<Team> findByCurrency(String currency);
    List<Team> findByCurrencyAndIsActiveTrue(String currency);
    
    // Find teams by max members
    List<Team> findByMaxMembersGreaterThanEqual(Integer maxMembers);
    
    // Find teams that auto approve members
    List<Team> findByAutoApproveMembersTrue();
    List<Team> findByAutoApproveMembersTrueAndIsActiveTrue();
    
    // Search teams by multiple criteria
    @Query("SELECT t FROM Team t WHERE " +
           "(:name IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:ownerId IS NULL OR t.owner.id = :ownerId) AND " +
           "(:isActive IS NULL OR t.isActive = :isActive) AND " +
           "(:currency IS NULL OR t.currency = :currency)")
    Page<Team> findTeamsByCriteria(@Param("name") String name,
                                  @Param("ownerId") Long ownerId,
                                  @Param("isActive") Boolean isActive,
                                  @Param("currency") String currency,
                                  Pageable pageable);
    
    // Get team statistics
    @Query("SELECT COUNT(t) FROM Team t WHERE t.isActive = true")
    long countActiveTeams();
    
    @Query("SELECT COUNT(t) FROM Team t WHERE t.owner.id = :ownerId AND t.isActive = true")
    long countActiveTeamsByOwner(@Param("ownerId") Long ownerId);
    
    // Find teams with expenses in date range
    @Query("SELECT DISTINCT t FROM Team t JOIN t.expenses e WHERE e.expenseDate BETWEEN :startDate AND :endDate AND t.isActive = true")
    List<Team> findTeamsWithExpensesInDateRange(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    // Find teams with budgets
    @Query("SELECT DISTINCT t FROM Team t JOIN t.budgets b WHERE b.isActive = true AND t.isActive = true")
    List<Team> findTeamsWithActiveBudgets();
    
    // Custom query to find teams user can manage
    @Query("SELECT DISTINCT t FROM Team t LEFT JOIN t.members tm WHERE " +
           "(t.owner.id = :userId) OR " +
           "(tm.user.id = :userId AND tm.isActive = true AND tm.role IN ('OWNER', 'ADMIN', 'MANAGER')) " +
           "AND t.isActive = true")
    List<Team> findTeamsUserCanManage(@Param("userId") Long userId);
    
    // Find teams with pending invitations
    @Query("SELECT DISTINCT t FROM Team t JOIN t.members tm WHERE tm.isActive = false AND tm.invitationExpiresAt > :currentTime AND t.isActive = true")
    List<Team> findTeamsWithPendingInvitations(@Param("currentTime") LocalDateTime currentTime);
    
    // Delete teams with no active members (cleanup)
    @Query("SELECT t FROM Team t WHERE t.isActive = true AND " +
           "(SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team = t AND tm.isActive = true) = 0")
    List<Team> findTeamsWithNoActiveMembers();
}