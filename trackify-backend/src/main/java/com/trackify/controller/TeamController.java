package com.trackify.controller;

import com.trackify.dto.request.TeamRequest;
import com.trackify.dto.response.ApiResponse;
import com.trackify.dto.response.TeamResponse;
import com.trackify.enums.TeamRole;
import com.trackify.service.TeamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
@Tag(name = "Team Management", description = "Team and team member management API")
@CrossOrigin(origins = "*")
public class TeamController {

    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);

    @Autowired
    private TeamService teamService;

    @PostMapping
    @Operation(summary = "Create team", description = "Create a new team")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Team created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @Valid @RequestBody TeamRequest teamRequest,
            Authentication authentication) {
        try {
            logger.info("Creating team: {} by user: {}", teamRequest.getName(), authentication.getName());
            
            TeamResponse teamResponse = teamService.createTeam(teamRequest, authentication.getName());
            
            return ResponseEntity.status(201).body(ApiResponse.success(
                    "Team created successfully",
                    teamResponse
            ));
        } catch (Exception e) {
            logger.error("Error creating team by user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to create team: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{teamId}")
    @Operation(summary = "Update team", description = "Update team information")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest teamRequest,
            Authentication authentication) {
        try {
            logger.info("Updating team: {} by user: {}", teamId, authentication.getName());
            
            TeamResponse teamResponse = teamService.updateTeam(teamId, teamRequest, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team updated successfully",
                    teamResponse
            ));
        } catch (Exception e) {
            logger.error("Error updating team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to update team: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{teamId}")
    @Operation(summary = "Get team by ID", description = "Retrieve team information by ID")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            TeamResponse teamResponse = teamService.getTeamById(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team retrieved successfully",
                    teamResponse
            ));
        } catch (Exception e) {
            logger.error("Error getting team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    @Operation(summary = "Get user teams", description = "Get all teams for the authenticated user")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getUserTeams(Authentication authentication) {
        try {
            List<TeamResponse> teams = teamService.getUserTeams(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Teams retrieved successfully",
                    teams
            ));
        } catch (Exception e) {
            logger.error("Error getting teams for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve teams: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/page")
    @Operation(summary = "Get user teams (paginated)", description = "Get teams for the authenticated user with pagination")
    public ResponseEntity<ApiResponse<Page<TeamResponse>>> getUserTeams(
            Pageable pageable,
            Authentication authentication) {
        try {
            Page<TeamResponse> teams = teamService.getUserTeams(authentication.getName(), pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Teams retrieved successfully",
                    teams
            ));
        } catch (Exception e) {
            logger.error("Error getting teams page for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve teams: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{teamId}")
    @Operation(summary = "Delete team", description = "Delete a team (owner only)")
    public ResponseEntity<ApiResponse<String>> deleteTeam(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            logger.info("Deleting team: {} by user: {}", teamId, authentication.getName());
            
            teamService.deleteTeam(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team deleted successfully",
                    "Team has been permanently deleted"
            ));
        } catch (Exception e) {
            logger.error("Error deleting team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to delete team: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{teamId}/deactivate")
    @Operation(summary = "Deactivate team", description = "Deactivate a team")
    public ResponseEntity<ApiResponse<String>> deactivateTeam(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            teamService.deactivateTeam(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team deactivated successfully",
                    "Team has been deactivated"
            ));
        } catch (Exception e) {
            logger.error("Error deactivating team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to deactivate team: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{teamId}/activate")
    @Operation(summary = "Activate team", description = "Activate a team")
    public ResponseEntity<ApiResponse<String>> activateTeam(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            teamService.activateTeam(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team activated successfully",
                    "Team has been activated"
            ));
        } catch (Exception e) {
            logger.error("Error activating team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to activate team: " + e.getMessage()
            ));
        }
    }

    // Team Member Management Endpoints

    @PostMapping("/{teamId}/members/invite")
    @Operation(summary = "Invite member", description = "Invite a user to join the team")
    public ResponseEntity<ApiResponse<TeamResponse.InvitationResponse>> inviteMember(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest.InviteMemberRequest request,
            Authentication authentication) {
        try {
            logger.info("Inviting member to team: {} by user: {}", teamId, authentication.getName());
            
            TeamResponse.InvitationResponse invitation = teamService.inviteMember(teamId, request, authentication.getName());
            
            return ResponseEntity.status(201).body(ApiResponse.success(
                    "Invitation sent successfully",
                    invitation
            ));
        } catch (Exception e) {
            logger.error("Error inviting member to team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to send invitation: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{teamId}/members/invite-bulk")
    @Operation(summary = "Bulk invite members", description = "Invite multiple users to join the team")
    public ResponseEntity<ApiResponse<List<TeamResponse.InvitationResponse>>> bulkInviteMembers(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest.BulkInviteRequest request,
            Authentication authentication) {
        try {
            logger.info("Bulk inviting {} members to team: {} by user: {}", 
                    request.getInvitations().size(), teamId, authentication.getName());
            
            List<TeamResponse.InvitationResponse> invitations = teamService.bulkInviteMembers(teamId, request, authentication.getName());
            
            return ResponseEntity.status(201).body(ApiResponse.success(
                    "Bulk invitations sent successfully",
                    invitations
            ));
        } catch (Exception e) {
            logger.error("Error bulk inviting members to team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to send bulk invitations: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/join")
    @Operation(summary = "Join team", description = "Join a team using invite code")
    public ResponseEntity<ApiResponse<TeamResponse>> joinTeam(
            @Valid @RequestBody TeamRequest.JoinTeamRequest request,
            Authentication authentication) {
        try {
            logger.info("User: {} joining team with invite code", authentication.getName());
            
            TeamResponse teamResponse = teamService.joinTeam(request, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Successfully joined team",
                    teamResponse
            ));
        } catch (Exception e) {
            logger.error("Error joining team by user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to join team: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/accept-invitation/{inviteCode}")
    @Operation(summary = "Accept invitation", description = "Accept a team invitation")
    public ResponseEntity<ApiResponse<TeamResponse>> acceptInvitation(
            @Parameter(description = "Invite code") @PathVariable String inviteCode,
            Authentication authentication) {
        try {
            logger.info("User: {} accepting invitation with code: {}", authentication.getName(), inviteCode);
            
            TeamResponse teamResponse = teamService.acceptInvitation(inviteCode, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Invitation accepted successfully",
                    teamResponse
            ));
        } catch (Exception e) {
            logger.error("Error accepting invitation by user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to accept invitation: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{teamId}/members")
    @Operation(summary = "Get team members", description = "Get all members of a team")
    public ResponseEntity<ApiResponse<List<TeamResponse.TeamMemberInfo>>> getTeamMembers(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            List<TeamResponse.TeamMemberInfo> members = teamService.getTeamMembers(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team members retrieved successfully",
                    members
            ));
        } catch (Exception e) {
            logger.error("Error getting team members for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team members: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{teamId}/members/page")
    @Operation(summary = "Get team members (paginated)", description = "Get team members with pagination")
    public ResponseEntity<ApiResponse<Page<TeamResponse.TeamMemberInfo>>> getTeamMembers(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Pageable pageable,
            Authentication authentication) {
        try {
            Page<TeamResponse.TeamMemberInfo> members = teamService.getTeamMembers(teamId, authentication.getName(), pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team members retrieved successfully",
                    members
            ));
        } catch (Exception e) {
            logger.error("Error getting team members page for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team members: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{teamId}/members/search")
    @Operation(summary = "Search team members", description = "Search team members by name, username, or email")
    public ResponseEntity<ApiResponse<List<TeamResponse.TeamMemberInfo>>> searchTeamMembers(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Parameter(description = "Search term") @RequestParam String searchTerm,
            Authentication authentication) {
        try {
            List<TeamResponse.TeamMemberInfo> members = teamService.searchTeamMembers(teamId, searchTerm, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team members search completed successfully",
                    members
            ));
        } catch (Exception e) {
            logger.error("Error searching team members for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to search team members: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{teamId}/members/role/{role}")
    @Operation(summary = "Get team members by role", description = "Get team members with a specific role")
    public ResponseEntity<ApiResponse<List<TeamResponse.TeamMemberInfo>>> getTeamMembersByRole(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Parameter(description = "Team role") @PathVariable TeamRole role,
            Authentication authentication) {
        try {
            List<TeamResponse.TeamMemberInfo> members = teamService.getTeamMembersByRole(teamId, role, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team members by role retrieved successfully",
                    members
            ));
        } catch (Exception e) {
            logger.error("Error getting team members by role: {} for team: {} by user: {}", 
            		role, teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team members by role: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{teamId}/members/{userId}")
    @Operation(summary = "Update member role", description = "Update a team member's role and settings")
    public ResponseEntity<ApiResponse<TeamResponse.TeamMemberInfo>> updateMemberRole(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody TeamRequest.UpdateMemberRequest request,
            Authentication authentication) {
        try {
            logger.info("Updating member role for user: {} in team: {} by user: {}", 
                    userId, teamId, authentication.getName());
            
            request.setUserId(userId); // Ensure consistency
            TeamResponse.TeamMemberInfo memberInfo = teamService.updateMemberRole(teamId, request, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Member role updated successfully",
                    memberInfo
            ));
        } catch (Exception e) {
            logger.error("Error updating member role for user: {} in team: {} by user: {}", 
                    userId, teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to update member role: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @Operation(summary = "Remove member", description = "Remove a member from the team")
    public ResponseEntity<ApiResponse<String>> removeMember(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Parameter(description = "User ID") @PathVariable Long userId,
            Authentication authentication) {
        try {
            logger.info("Removing member: {} from team: {} by user: {}", userId, teamId, authentication.getName());
            
            teamService.removeMember(teamId, userId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Member removed successfully",
                    "Member has been removed from the team"
            ));
        } catch (Exception e) {
            logger.error("Error removing member: {} from team: {} by user: {}", userId, teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to remove member: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{teamId}/leave")
    @Operation(summary = "Leave team", description = "Leave a team")
    public ResponseEntity<ApiResponse<String>> leaveTeam(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            logger.info("User: {} leaving team: {}", authentication.getName(), teamId);
            
            teamService.leaveTeam(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Left team successfully",
                    "You have left the team"
            ));
        } catch (Exception e) {
            logger.error("Error leaving team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to leave team: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{teamId}/transfer-ownership")
    @Operation(summary = "Transfer ownership", description = "Transfer team ownership to another member")
    public ResponseEntity<ApiResponse<String>> transferOwnership(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest.TransferOwnershipRequest request,
            Authentication authentication) {
        try {
            logger.info("Transferring ownership of team: {} to user: {} by user: {}", 
                    teamId, request.getNewOwnerId(), authentication.getName());
            
            teamService.transferOwnership(teamId, request, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Ownership transferred successfully",
                    "Team ownership has been transferred"
            ));
        } catch (Exception e) {
            logger.error("Error transferring ownership of team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to transfer ownership: " + e.getMessage()
            ));
        }
    }

    // Invitation Management Endpoints

    @GetMapping("/{teamId}/invitations")
    @Operation(summary = "Get pending invitations", description = "Get all pending invitations for a team")
    public ResponseEntity<ApiResponse<List<TeamResponse.InvitationResponse>>> getPendingInvitations(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            List<TeamResponse.InvitationResponse> invitations = teamService.getPendingInvitations(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Pending invitations retrieved successfully",
                    invitations
            ));
        } catch (Exception e) {
            logger.error("Error getting pending invitations for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve pending invitations: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/invitations")
    @Operation(summary = "Get user pending invitations", description = "Get all pending invitations for the user")
    public ResponseEntity<ApiResponse<List<TeamResponse.InvitationResponse>>> getUserPendingInvitations(
            Authentication authentication) {
        try {
            List<TeamResponse.InvitationResponse> invitations = teamService.getUserPendingInvitations(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "User pending invitations retrieved successfully",
                    invitations
            ));
        } catch (Exception e) {
            logger.error("Error getting pending invitations for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve pending invitations: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{teamId}/invitations/{invitationId}")
    @Operation(summary = "Cancel invitation", description = "Cancel a pending invitation")
    public ResponseEntity<ApiResponse<String>> cancelInvitation(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Parameter(description = "Invitation ID") @PathVariable Long invitationId,
            Authentication authentication) {
        try {
            teamService.cancelInvitation(teamId, invitationId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Invitation canceled successfully",
                    "Invitation has been canceled"
            ));
        } catch (Exception e) {
            logger.error("Error canceling invitation: {} for team: {} by user: {}", 
                    invitationId, teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to cancel invitation: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{teamId}/invitations/{invitationId}/resend")
    @Operation(summary = "Resend invitation", description = "Resend a pending invitation")
    public ResponseEntity<ApiResponse<String>> resendInvitation(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Parameter(description = "Invitation ID") @PathVariable Long invitationId,
            Authentication authentication) {
        try {
            teamService.resendInvitation(teamId, invitationId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Invitation resent successfully",
                    "Invitation has been resent"
            ));
        } catch (Exception e) {
            logger.error("Error resending invitation: {} for team: {} by user: {}", 
                    invitationId, teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to resend invitation: " + e.getMessage()
            ));
        }
    }

    // Team Settings and Configuration Endpoints

    @PutMapping("/{teamId}/settings")
    @Operation(summary = "Update team settings", description = "Update team settings and configuration")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeamSettings(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest.TeamSettingsRequest request,
            Authentication authentication) {
        try {
            TeamResponse teamResponse = teamService.updateTeamSettings(teamId, request, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team settings updated successfully",
                    teamResponse
            ));
        } catch (Exception e) {
            logger.error("Error updating team settings for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to update team settings: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{teamId}/regenerate-invite-code")
    @Operation(summary = "Regenerate invite code", description = "Generate a new invite code for the team")
    public ResponseEntity<ApiResponse<String>> regenerateInviteCode(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            String newInviteCode = teamService.regenerateInviteCode(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Invite code regenerated successfully",
                    newInviteCode
            ));
        } catch (Exception e) {
            logger.error("Error regenerating invite code for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to regenerate invite code: " + e.getMessage()
            ));
        }
    }

    // Search and Discovery Endpoints

    @GetMapping("/search")
    @Operation(summary = "Search teams", description = "Search teams by name")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> searchTeams(
            @Parameter(description = "Search term") @RequestParam String searchTerm,
            Authentication authentication) {
        try {
            List<TeamResponse> teams = teamService.searchTeams(searchTerm, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Teams search completed successfully",
                    teams
            ));
        } catch (Exception e) {
            logger.error("Error searching teams with term: {} by user: {}", searchTerm, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to search teams: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/search/page")
    @Operation(summary = "Search teams (paginated)", description = "Search teams by name with pagination")
    public ResponseEntity<ApiResponse<Page<TeamResponse>>> searchTeams(
            @Parameter(description = "Search term") @RequestParam String searchTerm,
            Pageable pageable,
            Authentication authentication) {
        try {
            Page<TeamResponse> teams = teamService.searchTeams(searchTerm, authentication.getName(), pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Teams search completed successfully",
                    teams
            ));
        } catch (Exception e) {
            logger.error("Error searching teams page with term: {} by user: {}", searchTerm, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to search teams: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/by-invite-code/{inviteCode}")
    @Operation(summary = "Get team by invite code", description = "Get team information using invite code")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamByInviteCode(
            @Parameter(description = "Invite code") @PathVariable String inviteCode) {
        try {
            TeamResponse teamResponse = teamService.getTeamByInviteCode(inviteCode);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team retrieved successfully",
                    teamResponse
            ));
        } catch (Exception e) {
            logger.error("Error getting team by invite code: {}", inviteCode, e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team: " + e.getMessage()
            ));
        }
    }

    // Statistics and Analytics Endpoints

    @GetMapping("/{teamId}/statistics")
    @Operation(summary = "Get team statistics", description = "Get comprehensive team statistics")
    public ResponseEntity<ApiResponse<TeamResponse.TeamStatistics>> getTeamStatistics(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            TeamResponse.TeamStatistics statistics = teamService.getTeamStatistics(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team statistics retrieved successfully",
                    statistics
            ));
        } catch (Exception e) {
            logger.error("Error getting team statistics for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team statistics: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/summaries")
    @Operation(summary = "Get user team summaries", description = "Get summary information for all user teams")
    public ResponseEntity<ApiResponse<List<TeamResponse.TeamSummary>>> getUserTeamSummaries(
            Authentication authentication) {
        try {
            List<TeamResponse.TeamSummary> summaries = teamService.getUserTeamSummaries(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team summaries retrieved successfully",
                    summaries
            ));
        } catch (Exception e) {
            logger.error("Error getting team summaries for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team summaries: " + e.getMessage()
            ));
        }
    }

    // Bulk Operations Endpoints

    @PutMapping("/{teamId}/members/bulk-update")
    @Operation(summary = "Bulk update member roles", description = "Update multiple team member roles at once")
    public ResponseEntity<ApiResponse<String>> bulkUpdateMemberRoles(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Valid @RequestBody List<TeamRequest.UpdateMemberRequest> requests,
            Authentication authentication) {
        try {
            logger.info("Bulk updating {} member roles for team: {} by user: {}", 
                    requests.size(), teamId, authentication.getName());
            
            teamService.bulkUpdateMemberRoles(teamId, requests, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Member roles updated successfully",
                    "Bulk member role update completed"
            ));
        } catch (Exception e) {
            logger.error("Error bulk updating member roles for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to bulk update member roles: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{teamId}/members/bulk-remove")
    @Operation(summary = "Bulk remove members", description = "Remove multiple team members at once")
    public ResponseEntity<ApiResponse<String>> bulkRemoveMembers(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @RequestBody List<Long> userIds,
            Authentication authentication) {
        try {
            logger.info("Bulk removing {} members from team: {} by user: {}", 
                    userIds.size(), teamId, authentication.getName());
            
            teamService.bulkRemoveMembers(teamId, userIds, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Members removed successfully",
                    "Bulk member removal completed"
            ));
        } catch (Exception e) {
            logger.error("Error bulk removing members from team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to bulk remove members: " + e.getMessage()
            ));
        }
    }

    // Export Endpoints

    @GetMapping("/{teamId}/members/export")
    @Operation(summary = "Export team members", description = "Export team members list")
    public ResponseEntity<ApiResponse<List<TeamResponse.TeamMemberInfo>>> exportTeamMembers(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            List<TeamResponse.TeamMemberInfo> members = teamService.exportTeamMembers(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team members exported successfully",
                    members
            ));
        } catch (Exception e) {
            logger.error("Error exporting team members for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to export team members: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{teamId}/members/export/csv")
    @Operation(summary = "Export team members to CSV", description = "Export team members list to CSV format")
    public ResponseEntity<byte[]> exportTeamMembersToCSV(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            byte[] csvData = teamService.exportTeamMembersToCSV(teamId, authentication.getName());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=team_members_" + teamId + ".csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvData);
                    
        } catch (Exception e) {
            logger.error("Error exporting team members to CSV for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{teamId}/members/export/pdf")
    @Operation(summary = "Export team members to PDF", description = "Export team members list to PDF format")
    public ResponseEntity<byte[]> exportTeamMembersToPDF(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            byte[] pdfData = teamService.exportTeamMembersToPDF(teamId, authentication.getName());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=team_members_" + teamId + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfData);
                    
        } catch (Exception e) {
            logger.error("Error exporting team members to PDF for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // Utility Endpoints

    @PostMapping("/{teamId}/notify")
    @Operation(summary = "Notify team members", description = "Send notification to all team members")
    public ResponseEntity<ApiResponse<String>> notifyTeamMembers(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @RequestParam String title,
            @RequestParam String message,
            Authentication authentication) {
        try {
            teamService.notifyTeamMembers(teamId, title, message, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Notification sent successfully",
                    "All team members have been notified"
            ));
        } catch (Exception e) {
            logger.error("Error notifying team members for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to send notification: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{teamId}/notify/role/{role}")
    @Operation(summary = "Notify team members by role", description = "Send notification to team members with specific role")
    public ResponseEntity<ApiResponse<String>> notifyTeamMembersByRole(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Parameter(description = "Team role") @PathVariable TeamRole role,
            @RequestParam String title,
            @RequestParam String message,
            Authentication authentication) {
        try {
            teamService.notifyTeamMembersByRole(teamId, role, title, message, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Notification sent successfully",
                    "Team members with role " + role.getDisplayName() + " have been notified"
            ));
        } catch (Exception e) {
            logger.error("Error notifying team members by role: {} for team: {} by user: {}", 
                    role, teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to send notification: " + e.getMessage()
            ));
        }
    }
}