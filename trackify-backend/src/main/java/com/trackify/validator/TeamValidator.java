package com.trackify.validator;

import com.trackify.dto.request.TeamRequest;
import com.trackify.entity.Team;
import com.trackify.entity.TeamMember;
import com.trackify.entity.User;
import com.trackify.enums.ExpenseStatus;
import com.trackify.enums.TeamRole;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ForbiddenException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.ExpenseRepository;
import com.trackify.repository.TeamMemberRepository;
import com.trackify.repository.TeamRepository;
import com.trackify.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class TeamValidator {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    // Add logger
    private static final Logger logger = LoggerFactory.getLogger(TeamValidator.class);

    private static final Set<String> VALID_CURRENCIES = Set.of(
            "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR", "BRL"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final int MAX_TEAM_NAME_LENGTH = 100;
    private static final int MIN_TEAM_NAME_LENGTH = 2;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_MEMBERS_LIMIT = 1000;
    private static final int MIN_MEMBERS_LIMIT = 1;
    private static final int MAX_BULK_INVITATIONS = 50;

    public void validateTeamCreation(TeamRequest request, String ownerUsername) {
        validateBasicTeamData(request);
        validateTeamOwner(ownerUsername);
        validateUniqueTeamName(request.getName(), ownerUsername, null);
        validateTeamLimits(ownerUsername);
    }

    public void validateTeamUpdate(Long teamId, TeamRequest request, String username) {
        Team existingTeam = validateTeamExists(teamId);
        validateTeamManagementPermission(existingTeam, username);
        validateBasicTeamData(request);
        
        // Check if name is being changed and ensure uniqueness
        if (!existingTeam.getName().equals(request.getName())) {
            validateUniqueTeamName(request.getName(), username, teamId);
        }
        
        // Validate member count changes
        if (request.getMaxMembers() != null && request.getMaxMembers() < existingTeam.getMemberCount()) {
            throw new BadRequestException("Cannot set max members below current member count of " + 
                    existingTeam.getMemberCount());
        }
    }

    public void validateTeamDeletion(Long teamId, String username) {
        Team team = validateTeamExists(teamId);
        validateTeamOwnership(team, username);
        
        // Check if team has pending expenses or other dependencies
        if (teamHasPendingExpenses(teamId)) {
            throw new BadRequestException("Cannot delete team with pending expenses. " +
                    "Please resolve all pending expenses first.");
        }
    }

    public void validateMemberInvitation(Long teamId, TeamRequest.InviteMemberRequest request, String inviterUsername) {
        Team team = validateTeamExists(teamId);
        validateMemberManagementPermission(team, inviterUsername);
        validateInvitationRequest(request);
        
        User invitedUser = validateUserByEmail(request.getEmail());
        validateInvitationEligibility(team, invitedUser);
        validateRoleAssignment(team, request.getRole(), inviterUsername);
    }

    public void validateBulkMemberInvitation(Long teamId, TeamRequest.BulkInviteRequest request, String inviterUsername) {
        Team team = validateTeamExists(teamId);
        validateMemberManagementPermission(team, inviterUsername);
        validateBulkInvitationRequest(request);
        
        for (TeamRequest.InviteMemberRequest invitation : request.getInvitations()) {
            validateInvitationRequest(invitation);
            User invitedUser = validateUserByEmail(invitation.getEmail());
            validateInvitationEligibility(team, invitedUser);
            validateRoleAssignment(team, invitation.getRole(), inviterUsername);
        }
    }

    public void validateTeamJoin(TeamRequest.JoinTeamRequest request, String username) {
        validateJoinRequest(request);
        
        Team team = teamRepository.findByInviteCodeAndIsActiveTrue(request.getInviteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired invite code"));
        
        User user = validateUserExists(username);
        validateJoinEligibility(team, user);
    }

    public void validateInvitationAcceptance(String inviteCode, String username) {
        if (!StringUtils.hasText(inviteCode)) {
            throw new BadRequestException("Invite code is required");
        }
        
        User user = validateUserExists(username);
        
        // Find the invitation
        TeamMember invitation = teamMemberRepository.findPendingInvitationByUserAndInviteCode(user.getId(), inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("No valid invitation found"));
        
        if (invitation.isInvitationExpired()) {
            throw new BadRequestException("Invitation has expired");
        }
    }

    public void validateMemberRemoval(Long teamId, Long userId, String removerUsername) {
        Team team = validateTeamExists(teamId);
        validateMemberManagementPermission(team, removerUsername);
        
        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Cannot remove team owner
        if (team.isOwner(userToRemove)) {
            throw new BadRequestException("Cannot remove team owner. Transfer ownership first.");
        }
        
        // Validate membership exists
        TeamMember memberToRemove = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this team"));
        
        // Additional role-based validation
        User remover = validateUserExists(removerUsername);
        validateMemberRemovalPermission(team, memberToRemove, remover);
    }

    public void validateTeamLeave(Long teamId, String username) {
        Team team = validateTeamExists(teamId);
        User user = validateUserExists(username);
        
        // Owner cannot leave team - must transfer ownership first
        if (team.isOwner(user)) {
            throw new BadRequestException("Team owner cannot leave. Transfer ownership first.");
        }
        
        // Validate membership exists
        teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this team"));
    }

    public void validateMemberRoleUpdate(Long teamId, TeamRequest.UpdateMemberRequest request, String updaterUsername) {
        Team team = validateTeamExists(teamId);
        validateMemberManagementPermission(team, updaterUsername);
        validateUpdateMemberRequest(request);
        
        User userToUpdate = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));
        
        // Cannot change owner role
        if (team.isOwner(userToUpdate)) {
            throw new BadRequestException("Cannot change owner role. Transfer ownership instead.");
        }
        
        // Cannot promote to owner
        if (request.getRole() == TeamRole.OWNER) {
            throw new BadRequestException("Cannot promote to owner. Use transfer ownership instead.");
        }
        
        validateRoleAssignment(team, request.getRole(), updaterUsername);
        
        // Validate membership exists
        teamMemberRepository.findByTeamIdAndUserId(teamId, request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this team"));
    }

    public void validateOwnershipTransfer(Long teamId, TeamRequest.TransferOwnershipRequest request, String currentOwnerUsername) {
        Team team = validateTeamExists(teamId);
        validateTeamOwnership(team, currentOwnerUsername);
        validateTransferOwnershipRequest(request);
        
        User newOwner = userRepository.findById(request.getNewOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("New owner not found with ID: " + request.getNewOwnerId()));
        
        // Verify new owner is a team member
        TeamMember newOwnerMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, request.getNewOwnerId())
                .orElseThrow(() -> new BadRequestException("New owner must be a team member"));
        
        if (!newOwnerMembership.getIsActive()) {
            throw new BadRequestException("New owner must be an active team member");
        }
        
        // Cannot transfer to same user
        User currentOwner = validateUserExists(currentOwnerUsername);
        if (currentOwner.getId().equals(request.getNewOwnerId())) {
            throw new BadRequestException("Cannot transfer ownership to yourself");
        }
    }

    public void validateTeamSettings(Long teamId, TeamRequest.TeamSettingsRequest request, String username) {
        Team team = validateTeamExists(teamId);
        validateTeamManagementPermission(team, username);
        validateTeamSettingsRequest(request);
        
        // Validate member count changes
        if (request.getMaxMembers() != null && request.getMaxMembers() < team.getMemberCount()) {
            throw new BadRequestException("Cannot set max members below current member count of " + 
                    team.getMemberCount());
        }
    }

    public void validateInviteCodeRegeneration(Long teamId, String username) {
        Team team = validateTeamExists(teamId);
        validateTeamManagementPermission(team, username);
    }

    public void validateBulkMemberOperation(Long teamId, List<Long> userIds, String username) {
        Team team = validateTeamExists(teamId);
        validateMemberManagementPermission(team, username);
        
        if (userIds == null || userIds.isEmpty()) {
            throw new BadRequestException("User IDs list cannot be empty");
        }
        
        if (userIds.size() > 50) {
            throw new BadRequestException("Cannot process more than 50 members at once");
        }
        
        for (Long userId : userIds) {
            if (!userRepository.existsById(userId)) {
                throw new ResourceNotFoundException("User not found with ID: " + userId);
            }
        }
    }

    // Private validation methods

    private void validateBasicTeamData(TeamRequest request) {
        // Name validation
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Team name is required");
        }
        
        String trimmedName = request.getName().trim();
        if (trimmedName.length() < MIN_TEAM_NAME_LENGTH) {
            throw new BadRequestException("Team name must be at least " + MIN_TEAM_NAME_LENGTH + " characters long");
        }
        
        if (trimmedName.length() > MAX_TEAM_NAME_LENGTH) {
            throw new BadRequestException("Team name cannot exceed " + MAX_TEAM_NAME_LENGTH + " characters");
        }
        
        // Description validation
        if (request.getDescription() != null && request.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new BadRequestException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        
        // Max members validation
        if (request.getMaxMembers() != null) {
            if (request.getMaxMembers() < MIN_MEMBERS_LIMIT) {
                throw new BadRequestException("Maximum members must be at least " + MIN_MEMBERS_LIMIT);
            }
            
            if (request.getMaxMembers() > MAX_MEMBERS_LIMIT) {
                throw new BadRequestException("Maximum members cannot exceed " + MAX_MEMBERS_LIMIT);
            }
        }
        
        // Currency validation
        if (StringUtils.hasText(request.getCurrency())) {
            validateCurrency(request.getCurrency());
        }
        
        // Notes validation
        if (request.getNotes() != null && request.getNotes().length() > 1000) {
            throw new BadRequestException("Notes cannot exceed 1000 characters");
        }
    }

    private void validateCurrency(String currencyCode) {
        if (currencyCode.length() != 3) {
            throw new BadRequestException("Currency code must be exactly 3 characters");
        }
        
        try {
            Currency.getInstance(currencyCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid currency code: " + currencyCode);
        }
    }

    private Team validateTeamExists(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + teamId));
    }

    private User validateUserExists(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private User validateUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void validateTeamOwner(String ownerUsername) {
        validateUserExists(ownerUsername);
    }

    private void validateTeamOwnership(Team team, String username) {
        User user = validateUserExists(username);
        if (!team.isOwner(user)) {
            throw new ForbiddenException("Only team owner can perform this action");
        }
    }

    private void validateTeamManagementPermission(Team team, String username) {
        User user = validateUserExists(username);
        if (!team.canUserManage(user)) {
            throw new ForbiddenException("You do not have permission to manage this team");
        }
    }

    private void validateMemberManagementPermission(Team team, String username) {
        User user = validateUserExists(username);
        if (!team.canUserManageMembers(user)) {
            throw new ForbiddenException("You do not have permission to manage team members");
        }
    }

    private void validateUniqueTeamName(String teamName, String ownerUsername, Long excludeTeamId) {
        User owner = validateUserExists(ownerUsername);
        Optional<Team> existingTeam = teamRepository.findByNameAndOwnerId(teamName.trim(), owner.getId());
        
        if (existingTeam.isPresent() && 
            (excludeTeamId == null || !existingTeam.get().getId().equals(excludeTeamId))) {
            throw new BadRequestException("You already have a team with this name");
        }
    }

    private void validateTeamLimits(String ownerUsername) {
        User owner = validateUserExists(ownerUsername);
        long teamCount = teamRepository.countActiveTeamsByOwner(owner.getId());
        
        // Assuming a limit of 10 teams per user
        if (teamCount >= 10) {
            throw new BadRequestException("You have reached the maximum limit of 10 teams");
        }
    }

    private void validateInvitationRequest(TeamRequest.InviteMemberRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            throw new BadRequestException("Email is required for invitation");
        }
        
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new BadRequestException("Invalid email format");
        }
        
        if (request.getRole() == null) {
            throw new BadRequestException("Role is required for invitation");
        }
        
        if (request.getNotes() != null && request.getNotes().length() > 500) {
            throw new BadRequestException("Invitation notes cannot exceed 500 characters");
        }
    }

    private void validateBulkInvitationRequest(TeamRequest.BulkInviteRequest request) {
        if (request.getInvitations() == null || request.getInvitations().isEmpty()) {
            throw new BadRequestException("At least one invitation is required");
        }
        
        if (request.getInvitations().size() > MAX_BULK_INVITATIONS) {
            throw new BadRequestException("Cannot invite more than " + MAX_BULK_INVITATIONS + " members at once");
        }
        
        if (request.getMessage() != null && request.getMessage().length() > 500) {
            throw new BadRequestException("Bulk invitation message cannot exceed 500 characters");
        }
        
        // Check for duplicate emails in the same request
        Set<String> emails = new HashSet<>();
        for (TeamRequest.InviteMemberRequest invitation : request.getInvitations()) {
            String email = invitation.getEmail().toLowerCase();
            if (emails.contains(email)) {
                throw new BadRequestException("Duplicate email found in invitation list: " + email);
            }
            emails.add(email);
        }
    }

    private void validateInvitationEligibility(Team team, User user) {
        // Check if user is already a member
        if (team.isMember(user)) {
            throw new BadRequestException("User is already a member of this team");
        }
        
        // Check if team is at max capacity
        if (team.isAtMaxCapacity()) {
            throw new BadRequestException("Team has reached maximum member capacity");
        }
        
        // Check if there's already a pending invitation
        Optional<TeamMember> existingInvitation = teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId());
        if (existingInvitation.isPresent() && !existingInvitation.get().getIsActive()) {
            if (!existingInvitation.get().isInvitationExpired()) {
                throw new BadRequestException("User already has a pending invitation to this team");
            }
        }
        
        // Check if team is active
        if (!team.getIsActive()) {
            throw new BadRequestException("Cannot invite members to inactive team");
        }
    }

    private void validateRoleAssignment(Team team, TeamRole role, String assignerUsername) {
        User assigner = validateUserExists(assignerUsername);
        TeamMember assignerMembership = team.getTeamMember(assigner);
        
        if (assignerMembership == null) {
            throw new ForbiddenException("You are not a member of this team");
        }
        
        // Role hierarchy validation - users cannot assign roles equal or higher than their own
        // unless they are the owner
        if (!team.isOwner(assigner)) {
            TeamRole assignerRole = assignerMembership.getRole();
            
            // Check if trying to assign owner role
            if (role == TeamRole.OWNER) {
                throw new BadRequestException("Only current owner can assign owner role through ownership transfer");
            }
            
            // Check role hierarchy
            if (role.ordinal() <= assignerRole.ordinal()) {
                throw new BadRequestException("You cannot assign a role equal to or higher than your own role");
            }
        }
    }

    private void validateJoinRequest(TeamRequest.JoinTeamRequest request) {
        if (!StringUtils.hasText(request.getInviteCode())) {
            throw new BadRequestException("Invite code is required");
        }
        
        if (request.getInviteCode().length() < 6 || request.getInviteCode().length() > 50) {
            throw new BadRequestException("Invalid invite code format");
        }
        
        if (request.getMessage() != null && request.getMessage().length() > 500) {
            throw new BadRequestException("Join message cannot exceed 500 characters");
        }
    }

    private void validateJoinEligibility(Team team, User user) {
        // Check if user is already a member
        if (team.isMember(user)) {
            throw new BadRequestException("You are already a member of this team");
        }
        
        // Check if team is at max capacity
        if (team.isAtMaxCapacity()) {
            throw new BadRequestException("Team has reached maximum member capacity");
        }
        
        // Check if team is active
        if (!team.getIsActive()) {
            throw new BadRequestException("Cannot join inactive team");
        }
    }

    private void validateMemberRemovalPermission(Team team, TeamMember memberToRemove, User remover) {
        TeamMember removerMembership = team.getTeamMember(remover);
        
        if (removerMembership == null) {
            throw new ForbiddenException("You are not a member of this team");
        }
        
        // Cannot remove yourself (use leave team instead)
        if (memberToRemove.getUser().getId().equals(remover.getId())) {
            throw new BadRequestException("Use leave team functionality to remove yourself");
        }
        
        // Role-based removal validation
        if (!team.isOwner(remover)) {
            TeamRole removerRole = removerMembership.getRole();
            TeamRole targetRole = memberToRemove.getRole();
            
            // Cannot remove someone with equal or higher role
            if (targetRole.ordinal() <= removerRole.ordinal()) {
                throw new ForbiddenException("You cannot remove a member with equal or higher role");
            }
        }
    }

    private void validateUpdateMemberRequest(TeamRequest.UpdateMemberRequest request) {
        if (request.getUserId() == null) {
            throw new BadRequestException("User ID is required");
        }
        
        if (request.getRole() == null) {
            throw new BadRequestException("Role is required");
        }
        
        if (request.getNotes() != null && request.getNotes().length() > 500) {
            throw new BadRequestException("Member notes cannot exceed 500 characters");
        }
    }

    private void validateTransferOwnershipRequest(TeamRequest.TransferOwnershipRequest request) {
        if (request.getNewOwnerId() == null) {
            throw new BadRequestException("New owner ID is required");
        }
        
        if (!StringUtils.hasText(request.getConfirmationPassword())) {
            throw new BadRequestException("Confirmation password is required for ownership transfer");
        }
        
        if (request.getReason() != null && request.getReason().length() > 500) {
            throw new BadRequestException("Transfer reason cannot exceed 500 characters");
        }
    }

    private void validateTeamSettingsRequest(TeamRequest.TeamSettingsRequest request) {
        if (request.getDescription() != null && request.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new BadRequestException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        
        if (request.getMaxMembers() != null) {
            if (request.getMaxMembers() < MIN_MEMBERS_LIMIT) {
                throw new BadRequestException("Maximum members must be at least " + MIN_MEMBERS_LIMIT);
            }
            
            if (request.getMaxMembers() > MAX_MEMBERS_LIMIT) {
                throw new BadRequestException("Maximum members cannot exceed " + MAX_MEMBERS_LIMIT);
            }
        }
        
        if (StringUtils.hasText(request.getCurrency())) {
            validateCurrency(request.getCurrency());
        }
    }

    private boolean teamHasPendingExpenses(Long teamId) {
        try {
            // First, check if ExpenseRepository is available
            if (expenseRepository == null) {
                // If expense repository is not injected, we can't check expenses
                // This might happen in some test scenarios or if there's a circular dependency
                return false;
            }
            
            // Count expenses with pending-related statuses for the team
            long pendingCount = expenseRepository.countByTeamIdAndStatusIn(
                teamId, 
                List.of(
                    ExpenseStatus.PENDING, 
                    ExpenseStatus.SUBMITTED, 
                    ExpenseStatus.DRAFT
                )
            );
            
            return pendingCount > 0;
            
        } catch (Exception e) {
            // Log the error but don't fail the validation
            // In case of database issues, we err on the side of caution
            logger.warn("Error checking pending expenses for team {}: {}", teamId, e.getMessage());
            return true; // Assume there are pending expenses to prevent accidental deletion
        }
    }

    // Additional utility validation methods

    public void validateTeamAccess(Long teamId, String username) {
        Team team = validateTeamExists(teamId);
        User user = validateUserExists(username);
        
        if (!team.isMember(user)) {
            throw new ForbiddenException("You are not a member of this team");
        }
    }

    public void validateActiveTeam(Long teamId) {
        Team team = validateTeamExists(teamId);
        
        if (!team.getIsActive()) {
            throw new BadRequestException("Team is not active");
        }
    }

    public void validateInvitationCancellation(Long teamId, Long invitationId, String username) {
        Team team = validateTeamExists(teamId);
        validateMemberManagementPermission(team, username);
        
        TeamMember invitation = teamMemberRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        
        if (!invitation.getTeam().getId().equals(teamId)) {
            throw new BadRequestException("Invitation does not belong to this team");
        }
        
        if (invitation.getIsActive()) {
            throw new BadRequestException("Cannot cancel accepted invitation");
        }
    }

    public void validateSearchTerm(String searchTerm) {
        if (!StringUtils.hasText(searchTerm)) {
            throw new BadRequestException("Search term is required");
        }
        
        if (searchTerm.trim().length() < 2) {
            throw new BadRequestException("Search term must be at least 2 characters long");
        }
        
        if (searchTerm.length() > 100) {
            throw new BadRequestException("Search term cannot exceed 100 characters");
        }
    }

    public void validateNotificationMessage(String title, String message) {
        if (!StringUtils.hasText(title)) {
            throw new BadRequestException("Notification title is required");
        }
        
        if (title.length() > 100) {
            throw new BadRequestException("Notification title cannot exceed 100 characters");
        }
        
        if (!StringUtils.hasText(message)) {
            throw new BadRequestException("Notification message is required");
        }
        
        if (message.length() > 500) {
            throw new BadRequestException("Notification message cannot exceed 500 characters");
        }
    }
 }