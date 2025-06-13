package com.trackify.service;

import com.trackify.dto.request.TeamRequest;
import com.trackify.dto.response.TeamResponse;
import com.trackify.entity.Team;
import com.trackify.entity.TeamMember;
import com.trackify.enums.TeamRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TeamService {
    
    // Team CRUD operations
    TeamResponse createTeam(TeamRequest teamRequest, String username);
    TeamResponse updateTeam(Long teamId, TeamRequest teamRequest, String username);
    TeamResponse getTeamById(Long teamId, String username);
    List<TeamResponse> getUserTeams(String username);
    Page<TeamResponse> getUserTeams(String username, Pageable pageable);
    void deleteTeam(Long teamId, String username);
    void deactivateTeam(Long teamId, String username);
    void activateTeam(Long teamId, String username);
    
    // Team membership operations
    TeamResponse.InvitationResponse inviteMember(Long teamId, TeamRequest.InviteMemberRequest request, String username);
    List<TeamResponse.InvitationResponse> bulkInviteMembers(Long teamId, TeamRequest.BulkInviteRequest request, String username);
    TeamResponse joinTeam(TeamRequest.JoinTeamRequest request, String username);
    TeamResponse acceptInvitation(String inviteCode, String username);
    void removeMember(Long teamId, Long userId, String username);
    void leaveTeam(Long teamId, String username);
    
    // Member role management
    TeamResponse.TeamMemberInfo updateMemberRole(Long teamId, TeamRequest.UpdateMemberRequest request, String username);
    TeamResponse.TeamMemberInfo promoteMember(Long teamId, Long userId, TeamRole newRole, String username);
    TeamResponse.TeamMemberInfo demoteMember(Long teamId, Long userId, TeamRole newRole, String username);
    void transferOwnership(Long teamId, TeamRequest.TransferOwnershipRequest request, String username);
    
    // Team search and discovery
    List<TeamResponse> searchTeams(String searchTerm, String username);
    Page<TeamResponse> searchTeams(String searchTerm, String username, Pageable pageable);
    List<TeamResponse> getTeamsByOwner(String ownerUsername, String requestingUsername);
    TeamResponse getTeamByInviteCode(String inviteCode);
    
    // Team members management
    List<TeamResponse.TeamMemberInfo> getTeamMembers(Long teamId, String username);
    Page<TeamResponse.TeamMemberInfo> getTeamMembers(Long teamId, String username, Pageable pageable);
    List<TeamResponse.TeamMemberInfo> searchTeamMembers(Long teamId, String searchTerm, String username);
    List<TeamResponse.TeamMemberInfo> getTeamMembersByRole(Long teamId, TeamRole role, String username);
    TeamResponse.TeamMemberInfo getTeamMember(Long teamId, Long userId, String username);
    
    // Invitation management
    List<TeamResponse.InvitationResponse> getPendingInvitations(Long teamId, String username);
    List<TeamResponse.InvitationResponse> getUserPendingInvitations(String username);
    void cancelInvitation(Long teamId, Long invitationId, String username);
    void resendInvitation(Long teamId, Long invitationId, String username);
    String generateNewInviteCode(Long teamId, String username);
    
    // Team settings and configuration
    TeamResponse updateTeamSettings(Long teamId, TeamRequest.TeamSettingsRequest request, String username);
    String regenerateInviteCode(Long teamId, String username);
    void updateTeamCurrency(Long teamId, String currency, String username);
    void updateAutoApproveMembers(Long teamId, Boolean autoApprove, String username);
    
    // Permission and validation methods
    boolean isUserMemberOfTeam(Long teamId, String username);
    boolean isUserOwnerOfTeam(Long teamId, String username);
    boolean canUserManageTeam(Long teamId, String username);
    boolean canUserManageMembers(Long teamId, String username);
    boolean canUserViewFinancials(Long teamId, String username);
    boolean canUserApproveExpenses(Long teamId, String username);
    TeamRole getUserRoleInTeam(Long teamId, String username);
    
    // Statistics and analytics
    TeamResponse.TeamStatistics getTeamStatistics(Long teamId, String username);
    List<TeamResponse.TeamSummary> getUserTeamSummaries(String username);
    
    // Bulk operations
    void bulkUpdateMemberRoles(Long teamId, List<TeamRequest.UpdateMemberRequest> requests, String username);
    void bulkRemoveMembers(Long teamId, List<Long> userIds, String username);
    
    // Maintenance and cleanup
    void cleanupExpiredInvitations();
    void cleanupInactiveTeams();
    void updateMemberLastActivity(Long teamId, String username);
    
    // Notification and communication
    void notifyTeamMembers(Long teamId, String title, String message, String username);
    void notifyTeamMembersByRole(Long teamId, TeamRole role, String title, String message, String username);
    
    // Export and reporting
    List<TeamResponse.TeamMemberInfo> exportTeamMembers(Long teamId, String username);
    byte[] exportTeamMembersToCSV(Long teamId, String username);
    byte[] exportTeamMembersToPDF(Long teamId, String username);
    
    // Internal utility methods
    Team getTeamEntityById(Long teamId);
    TeamMember getTeamMemberEntity(Long teamId, String username);
    void validateTeamAccess(Long teamId, String username);
    void validateTeamManagementPermission(Long teamId, String username);
    void validateMemberManagementPermission(Long teamId, String username);
}