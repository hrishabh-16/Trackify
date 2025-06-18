package com.trackify.service.impl;

import com.trackify.dto.request.TeamRequest;
import com.trackify.dto.response.TeamResponse;
import com.trackify.dto.response.TeamResponse.TeamMemberInfo;
import com.trackify.entity.Team;
import com.trackify.entity.TeamMember;
import com.trackify.entity.User;
import com.trackify.enums.TeamRole;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ForbiddenException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.TeamMemberRepository;
import com.trackify.repository.TeamRepository;
import com.trackify.repository.UserRepository;
import com.trackify.service.TeamService;
import com.trackify.service.WebSocketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


//iText PDF imports
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;


//Apache Commons CSV
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

//Java I/O and encoding
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

//Collections
import java.util.Map;
import java.util.stream.Collectors;

//Java imports
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private static final Logger logger = LoggerFactory.getLogger(TeamServiceImpl.class);

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Override
    public TeamResponse createTeam(TeamRequest teamRequest, String username) {
        try {
            logger.info("Creating team: {} by user: {}", teamRequest.getName(), username);

            User owner = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            // Check if user already has a team with the same name
            Optional<Team> existingTeam = teamRepository.findByNameAndOwnerId(teamRequest.getName(), owner.getId());
            if (existingTeam.isPresent()) {
                throw new BadRequestException("You already have a team with this name");
            }

            // Create team
            Team team = new Team(
                    teamRequest.getName(),
                    teamRequest.getDescription(),
                    owner
            );
            team.setMaxMembers(teamRequest.getMaxMembers());
            team.setCurrency(teamRequest.getCurrency());
            team.setAutoApproveMembers(teamRequest.getAutoApproveMembers());
            team.setInviteCode(generateInviteCode());

            team = teamRepository.save(team);

            // Add owner as team member
            TeamMember ownerMember = new TeamMember(team, owner, TeamRole.OWNER);
            teamMemberRepository.save(ownerMember);

            logger.info("Successfully created team: {} with ID: {}", team.getName(), team.getId());

            return convertToTeamResponse(team, username);

        } catch (Exception e) {
            logger.error("Error creating team for user: {}", username, e);
            throw e;
        }
    }

    @Override
    public TeamResponse updateTeam(Long teamId, TeamRequest teamRequest, String username) {
        try {
            logger.info("Updating team: {} by user: {}", teamId, username);

            validateTeamManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
            team.setName(teamRequest.getName());
            team.setDescription(teamRequest.getDescription());
            team.setMaxMembers(teamRequest.getMaxMembers());
            team.setCurrency(teamRequest.getCurrency());
            team.setAutoApproveMembers(teamRequest.getAutoApproveMembers());

            team = teamRepository.save(team);

            logger.info("Successfully updated team: {}", teamId);

            // Notify team members about update
            webSocketService.sendTeamNotification(teamId, 
                new com.trackify.dto.websocket.NotificationMessage(
                    "Team Updated",
                    "Team settings have been updated by " + username,
                    "INFO",
                    LocalDateTime.now()
                ));

            return convertToTeamResponse(team, username);

        } catch (Exception e) {
            logger.error("Error updating team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long teamId, String username) {
        try {
            validateTeamAccess(teamId, username);
            Team team = getTeamEntityById(teamId);
            return convertToTeamResponse(team, username);
        } catch (Exception e) {
            logger.error("Error getting team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> getUserTeams(String username) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Team> teams = teamRepository.findTeamsByUserId(user.getId());
            
            return teams.stream()
                    .map(team -> convertToTeamResponse(team, username))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting teams for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamResponse> getUserTeams(String username, Pageable pageable) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            Page<Team> teams = teamRepository.findTeamsByUserId(user.getId(), pageable);
            
            return teams.map(team -> convertToTeamResponse(team, username));

        } catch (Exception e) {
            logger.error("Error getting teams page for user: {}", username, e);
            throw e;
        }
    }

    @Override
    public void deleteTeam(Long teamId, String username) {
        try {
            logger.info("Deleting team: {} by user: {}", teamId, username);

            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            Team team = getTeamEntityById(teamId);

            if (!team.isOwner(user)) {
                throw new ForbiddenException("Only team owner can delete the team");
            }

            // Notify team members before deletion
            webSocketService.sendTeamNotification(teamId, 
                new com.trackify.dto.websocket.NotificationMessage(
                    "Team Deleted",
                    "Team " + team.getName() + " has been deleted by the owner",
                    "WARNING",
                    LocalDateTime.now()
                ));

            teamRepository.delete(team);

            logger.info("Successfully deleted team: {}", teamId);

        } catch (Exception e) {
            logger.error("Error deleting team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public void deactivateTeam(Long teamId, String username) {
        try {
            logger.info("Deactivating team: {} by user: {}", teamId, username);

            validateTeamManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
            team.setIsActive(false);
            teamRepository.save(team);

            // Notify team members
            webSocketService.sendTeamNotification(teamId, 
                new com.trackify.dto.websocket.NotificationMessage(
                    "Team Deactivated",
                    "Team has been deactivated by " + username,
                    "WARNING",
                    LocalDateTime.now()
                ));

            logger.info("Successfully deactivated team: {}", teamId);

        } catch (Exception e) {
            logger.error("Error deactivating team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public void activateTeam(Long teamId, String username) {
        try {
            logger.info("Activating team: {} by user: {}", teamId, username);

            validateTeamManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
            team.setIsActive(true);
            teamRepository.save(team);

            // Notify team members
            webSocketService.sendTeamNotification(teamId, 
                new com.trackify.dto.websocket.NotificationMessage(
                    "Team Activated",
                    "Team has been activated by " + username,
                    "SUCCESS",
                    LocalDateTime.now()
                ));

            logger.info("Successfully activated team: {}", teamId);

        } catch (Exception e) {
            logger.error("Error activating team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public TeamResponse.InvitationResponse inviteMember(Long teamId, TeamRequest.InviteMemberRequest request, String username) {
        try {
            logger.info("Inviting member to team: {} by user: {}", teamId, username);

            validateMemberManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
            User inviter = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            // Check if team is at max capacity
            if (team.isAtMaxCapacity()) {
                throw new BadRequestException("Team has reached maximum member capacity");
            }

            // Find user to invite
            User invitedUser = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

            // Check if user is already a member
            if (team.isMember(invitedUser)) {
                throw new BadRequestException("User is already a member of this team");
            }

            // Create team member invitation
            TeamMember invitation = new TeamMember(team, invitedUser, request.getRole(), inviter.getId());
            invitation.setNotes(request.getNotes());
            invitation = teamMemberRepository.save(invitation);

            // Send notification to invited user
            webSocketService.sendNotificationToUser(invitedUser.getUsername(),
                new com.trackify.dto.websocket.NotificationMessage(
                    "Team Invitation",
                    "You have been invited to join team: " + team.getName(),
                    "INFO",
                    invitedUser.getUsername(),
                    LocalDateTime.now()
                ));

            logger.info("Successfully invited user: {} to team: {}", request.getEmail(), teamId);

            return convertToInvitationResponse(invitation);

        } catch (Exception e) {
            logger.error("Error inviting member to team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public List<TeamResponse.InvitationResponse> bulkInviteMembers(Long teamId, TeamRequest.BulkInviteRequest request, String username) {
        try {
            logger.info("Bulk inviting {} members to team: {} by user: {}", 
                    request.getInvitations().size(), teamId, username);

            List<TeamResponse.InvitationResponse> responses = new ArrayList<>();

            for (TeamRequest.InviteMemberRequest invitation : request.getInvitations()) {
                try {
                    TeamResponse.InvitationResponse response = inviteMember(teamId, invitation, username);
                    responses.add(response);
                } catch (Exception e) {
                    logger.warn("Failed to invite user: {} to team: {}", invitation.getEmail(), teamId, e);
                    // Continue with other invitations
                }
            }
            logger.info("Successfully sent {} invitations to team: {}", responses.size(), teamId);

            return responses;

        } catch (Exception e) {
            logger.error("Error bulk inviting members to team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public TeamResponse joinTeam(TeamRequest.JoinTeamRequest request, String username) {
        try {
            logger.info("User: {} attempting to join team with invite code: {}", username, request.getInviteCode());

            Team team = teamRepository.findByInviteCodeAndIsActiveTrue(request.getInviteCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired invite code"));

            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            // Check if user is already a member
            if (team.isMember(user)) {
                throw new BadRequestException("You are already a member of this team");
            }

            // Check if team is at max capacity
            if (team.isAtMaxCapacity()) {
                throw new BadRequestException("Team has reached maximum member capacity");
            }

            // Create team membership
            TeamMember member;
            if (team.getAutoApproveMembers()) {
                member = new TeamMember(team, user, TeamRole.MEMBER);
                member.acceptInvitation();
            } else {
                member = new TeamMember(team, user, TeamRole.MEMBER, null);
            }

            teamMemberRepository.save(member);

            // Notify team members
            String notificationMessage = team.getAutoApproveMembers() ? 
                    username + " has joined the team" : 
                    username + " has requested to join the team";

            webSocketService.sendTeamNotification(team.getId(),
                new com.trackify.dto.websocket.NotificationMessage(
                    "New Member",
                    notificationMessage,
                    "INFO",
                    LocalDateTime.now()
                ));

            logger.info("User: {} successfully joined team: {}", username, team.getId());

            return convertToTeamResponse(team, username);

        } catch (Exception e) {
            logger.error("Error joining team by user: {}", username, e);
            throw e;
        }
    }

    @Override
    public TeamResponse acceptInvitation(String inviteCode, String username) {
        try {
            logger.info("User: {} accepting invitation with code: {}", username, inviteCode);

            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            // Find pending invitation
            TeamMember invitation = teamMemberRepository.findByTeamIdAndUserId(null, user.getId())
                    .stream()
                    .filter(tm -> !tm.getIsActive() && !tm.isInvitationExpired())
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No valid invitation found"));

            Team team = invitation.getTeam();

            // Verify invite code matches
            if (!team.getInviteCode().equals(inviteCode)) {
                throw new BadRequestException("Invalid invite code");
            }

            // Accept invitation
            invitation.acceptInvitation();
            teamMemberRepository.save(invitation);

            // Notify team members
            webSocketService.sendTeamNotification(team.getId(),
                new com.trackify.dto.websocket.NotificationMessage(
                    "Invitation Accepted",
                    username + " has accepted the invitation and joined the team",
                    "SUCCESS",
                    LocalDateTime.now()
                ));

            logger.info("User: {} successfully accepted invitation to team: {}", username, team.getId());

            return convertToTeamResponse(team, username);

        } catch (Exception e) {
            logger.error("Error accepting invitation by user: {}", username, e);
            throw e;
        }
    }

    @Override
    public void removeMember(Long teamId, Long userId, String username) {
        try {
            logger.info("Removing member: {} from team: {} by user: {}", userId, teamId, username);

            validateMemberManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
            User userToRemove = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

            // Cannot remove team owner
            if (team.isOwner(userToRemove)) {
                throw new BadRequestException("Cannot remove team owner. Transfer ownership first.");
            }

            TeamMember memberToRemove = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this team"));

            teamMemberRepository.delete(memberToRemove);

            // Notify removed user
            webSocketService.sendNotificationToUser(userToRemove.getUsername(),
                new com.trackify.dto.websocket.NotificationMessage(
                    "Removed from Team",
                    "You have been removed from team: " + team.getName(),
                    "WARNING",
                    userToRemove.getUsername(),
                    LocalDateTime.now()
                ));

            // Notify team members
            webSocketService.sendTeamNotification(teamId,
                new com.trackify.dto.websocket.NotificationMessage(
                    "Member Removed",
                    userToRemove.getUsername() + " has been removed from the team",
                    "INFO",
                    LocalDateTime.now()
                ));

            logger.info("Successfully removed member: {} from team: {}", userId, teamId);

        } catch (Exception e) {
            logger.error("Error removing member: {} from team: {} by user: {}", userId, teamId, username, e);
            throw e;
        }
    }

    @Override
    public void leaveTeam(Long teamId, String username) {
        try {
            logger.info("User: {} leaving team: {}", username, teamId);

            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            Team team = getTeamEntityById(teamId);

            // Owner cannot leave team - must transfer ownership first
            if (team.isOwner(user)) {
                throw new BadRequestException("Team owner cannot leave. Transfer ownership first.");
            }

            TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this team"));

            teamMemberRepository.delete(membership);

            // Notify team members
            webSocketService.sendTeamNotification(teamId,
                new com.trackify.dto.websocket.NotificationMessage(
                    "Member Left",
                    username + " has left the team",
                    "INFO",
                    LocalDateTime.now()
                ));

            logger.info("User: {} successfully left team: {}", username, teamId);

        } catch (Exception e) {
            logger.error("Error leaving team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public TeamResponse.TeamMemberInfo updateMemberRole(Long teamId, TeamRequest.UpdateMemberRequest request, String username) {
        try {
            logger.info("Updating member role for user: {} in team: {} by user: {}", 
                    request.getUserId(), teamId, username);

            validateMemberManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
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

            TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this team"));

            TeamRole oldRole = member.getRole();
            member.setRole(request.getRole());
            member.setNotes(request.getNotes());
            member.setIsActive(request.getIsActive());

            member = teamMemberRepository.save(member);

            // Notify updated user
            webSocketService.sendNotificationToUser(userToUpdate.getUsername(),
                new com.trackify.dto.websocket.NotificationMessage(
                    "Role Updated",
                    "Your role in team " + team.getName() + " has been updated to " + request.getRole().getDisplayName(),
                    "INFO",
                    userToUpdate.getUsername(),
                    LocalDateTime.now()
                ));

            // Notify team members
            webSocketService.sendTeamNotification(teamId,
                new com.trackify.dto.websocket.NotificationMessage(
                    "Member Role Updated",
                    userToUpdate.getUsername() + "'s role has been updated from " + 
                    oldRole.getDisplayName() + " to " + request.getRole().getDisplayName(),
                    "INFO",
                    LocalDateTime.now()
                ));

            logger.info("Successfully updated member role for user: {} in team: {}", request.getUserId(), teamId);

            return convertToTeamMemberInfo(member);

        } catch (Exception e) {
            logger.error("Error updating member role for user: {} in team: {} by user: {}", 
                    request.getUserId(), teamId, username, e);
            throw e;
        }
    }

    @Override
    public TeamResponse.TeamMemberInfo promoteMember(Long teamId, Long userId, TeamRole newRole, String username) {
        TeamRequest.UpdateMemberRequest request = new TeamRequest.UpdateMemberRequest(userId, newRole);
        return updateMemberRole(teamId, request, username);
    }

    @Override
    public TeamResponse.TeamMemberInfo demoteMember(Long teamId, Long userId, TeamRole newRole, String username) {
        TeamRequest.UpdateMemberRequest request = new TeamRequest.UpdateMemberRequest(userId, newRole);
        return updateMemberRole(teamId, request, username);
    }

    @Override
    public void transferOwnership(Long teamId, TeamRequest.TransferOwnershipRequest request, String username) {
        try {
            logger.info("Transferring ownership of team: {} to user: {} by user: {}", 
                    teamId, request.getNewOwnerId(), username);

            User currentOwner = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            Team team = getTeamEntityById(teamId);

            // Verify current user is owner
            if (!team.isOwner(currentOwner)) {
                throw new ForbiddenException("Only team owner can transfer ownership");
            }

            User newOwner = userRepository.findById(request.getNewOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("New owner not found with ID: " + request.getNewOwnerId()));

            // Verify new owner is a team member
            TeamMember newOwnerMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, request.getNewOwnerId())
                    .orElseThrow(() -> new BadRequestException("New owner must be a team member"));

            TeamMember currentOwnerMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, currentOwner.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Current owner membership not found"));

            // Update team owner
            team.setOwner(newOwner);
            teamRepository.save(team);

            // Update memberships
            newOwnerMembership.setRole(TeamRole.OWNER);
            currentOwnerMembership.setRole(TeamRole.ADMIN); // Demote current owner to admin

            teamMemberRepository.save(newOwnerMembership);
            teamMemberRepository.save(currentOwnerMembership);

            // Notify new owner
            webSocketService.sendNotificationToUser(newOwner.getUsername(),
                new com.trackify.dto.websocket.NotificationMessage(
                    "Ownership Transferred",
                    "You are now the owner of team: " + team.getName(),
                    "SUCCESS",
                    newOwner.getUsername(),
                    LocalDateTime.now()
                ));

            // Notify team members
            webSocketService.sendTeamNotification(teamId,
                new com.trackify.dto.websocket.NotificationMessage(
                    "Ownership Transferred",
                    "Team ownership has been transferred from " + currentOwner.getUsername() + 
                    " to " + newOwner.getUsername(),
                    "INFO",
                    LocalDateTime.now()
                ));

            logger.info("Successfully transferred ownership of team: {} to user: {}", teamId, request.getNewOwnerId());

        } catch (Exception e) {
            logger.error("Error transferring ownership of team: {} to user: {} by user: {}", 
                    teamId, request.getNewOwnerId(), username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> searchTeams(String searchTerm, String username) {
        try {
            List<Team> teams = teamRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(searchTerm);
            
            return teams.stream()
                    .map(team -> convertToTeamResponse(team, username))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error searching teams with term: {} by user: {}", searchTerm, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamResponse> searchTeams(String searchTerm, String username, Pageable pageable) {
        try {
            Page<Team> teams = teamRepository.findTeamsByCriteria(searchTerm, null, true, null, pageable);
            
            return teams.map(team -> convertToTeamResponse(team, username));

        } catch (Exception e) {
            logger.error("Error searching teams page with term: {} by user: {}", searchTerm, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsByOwner(String ownerUsername, String requestingUsername) {
        try {
            User owner = userRepository.findByUsernameOrEmail(ownerUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + ownerUsername));

            List<Team> teams = teamRepository.findByOwnerIdAndIsActiveTrue(owner.getId());
            
            return teams.stream()
                    .map(team -> convertToTeamResponse(team, requestingUsername))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting teams by owner: {} requested by user: {}", ownerUsername, requestingUsername, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamByInviteCode(String inviteCode) {
        try {
            Team team = teamRepository.findByInviteCodeAndIsActiveTrue(inviteCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found with invite code: " + inviteCode));

            return convertToBasicTeamResponse(team);

        } catch (Exception e) {
            logger.error("Error getting team by invite code: {}", inviteCode, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse.TeamMemberInfo> getTeamMembers(Long teamId, String username) {
        try {
            validateTeamAccess(teamId, username);

            List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId);
            
            return members.stream()
                    .map(this::convertToTeamMemberInfo)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting team members for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamMemberInfo> getTeamMembers(Long teamId, String username, Pageable pageable) {
        try {
            validateTeamAccess(teamId, username);
            
            Page<TeamMember> members = getTeamMembersWithSort(teamId, pageable);
            
            return members.map(this::convertToTeamMemberInfo);
            
        } catch (Exception e) {
            logger.error("Error getting team members page for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    private Page<TeamMember> getTeamMembersWithSort(Long teamId, Pageable pageable) {
        // Check if sorting is requested
        if (!pageable.getSort().isSorted()) {
            // Default: no sorting, use original method
            return teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId, 
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        }
        
        // Get the first sort order
        Sort.Order order = pageable.getSort().iterator().next();
        String property = order.getProperty().toLowerCase();
        boolean isAscending = order.getDirection() == Sort.Direction.ASC;
        
        // Create unsorted pageable for the custom queries
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        
        // Route to appropriate method based on sort property and direction
        switch (property) {
            case "name":
            case "firstname":
            case "first_name":
            case "lastname":
            case "last_name":
                return isAscending ? 
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortByName(teamId, unsortedPageable) :
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortByNameDesc(teamId, unsortedPageable);
                    
            case "email":
                return isAscending ? 
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortByEmail(teamId, unsortedPageable) :
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortByEmailDesc(teamId, unsortedPageable);
                    
            case "role":
                return isAscending ? 
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortByRole(teamId, unsortedPageable) :
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortByRoleDesc(teamId, unsortedPageable);
                    
            case "createdat":
            case "created_at":
            case "joinedat":
            case "joined_at":
                return isAscending ? 
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortByJoinedAt(teamId, unsortedPageable) :
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortByJoinedAtDesc(teamId, unsortedPageable);
                    
            case "id":
                return isAscending ? 
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortById(teamId, unsortedPageable) :
                    teamMemberRepository.findByTeamIdAndIsActiveTrueSortByIdDesc(teamId, unsortedPageable);
                    
            default:
                // For unknown properties, fall back to unsorted
                logger.warn("Unknown sort property '{}', using default sorting by name", property);
                return teamMemberRepository.findByTeamIdAndIsActiveTrueSortByName(teamId, unsortedPageable);
        }
    }
    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse.TeamMemberInfo> searchTeamMembers(Long teamId, String searchTerm, String username) {
        try {
            validateTeamAccess(teamId, username);

            List<TeamMember> members = teamMemberRepository.searchTeamMembers(teamId, searchTerm);
            
            return members.stream()
                    .map(this::convertToTeamMemberInfo)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error searching team members for team: {} with term: {} by user: {}", 
                    teamId, searchTerm, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse.TeamMemberInfo> getTeamMembersByRole(Long teamId, TeamRole role, String username) {
        try {
            validateTeamAccess(teamId, username);

            List<TeamMember> members = teamMemberRepository.findByTeamIdAndRoleAndIsActiveTrue(teamId, role);
            
            return members.stream()
                    .map(this::convertToTeamMemberInfo)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting team members by role: {} for team: {} by user: {}", 
                    role, teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse.TeamMemberInfo getTeamMember(Long teamId, Long userId, String username) {
        try {
            validateTeamAccess(teamId, username);

            TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team member not found"));

            return convertToTeamMemberInfo(member);

        } catch (Exception e) {
            logger.error("Error getting team member: {} for team: {} by user: {}", userId, teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse.InvitationResponse> getPendingInvitations(Long teamId, String username) {
        try {
            validateMemberManagementPermission(teamId, username);

            List<TeamMember> pendingInvitations = teamMemberRepository
                    .findByTeamIdAndIsActiveFalseAndInvitationExpiresAtAfter(teamId, LocalDateTime.now());
            
            return pendingInvitations.stream()
                    .map(this::convertToInvitationResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting pending invitations for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse.InvitationResponse> getUserPendingInvitations(String username) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<TeamMember> pendingInvitations = teamMemberRepository
                    .findByUserIdAndIsActiveFalseAndInvitationExpiresAtAfter(user.getId(), LocalDateTime.now());
            
            return pendingInvitations.stream()
                    .map(this::convertToInvitationResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting pending invitations for user: {}", username, e);
            throw e;
        }
    }

    @Override
    public void cancelInvitation(Long teamId, Long invitationId, String username) {
        try {
            logger.info("Canceling invitation: {} for team: {} by user: {}", invitationId, teamId, username);

            validateMemberManagementPermission(teamId, username);

            TeamMember invitation = teamMemberRepository.findById(invitationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

            if (!invitation.getTeam().getId().equals(teamId)) {
                throw new BadRequestException("Invitation does not belong to this team");
            }

            if (invitation.getIsActive()) {
                throw new BadRequestException("Cannot cancel accepted invitation");
            }

            teamMemberRepository.delete(invitation);

            // Notify invited user
            webSocketService.sendNotificationToUser(invitation.getUser().getUsername(),
                new com.trackify.dto.websocket.NotificationMessage(
                    "Invitation Canceled",
                    "Your invitation to join team " + invitation.getTeam().getName() + " has been canceled",
                    "INFO",
                    invitation.getUser().getUsername(),
                    LocalDateTime.now()
                ));

            logger.info("Successfully canceled invitation: {} for team: {}", invitationId, teamId);

        } catch (Exception e) {
            logger.error("Error canceling invitation: {} for team: {} by user: {}", invitationId, teamId, username, e);
            throw e;
        }
    }

    @Override
    public void resendInvitation(Long teamId, Long invitationId, String username) {
        try {
            logger.info("Resending invitation: {} for team: {} by user: {}", invitationId, teamId, username);

            validateMemberManagementPermission(teamId, username);

            TeamMember invitation = teamMemberRepository.findById(invitationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

            if (!invitation.getTeam().getId().equals(teamId)) {
                throw new BadRequestException("Invitation does not belong to this team");
            }

            if (invitation.getIsActive()) {
                throw new BadRequestException("Cannot resend accepted invitation");
            }

            // Extend expiry time
            invitation.setInvitationExpiresAt(LocalDateTime.now().plusHours(72));
            teamMemberRepository.save(invitation);

            // Send notification to invited user
            webSocketService.sendNotificationToUser(invitation.getUser().getUsername(),
                new com.trackify.dto.websocket.NotificationMessage(
                    "Invitation Resent",
                    "Your invitation to join team " + invitation.getTeam().getName() + " has been resent",
                    "INFO",
                    invitation.getUser().getUsername(),
                    LocalDateTime.now()
                ));

            logger.info("Successfully resent invitation: {} for team: {}", invitationId, teamId);

        } catch (Exception e) {
            logger.error("Error resending invitation: {} for team: {} by user: {}", invitationId, teamId, username, e);
            throw e;
        }
    }

    @Override
    public String generateNewInviteCode(Long teamId, String username) {
        return regenerateInviteCode(teamId, username);
    }

    @Override
    public TeamResponse updateTeamSettings(Long teamId, TeamRequest.TeamSettingsRequest request, String username) {
        try {
            logger.info("Updating team settings for team: {} by user: {}", teamId, username);

            validateTeamManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
            
            if (request.getDescription() != null) {
                team.setDescription(request.getDescription());
            }
            if (request.getMaxMembers() != null) {
                team.setMaxMembers(request.getMaxMembers());
            }
            if (request.getCurrency() != null) {
                team.setCurrency(request.getCurrency());
            }
            if (request.getAutoApproveMembers() != null) {
                team.setAutoApproveMembers(request.getAutoApproveMembers());
            }
            if (request.getIsActive() != null) {
                team.setIsActive(request.getIsActive());
            }

            team = teamRepository.save(team);

            // Notify team members
            webSocketService.sendTeamNotification(teamId,
                new com.trackify.dto.websocket.NotificationMessage(
                    "Team Settings Updated",
                    "Team settings have been updated by " + username,
                    "INFO",
                    LocalDateTime.now()
                ));

            logger.info("Successfully updated team settings for team: {}", teamId);

            return convertToTeamResponse(team, username);

        } catch (Exception e) {
            logger.error("Error updating team settings for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public String regenerateInviteCode(Long teamId, String username) {
        try {
            logger.info("Regenerating invite code for team: {} by user: {}", teamId, username);

            validateTeamManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
            String newInviteCode = generateInviteCode();
            team.setInviteCode(newInviteCode);
            teamRepository.save(team);

            logger.info("Successfully regenerated invite code for team: {}", teamId);

            return newInviteCode;

        } catch (Exception e) {
            logger.error("Error regenerating invite code for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public void updateTeamCurrency(Long teamId, String currency, String username) {
        try {
            validateTeamManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
            team.setCurrency(currency);
            teamRepository.save(team);

            logger.info("Updated currency for team: {} to: {}", teamId, currency);

        } catch (Exception e) {
            logger.error("Error updating currency for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public void updateAutoApproveMembers(Long teamId, Boolean autoApprove, String username) {
        try {
            validateTeamManagementPermission(teamId, username);

            Team team = getTeamEntityById(teamId);
            team.setAutoApproveMembers(autoApprove);
            teamRepository.save(team);

            logger.info("Updated auto approve members for team: {} to: {}", teamId, autoApprove);

        } catch (Exception e) {
            logger.error("Error updating auto approve members for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    // Permission and validation methods implementation
    @Override
    @Transactional(readOnly = true)
    public boolean isUserMemberOfTeam(Long teamId, String username) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            
            return teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(teamId, user.getId());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserOwnerOfTeam(Long teamId, String username) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            
            return teamRepository.isUserOwnerOfTeam(teamId, user.getId());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserManageTeam(Long teamId, String username) {
        try {
            if (isUserOwnerOfTeam(teamId, username)) {
                return true;
            }

            TeamMember member = getTeamMemberEntity(teamId, username);
            return member != null && member.canManageTeam();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserManageMembers(Long teamId, String username) {
        try {
            if (isUserOwnerOfTeam(teamId, username)) {
                return true;
            }

            TeamMember member = getTeamMemberEntity(teamId, username);
            return member != null && member.canManageMembers();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserViewFinancials(Long teamId, String username) {
        try {
            TeamMember member = getTeamMemberEntity(teamId, username);
            return member != null && member.canViewFinancials();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserApproveExpenses(Long teamId, String username) {
        try {
            TeamMember member = getTeamMemberEntity(teamId, username);
            return member != null && member.canApproveExpenses();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TeamRole getUserRoleInTeam(Long teamId, String username) {
        try {
            TeamMember member = getTeamMemberEntity(teamId, username);
            return member != null ? member.getRole() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse.TeamStatistics getTeamStatistics(Long teamId, String username) {
        try {
            validateTeamAccess(teamId, username);

            Team team = getTeamEntityById(teamId);
            
            int totalMembers = team.getMemberCount();
            int activeMembers = team.getActiveMembers().size();
            int pendingInvitations = teamMemberRepository
                    .findByTeamIdAndIsActiveFalseAndInvitationExpiresAtAfter(teamId, LocalDateTime.now()).size();

            // You would implement expense and budget statistics here
            // For now, using placeholder values
            
            return new TeamResponse.TeamStatistics(
                    totalMembers,
                    activeMembers,
                    pendingInvitations,
                    java.math.BigDecimal.ZERO // Placeholder for total expenses
            );

        } catch (Exception e) {
            logger.error("Error getting team statistics for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse.TeamSummary> getUserTeamSummaries(String username) {
        try {
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<TeamMember> memberships = teamMemberRepository.findActiveMembershipsByUserId(user.getId());
            
            return memberships.stream()
                    .map(membership -> {
                        Team team = membership.getTeam();
                        return new TeamResponse.TeamSummary(
                                team.getId(),
                                team.getName(),
                                team.getDescription(),
                                team.getMemberCount(),
                                membership.getRole(),
                                team.getIsActive(),
                                membership.getJoinedAt()
                        );
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting team summaries for user: {}", username, e);
            throw e;
        }
    }

    @Override
    public void bulkUpdateMemberRoles(Long teamId, List<TeamRequest.UpdateMemberRequest> requests, String username) {
        try {
            logger.info("Bulk updating {} member roles for team: {} by user: {}", 
                    requests.size(), teamId, username);

            validateMemberManagementPermission(teamId, username);

            for (TeamRequest.UpdateMemberRequest request : requests) {
                try {
                    updateMemberRole(teamId, request, username);
                } catch (Exception e) {
                    logger.warn("Failed to update role for user: {} in team: {}", request.getUserId(), teamId, e);
                    // Continue with other updates
                }
            }

            logger.info("Successfully completed bulk role update for team: {}", teamId);

        } catch (Exception e) {
            logger.error("Error bulk updating member roles for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public void bulkRemoveMembers(Long teamId, List<Long> userIds, String username) {
        try {
            logger.info("Bulk removing {} members from team: {} by user: {}", 
                    userIds.size(), teamId, username);

            validateMemberManagementPermission(teamId, username);

            for (Long userId : userIds) {
                try {
                    removeMember(teamId, userId, username);
                } catch (Exception e) {
                    logger.warn("Failed to remove user: {} from team: {}", userId, teamId, e);
                    // Continue with other removals
                }
            }

            logger.info("Successfully completed bulk member removal for team: {}", teamId);

        } catch (Exception e) {
            logger.error("Error bulk removing members from team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void cleanupExpiredInvitations() {
        try {
            logger.info("Cleaning up expired invitations");

            teamMemberRepository.deleteByIsActiveFalseAndInvitationExpiresAtBefore(LocalDateTime.now());

            logger.info("Successfully cleaned up expired invitations");

        } catch (Exception e) {
            logger.error("Error cleaning up expired invitations", e);
        }
    }

    @Override
    @Transactional
    public void cleanupInactiveTeams() {
        try {
            logger.info("Cleaning up inactive teams");

            List<Team> teamsWithNoMembers = teamRepository.findTeamsWithNoActiveMembers();
            
            for (Team team : teamsWithNoMembers) {
                logger.info("Deactivating team with no active members: {}", team.getId());
                team.setIsActive(false);
                teamRepository.save(team);
            }

            logger.info("Successfully cleaned up {} inactive teams", teamsWithNoMembers.size());

        } catch (Exception e) {
            logger.error("Error cleaning up inactive teams", e);
        }
    }

    @Override
    public void updateMemberLastActivity(Long teamId, String username) {
        try {
            TeamMember member = getTeamMemberEntity(teamId, username);
            if (member != null) {
                member.updateLastActive();
                teamMemberRepository.save(member);
            }
        } catch (Exception e) {
            logger.warn("Error updating last activity for user: {} in team: {}", username, teamId, e);
        }
    }

    @Override
    public void notifyTeamMembers(Long teamId, String title, String message, String username) {
        try {
            webSocketService.sendTeamNotification(teamId,
                new com.trackify.dto.websocket.NotificationMessage(
                    title,
                    message,
                    "INFO",
                    LocalDateTime.now()
                ));

            logger.info("Sent notification to team: {} members by user: {}", teamId, username);

        } catch (Exception e) {
            logger.error("Error notifying team: {} members by user: {}", teamId, username, e);
        }
    }

    @Override
    public void notifyTeamMembersByRole(Long teamId, TeamRole role, String title, String message, String username) {
        try {
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndRoleAndIsActiveTrue(teamId, role);
            
            for (TeamMember member : members) {
                webSocketService.sendNotificationToUser(member.getUser().getUsername(),
                    new com.trackify.dto.websocket.NotificationMessage(
                        title,
                        message,
                        "INFO",
                        member.getUser().getUsername(),
                        LocalDateTime.now()
                    ));
            }

            logger.info("Sent notification to team: {} members with role: {} by user: {}", teamId, role, username);

        } catch (Exception e) {
            logger.error("Error notifying team: {} members with role: {} by user: {}", teamId, role, username, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse.TeamMemberInfo> exportTeamMembers(Long teamId, String username) {
        try {
            validateTeamAccess(teamId, username);
            return getTeamMembers(teamId, username);
        } catch (Exception e) {
            logger.error("Error exporting team members for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTeamMembersToCSV(Long teamId, String username) {
        try {
            logger.info("Starting CSV export for team: {} by user: {}", teamId, username);
            
            validateTeamAccess(teamId, username);
            
            // Get team and members data
            Team team = getTeamEntityById(teamId);
            List<TeamResponse.TeamMemberInfo> members = exportTeamMembers(teamId, username);
            
            logger.info("Found {} members for team: {} to export to CSV", members.size(), teamId);
            
            // Use ByteArrayOutputStream for better memory management
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, createCSVFormat())) {
                
                // Add BOM for Excel compatibility
                outputStream.write(0xEF);
                outputStream.write(0xBB);
                outputStream.write(0xBF);
                
                // Add team information header
                addTeamInfoToCSV(csvPrinter, team);
                
                // Add empty line
                csvPrinter.println();
                
                // Add summary statistics
                addSummaryToCSV(csvPrinter, members);
                
                // Add empty line
                csvPrinter.println();
                
                // Add member data headers
                addMemberHeaders(csvPrinter);
                
                // Add member data
                addMemberData(csvPrinter, members);
                
                // Add export metadata
                addExportMetadata(csvPrinter, username);
                
                csvPrinter.flush();
                
            } catch (IOException e) {
                logger.error("Error writing CSV data for team: {}", teamId, e);
                throw new RuntimeException("Failed to write CSV data", e);
            }
            
            byte[] csvBytes = outputStream.toByteArray();
            
            logger.info("CSV export completed for team: {}, size: {} bytes", teamId, csvBytes.length);
            
            return csvBytes;
            
        } catch (Exception e) {
            logger.error("Error exporting team members to CSV for team: {} by user: {}", teamId, username, e);
            throw new RuntimeException("Failed to export CSV: " + e.getMessage(), e);
        }
    }

    private CSVFormat createCSVFormat() {
        return CSVFormat.DEFAULT
                .builder()
                .setHeader() // We'll add headers manually for better control
                .setQuoteMode(QuoteMode.MINIMAL)
                .setEscape('\\')
                .setNullString("")
                .setRecordSeparator("\r\n") // Windows-style line endings for better compatibility
                .build();
    }

    private void addTeamInfoToCSV(CSVPrinter csvPrinter, Team team) throws IOException {
        // Team information section
        csvPrinter.printComment("=== TEAM INFORMATION ===");
        csvPrinter.printRecord("Field", "Value");
        csvPrinter.printRecord("Team Name", cleanCSVValue(team.getName()));
        csvPrinter.printRecord("Description", cleanCSVValue(team.getDescription()));
        csvPrinter.printRecord("Owner", cleanCSVValue(team.getOwner().getFirstName() + " " + team.getOwner().getLastName()));
        csvPrinter.printRecord("Owner Email", cleanCSVValue(team.getOwner().getEmail()));
        csvPrinter.printRecord("Currency", cleanCSVValue(team.getCurrency()));
        csvPrinter.printRecord("Maximum Members", String.valueOf(team.getMaxMembers()));
        csvPrinter.printRecord("Auto Approve Members", team.getAutoApproveMembers() ? "Yes" : "No");
        csvPrinter.printRecord("Team Status", team.getIsActive() ? "Active" : "Inactive");
        csvPrinter.printRecord("Created Date", formatDateTime(team.getCreatedAt()));
        csvPrinter.printRecord("Last Updated", formatDateTime(team.getUpdatedAt()));
    }

    private void addSummaryToCSV(CSVPrinter csvPrinter, List<TeamResponse.TeamMemberInfo> members) throws IOException {
        csvPrinter.printComment("=== MEMBER SUMMARY ===");
        
        // Calculate statistics
        long totalMembers = members.size();
        long activeMembers = members.stream().filter(TeamResponse.TeamMemberInfo::getIsActive).count();
        long pendingMembers = totalMembers - activeMembers;
        
        // Role statistics
        Map<TeamRole, Long> roleCount = members.stream()
                .collect(Collectors.groupingBy(TeamResponse.TeamMemberInfo::getRole, Collectors.counting()));
        
        // Basic statistics
        csvPrinter.printRecord("Statistic", "Count");
        csvPrinter.printRecord("Total Members", String.valueOf(totalMembers));
        csvPrinter.printRecord("Active Members", String.valueOf(activeMembers));
        csvPrinter.printRecord("Pending Invitations", String.valueOf(pendingMembers));
        
        // Role breakdown
        for (Map.Entry<TeamRole, Long> entry : roleCount.entrySet()) {
            csvPrinter.printRecord(entry.getKey().getDisplayName() + " Count", String.valueOf(entry.getValue()));
        }
    }

    private void addMemberHeaders(CSVPrinter csvPrinter) throws IOException {
        csvPrinter.printComment("=== TEAM MEMBERS DATA ===");
        csvPrinter.printRecord(
                "Member ID",
                "User ID", 
                "Username",
                "First Name",
                "Last Name",
                "Full Name",
                "Email",
                "Role",
                "Status",
                "Is Active",
                "Joined Date",
                "Joined Time",
                "Last Active Date",
                "Last Active Time",
                "Invitation Expires",
                "Notes",
                "Is Pending Invitation",
                "Days Since Joined",
                "Days Since Last Active"
        );
    }

    private void addMemberData(CSVPrinter csvPrinter, List<TeamResponse.TeamMemberInfo> members) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        
        for (TeamResponse.TeamMemberInfo member : members) {
            // Calculate additional metrics
            String daysSinceJoined = "N/A";
            String daysSinceLastActive = "N/A";
            
            if (member.getJoinedAt() != null) {
                long days = ChronoUnit.DAYS.between(member.getJoinedAt().toLocalDate(), now.toLocalDate());
                daysSinceJoined = String.valueOf(days);
            }
            
            if (member.getLastActiveAt() != null) {
                long days = ChronoUnit.DAYS.between(member.getLastActiveAt().toLocalDate(), now.toLocalDate());
                daysSinceLastActive = String.valueOf(days);
            }
            
            csvPrinter.printRecord(
                    // Basic IDs
                    member.getId() != null ? member.getId().toString() : "",
                    member.getUserId() != null ? member.getUserId().toString() : "",
                    
                    // User information
                    cleanCSVValue(member.getUsername()),
                    cleanCSVValue(member.getFirstName()),
                    cleanCSVValue(member.getLastName()),
                    cleanCSVValue(member.getFullName()),
                    cleanCSVValue(member.getEmail()),
                    
                    // Role and status
                    member.getRole() != null ? member.getRole().getDisplayName() : "",
                    member.getIsActive() ? "Active" : "Pending",
                    member.getIsActive() ? "TRUE" : "FALSE",
                    
                    // Dates and times
                    member.getJoinedAt() != null ? member.getJoinedAt().format(dateFormatter) : "",
                    member.getJoinedAt() != null ? member.getJoinedAt().format(timeFormatter) : "",
                    member.getLastActiveAt() != null ? member.getLastActiveAt().format(dateFormatter) : "",
                    member.getLastActiveAt() != null ? member.getLastActiveAt().format(timeFormatter) : "",
                    
                    // Additional information
                    member.getInvitationExpiresAt() != null ? member.getInvitationExpiresAt().format(dateTimeFormatter) : "",
                    cleanCSVValue(member.getNotes()),
                    member.isPendingInvitation() ? "TRUE" : "FALSE",
                    
                    // Calculated metrics
                    daysSinceJoined,
                    daysSinceLastActive
            );
        }
    }

    private void addExportMetadata(CSVPrinter csvPrinter, String username) throws IOException {
        csvPrinter.println();
        csvPrinter.printComment("=== EXPORT METADATA ===");
        csvPrinter.printRecord("Field", "Value");
        csvPrinter.printRecord("Export Date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        csvPrinter.printRecord("Export Time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        csvPrinter.printRecord("Export Timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        csvPrinter.printRecord("Exported By", cleanCSVValue(username));
        csvPrinter.printRecord("Export Format", "CSV");
        csvPrinter.printRecord("Character Encoding", "UTF-8");
        csvPrinter.printRecord("Application", "Trackify Team Management System");
        csvPrinter.printRecord("Version", "1.0");
    }

    private String cleanCSVValue(String value) {
        if (value == null) {
            return "";
        }
        
        // Remove problematic characters and normalize whitespace
        return value.trim()
                .replaceAll("[\r\n]+", " ") // Replace line breaks with spaces
                .replaceAll("\\s+", " ")   // Normalize multiple spaces
                .replaceAll("\"", "\"\""); // Escape quotes by doubling them
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Alternative method for simple CSV export (if you need a simpler version)
    public byte[] exportTeamMembersToSimpleCSV(Long teamId, String username) {
        try {
            logger.info("Starting simple CSV export for team: {} by user: {}", teamId, username);
            
            validateTeamAccess(teamId, username);
            List<TeamResponse.TeamMemberInfo> members = exportTeamMembers(teamId, username);
            
            StringBuilder csv = new StringBuilder();
            
            // Add BOM for Excel compatibility
            csv.append('\ufeff');
            
            // Simple header
            csv.append("Username,Full Name,Email,Role,Status,Joined Date,Last Active\n");
            
            // Data rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            for (TeamResponse.TeamMemberInfo member : members) {
                csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        escapeCSV(member.getUsername()),
                        escapeCSV(member.getFullName()),
                        escapeCSV(member.getEmail()),
                        member.getRole() != null ? member.getRole().getDisplayName() : "",
                        member.getIsActive() ? "Active" : "Pending",
                        member.getJoinedAt() != null ? member.getJoinedAt().format(formatter) : "",
                        member.getLastActiveAt() != null ? member.getLastActiveAt().format(formatter) : ""
                ));
            }
            
            return csv.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Error exporting simple CSV for team: {} by user: {}", teamId, username, e);
            throw new RuntimeException("Failed to export simple CSV: " + e.getMessage(), e);
        }
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and remove line breaks
        return value.replaceAll("\"", "\"\"").replaceAll("[\r\n]+", " ");
    }
    @Override
    @Transactional(readOnly = true)
    public byte[] exportTeamMembersToPDF(Long teamId, String username) {
        ByteArrayOutputStream outputStream = null;
        PdfWriter writer = null;
        PdfDocument pdfDocument = null;
        Document document = null;
        
        try {
            logger.info("Starting PDF export for team: {} by user: {}", teamId, username);
            
            validateTeamAccess(teamId, username);
            
            // Get team and members data
            Team team = getTeamEntityById(teamId);
            List<TeamResponse.TeamMemberInfo> members = exportTeamMembers(teamId, username);
            
            logger.info("Found {} members for team: {}", members.size(), teamId);
            
            // Create PDF in memory
            outputStream = new ByteArrayOutputStream();
            writer = new PdfWriter(outputStream);
            pdfDocument = new PdfDocument(writer);
            document = new Document(pdfDocument, PageSize.A4);
            document.setMargins(50, 50, 50, 50);
            
            // Create fonts
            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont bodyFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            
            // Define colors
            Color primaryColor = new DeviceRgb(51, 122, 183);      // Bootstrap primary blue
            Color successColor = new DeviceRgb(92, 184, 92);       // Green
            Color warningColor = new DeviceRgb(240, 173, 78);      // Orange
            Color headerBgColor = new DeviceRgb(245, 245, 245);    // Light gray
            Color textColor = new DeviceRgb(51, 51, 51);           // Dark gray
            
            // Build PDF content
            addPDFTitle(document, titleFont, primaryColor);
            addTeamInformation(document, team, headerFont, bodyFont, primaryColor, textColor);
            addMembersSummary(document, members, headerFont, bodyFont, primaryColor, textColor);
            addMembersDetailTable(document, members, headerFont, bodyFont, primaryColor, headerBgColor, textColor, successColor, warningColor);
            addPDFFooter(document, username, bodyFont, textColor);
            
            // Close document properly
            document.close();
            pdfDocument.close();
            writer.close();
            
            byte[] pdfBytes = outputStream.toByteArray();
            outputStream.close();
            
            logger.info("PDF generated successfully for team: {}, size: {} bytes", teamId, pdfBytes.length);
            
            return pdfBytes;
            
        } catch (Exception e) {
            logger.error("Error generating PDF for team: {} by user: {}", teamId, username, e);
            
            // Clean up resources
            cleanupPDFResources(document, pdfDocument, writer, outputStream);
            
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void addPDFTitle(Document document, PdfFont titleFont, Color primaryColor) {
        // Main title
        Paragraph title = new Paragraph("TEAM MEMBERS REPORT")
                .setFont(titleFont)
                .setFontSize(24)
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30)
                .setBold();
        
        document.add(title);
        
        // Add a separator line
        LineSeparator separator = new LineSeparator(new SolidLine(2f));
        separator.setMarginTop(10);
        separator.setMarginBottom(20);
        document.add(separator);
    }

    private void addTeamInformation(Document document, Team team, PdfFont headerFont, PdfFont bodyFont, 
                                   Color primaryColor, Color textColor) {
        
        // Section header
        Paragraph sectionHeader = new Paragraph("Team Information")
                .setFont(headerFont)
                .setFontSize(18)
                .setFontColor(primaryColor)
                .setMarginBottom(15)
                .setBold();
        document.add(sectionHeader);
        
        // Create info table with 2 columns
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30f, 70f}));
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.setMarginBottom(20);
        
        // Add team information rows
        addInfoTableRow(infoTable, "Team Name:", team.getName(), headerFont, bodyFont, textColor);
        addInfoTableRow(infoTable, "Description:", 
                       team.getDescription() != null && !team.getDescription().trim().isEmpty() 
                       ? team.getDescription() : "No description provided", 
                       headerFont, bodyFont, textColor);
        
        String ownerName = team.getOwner().getFirstName() + " " + team.getOwner().getLastName();
        String ownerInfo = ownerName + " (" + team.getOwner().getEmail() + ")";
        addInfoTableRow(infoTable, "Team Owner:", ownerInfo, headerFont, bodyFont, textColor);
        
        addInfoTableRow(infoTable, "Currency:", team.getCurrency() != null ? team.getCurrency() : "USD", 
                       headerFont, bodyFont, textColor);
        addInfoTableRow(infoTable, "Maximum Members:", String.valueOf(team.getMaxMembers()), 
                       headerFont, bodyFont, textColor);
        addInfoTableRow(infoTable, "Auto Approve Members:", team.getAutoApproveMembers() ? "Yes" : "No", 
                       headerFont, bodyFont, textColor);
        addInfoTableRow(infoTable, "Team Status:", team.getIsActive() ? "Active" : "Inactive", 
                       headerFont, bodyFont, textColor);
        
        String createdDate = team.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm"));
        addInfoTableRow(infoTable, "Created Date:", createdDate, headerFont, bodyFont, textColor);
        
        document.add(infoTable);
    }

    private void addMembersSummary(Document document, List<TeamResponse.TeamMemberInfo> members, 
                                  PdfFont headerFont, PdfFont bodyFont, Color primaryColor, Color textColor) {
        
        // Section header
        Paragraph summaryHeader = new Paragraph("Members Summary")
                .setFont(headerFont)
                .setFontSize(18)
                .setFontColor(primaryColor)
                .setMarginBottom(15)
                .setBold();
        document.add(summaryHeader);
        
        // Calculate statistics
        long totalMembers = members.size();
        long activeMembers = members.stream().filter(TeamResponse.TeamMemberInfo::getIsActive).count();
        long pendingMembers = totalMembers - activeMembers;
        
        // Role statistics
        Map<TeamRole, Long> roleCount = members.stream()
                .collect(Collectors.groupingBy(TeamResponse.TeamMemberInfo::getRole, Collectors.counting()));
        
        // Create summary table
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{40f, 60f}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        summaryTable.setMarginBottom(20);
        
        // Basic statistics
        addInfoTableRow(summaryTable, "Total Members:", String.valueOf(totalMembers), 
                       headerFont, bodyFont, textColor);
        addInfoTableRow(summaryTable, "Active Members:", String.valueOf(activeMembers), 
                       headerFont, bodyFont, textColor);
        addInfoTableRow(summaryTable, "Pending Invitations:", String.valueOf(pendingMembers), 
                       headerFont, bodyFont, textColor);
        
        // Role breakdown
        if (!roleCount.isEmpty()) {
            addInfoTableRow(summaryTable, "", "", headerFont, bodyFont, textColor); // Empty row for spacing
            
            for (Map.Entry<TeamRole, Long> entry : roleCount.entrySet()) {
                String roleLabel = entry.getKey().getDisplayName() + "(s):";
                addInfoTableRow(summaryTable, roleLabel, String.valueOf(entry.getValue()), 
                               headerFont, bodyFont, textColor);
            }
        }
        
        document.add(summaryTable);
    }

    private void addMembersDetailTable(Document document, List<TeamResponse.TeamMemberInfo> members, 
                                      PdfFont headerFont, PdfFont bodyFont, Color primaryColor, 
                                      Color headerBgColor, Color textColor, Color successColor, Color warningColor) {
        
        // Section header
        Paragraph membersHeader = new Paragraph("Team Members Details")
                .setFont(headerFont)
                .setFontSize(18)
                .setFontColor(primaryColor)
                .setMarginBottom(15)
                .setBold();
        document.add(membersHeader);
        
        // Create members table with 6 columns
        float[] columnWidths = {2.5f, 3.5f, 4f, 2f, 2f, 3f};
        Table membersTable = new Table(UnitValue.createPercentArray(columnWidths));
        membersTable.setWidth(UnitValue.createPercentValue(100));
        
        // Table headers
        String[] headers = {"Username", "Full Name", "Email", "Role", "Status", "Joined Date"};
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header)
                            .setFont(headerFont)
                            .setFontSize(11)
                            .setBold())
                    .setBackgroundColor(headerBgColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(10)
                    .setBorder(new SolidBorder(ColorConstants.GRAY, 1));
            membersTable.addHeaderCell(headerCell);
        }
        
        // Add member data rows
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        
        for (TeamResponse.TeamMemberInfo member : members) {
            // Username
            membersTable.addCell(createMemberDataCell(
                    member.getUsername() != null ? member.getUsername() : "N/A", 
                    bodyFont, textColor));
            
            // Full Name
            membersTable.addCell(createMemberDataCell(
                    member.getFullName() != null ? member.getFullName() : "N/A", 
                    bodyFont, textColor));
            
            // Email
            membersTable.addCell(createMemberDataCell(
                    member.getEmail() != null ? member.getEmail() : "N/A", 
                    bodyFont, textColor));
            
            // Role with color coding
            Cell roleCell = createMemberDataCell(
                    member.getRole() != null ? member.getRole().getDisplayName() : "N/A", 
                    bodyFont, textColor);
            
            // Color code roles
            if (member.getRole() != null) {
                switch (member.getRole()) {
                    case OWNER:
                        roleCell.setFontColor(new DeviceRgb(220, 53, 69)); // Red
                        roleCell.setBold();
                        break;
                    case ADMIN:
                        roleCell.setFontColor(new DeviceRgb(255, 193, 7)); // Yellow/Orange
                        roleCell.setBold();
                        break;
                    case MANAGER:
                        roleCell.setFontColor(new DeviceRgb(13, 110, 253)); // Blue
                        break;
                    default:
                        roleCell.setFontColor(textColor);
                }
            }
            membersTable.addCell(roleCell);
            
            // Status with color coding
            String status = member.getIsActive() ? "Active" : "Pending";
            Cell statusCell = createMemberDataCell(status, bodyFont, textColor);
            if (member.getIsActive()) {
                statusCell.setFontColor(successColor);
                statusCell.setBold();
            } else {
                statusCell.setFontColor(warningColor);
            }
            membersTable.addCell(statusCell);
            
            // Joined Date
            String joinedDate = "N/A";
            if (member.getJoinedAt() != null) {
                joinedDate = member.getJoinedAt().format(dateFormatter);
            }
            membersTable.addCell(createMemberDataCell(joinedDate, bodyFont, textColor));
        }
        
        document.add(membersTable);
    }

    private void addPDFFooter(Document document, String exportedBy, PdfFont bodyFont, Color textColor) {
        // Add some space before footer
        document.add(new Paragraph("\n"));
        
        // Export information
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss"));
        
        Paragraph exportInfo = new Paragraph()
                .add(new Text("Report generated on: ").setFont(bodyFont).setFontSize(10))
                .add(new Text(currentDateTime).setFont(bodyFont).setFontSize(10).setBold())
                .add(new Text("\nExported by: ").setFont(bodyFont).setFontSize(10))
                .add(new Text(exportedBy).setFont(bodyFont).setFontSize(10).setBold())
                .setFontColor(textColor)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(30);
        
        document.add(exportInfo);
        
        // Add disclaimer
        Paragraph disclaimer = new Paragraph("This report contains confidential team information. Please handle with appropriate care.")
                .setFont(bodyFont)
                .setFontSize(8)
                .setFontColor(new DeviceRgb(108, 117, 125)) // Bootstrap text-muted color
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20)
                .setItalic();
        
        document.add(disclaimer);
    }

    // Helper methods
    private void addInfoTableRow(Table table, String label, String value, PdfFont headerFont, 
                                PdfFont bodyFont, Color textColor) {
        // Label cell
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(headerFont).setFontSize(11).setBold())
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);
        
        // Value cell
        Cell valueCell = new Cell()
                .add(new Paragraph(value != null ? value : "N/A").setFont(bodyFont).setFontSize(11))
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT)
                .setFontColor(textColor);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Cell createMemberDataCell(String content, PdfFont font, Color textColor) {
        return new Cell()
                .add(new Paragraph(content != null ? content : "N/A")
                        .setFont(font)
                        .setFontSize(10))
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT)
                .setFontColor(textColor)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));
    }

    private void cleanupPDFResources(Document document, PdfDocument pdfDocument, 
                                    PdfWriter writer, ByteArrayOutputStream outputStream) {
        try {
            if (document != null) document.close();
            if (pdfDocument != null) pdfDocument.close();
            if (writer != null) writer.close();
            if (outputStream != null) outputStream.close();
        } catch (Exception cleanup) {
            logger.warn("Error during PDF resource cleanup", cleanup);
        }
    }

    // Internal utility methods
    @Override
    @Transactional(readOnly = true)
    public Team getTeamEntityById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + teamId));
    }

    @Override
    @Transactional(readOnly = true)
    public TeamMember getTeamMemberEntity(Long teamId, String username) {
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId()).orElse(null);
    }

    @Override
    public void validateTeamAccess(Long teamId, String username) {
        if (!isUserMemberOfTeam(teamId, username)) {
            throw new ForbiddenException("You are not a member of this team");
        }
    }

    @Override
    public void validateTeamManagementPermission(Long teamId, String username) {
        if (!canUserManageTeam(teamId, username)) {
            throw new ForbiddenException("You do not have permission to manage this team");
        }
    }

    @Override
    public void validateMemberManagementPermission(Long teamId, String username) {
        if (!canUserManageMembers(teamId, username)) {
            throw new ForbiddenException("You do not have permission to manage team members");
        }
    }

    // Helper methods for conversion
    private TeamResponse convertToTeamResponse(Team team, String username) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        response.setDescription(team.getDescription());
        response.setIsActive(team.getIsActive());
        response.setMaxMembers(team.getMaxMembers());
        response.setCurrentMemberCount(team.getMemberCount());
        response.setInviteCode(team.getInviteCode());
        response.setAutoApproveMembers(team.getAutoApproveMembers());
        response.setCurrency(team.getCurrency());
        response.setCreatedAt(team.getCreatedAt());
        response.setUpdatedAt(team.getUpdatedAt());

        // Set owner info
        User owner = team.getOwner();
        response.setOwner(new TeamResponse.OwnerInfo(
                owner.getId(),
                owner.getUsername(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getEmail(),
                owner.getFirstName() + " " + owner.getLastName()
        ));

        // Set current user role and permissions
        TeamMember currentUserMembership = getTeamMemberEntity(team.getId(), username);
        if (currentUserMembership != null) {
            response.setCurrentUserRole(new TeamResponse.UserRole(currentUserMembership.getRole()));
            response.setCurrentUserPermissions(new TeamResponse.UserPermissions(
                    currentUserMembership.getRole(),
                    team.isOwner(currentUserMembership.getUser())
            ));
        }

        // Set members
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId());
        response.setMembers(activeMembers.stream()
                .map(this::convertToTeamMemberInfo)
                .collect(Collectors.toList()));

        // Set statistics
        response.setStatistics(createTeamStatistics(team));

        return response;
    }

    private TeamResponse convertToBasicTeamResponse(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                new TeamResponse.OwnerInfo(
                        team.getOwner().getId(),
                        team.getOwner().getUsername(),
                        team.getOwner().getFirstName(),
                        team.getOwner().getLastName()
                ),
                team.getIsActive(),
                team.getMaxMembers(),
                team.getMemberCount(),
                team.getCurrency(),
                team.getCreatedAt()
        );
    }

    private TeamResponse.TeamMemberInfo convertToTeamMemberInfo(TeamMember member) {
        User user = member.getUser();
        
        return new TeamResponse.TeamMemberInfo(
                member.getId(),
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                member.getRole(),
                member.getIsActive(),
                member.getJoinedAt(),
                member.getLastActiveAt(),
                member.getInvitationExpiresAt(),
                member.getNotes(),
                member.isPendingInvitation()
        );
    }

    private TeamResponse.InvitationResponse convertToInvitationResponse(TeamMember invitation) {
        User invitedBy = invitation.getInvitedBy() != null ? 
                userRepository.findById(invitation.getInvitedBy()).orElse(null) : null;

        return new TeamResponse.InvitationResponse(
                invitation.getId(),
                invitation.getTeam().getInviteCode(),
                invitation.getUser().getEmail(),
                invitation.getRole(),
                invitedBy != null ? invitedBy.getUsername() : "System",
                invitation.getJoinedAt(),
                invitation.getInvitationExpiresAt()
        );
    }

    private TeamResponse.TeamStatistics createTeamStatistics(Team team) {
        int totalMembers = team.getMemberCount();
        int activeMembers = team.getActiveMembers().size();
        int pendingInvitations = teamMemberRepository
                .findByTeamIdAndIsActiveFalseAndInvitationExpiresAtAfter(team.getId(), LocalDateTime.now()).size();

        // Create role breakdown
        List<Object[]> roleStats = teamMemberRepository.getTeamMemberRoleStatistics(team.getId());
        TeamResponse.RoleBreakdown roleBreakdown = new TeamResponse.RoleBreakdown(0, 0, 0, 0, 0);
        
        for (Object[] stat : roleStats) {
            TeamRole role = (TeamRole) stat[0];
            Long count = (Long) stat[1];
            
            switch (role) {
                case OWNER -> roleBreakdown.setOwners(count.intValue());
                case ADMIN -> roleBreakdown.setAdmins(count.intValue());
                case MANAGER -> roleBreakdown.setManagers(count.intValue());
                case MEMBER -> roleBreakdown.setMembers(count.intValue());
                case VIEWER -> roleBreakdown.setViewers(count.intValue());
            }
        }

        TeamResponse.TeamStatistics statistics = new TeamResponse.TeamStatistics(
                totalMembers,
                activeMembers,
                pendingInvitations,
                java.math.BigDecimal.ZERO // Placeholder for expenses
        );
        statistics.setRoleBreakdown(roleBreakdown);
        
        return statistics;
    }

    private String generateInviteCode() {
        return "TEAM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
            