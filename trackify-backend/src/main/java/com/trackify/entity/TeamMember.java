package com.trackify.entity;

import com.trackify.enums.TeamRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_id", "user_id"})
})
@EntityListeners(AuditingEntityListener.class)

public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamRole role = TeamRole.MEMBER;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "invited_by")
    private Long invitedBy;

    @Column(name = "invitation_accepted_at")
    private LocalDateTime invitationAcceptedAt;

    @Column(name = "invitation_expires_at")
    private LocalDateTime invitationExpiresAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Custom constructors
    public TeamMember(Team team, User user, TeamRole role) {
        this.team = team;
        this.user = user;
        this.role = role;
        this.isActive = true;
        this.invitationAcceptedAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
    }

    public TeamMember(Team team, User user) {
        this.team = team;
        this.user = user;
        this.role = TeamRole.MEMBER;
        this.isActive = true;
        this.invitationAcceptedAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
    }

    public TeamMember(Team team, User user, TeamRole role, Long invitedBy) {
        this.team = team;
        this.user = user;
        this.role = role;
        this.invitedBy = invitedBy;
        this.isActive = false; // Will be activated when invitation is accepted
        this.invitationExpiresAt = LocalDateTime.now().plusHours(72); // 3 days expiry
    }

    // Helper methods
    public boolean isOwner() {
        return TeamRole.OWNER.equals(this.role);
    }

    public boolean isAdmin() {
        return TeamRole.ADMIN.equals(this.role);
    }

    public boolean isManager() {
        return TeamRole.MANAGER.equals(this.role);
    }

    public boolean isMember() {
        return TeamRole.MEMBER.equals(this.role);
    }

    public boolean isViewer() {
        return TeamRole.VIEWER.equals(this.role);
    }

    public boolean hasPermission(TeamRole requiredRole) {
        return this.role.hasPermission(requiredRole);
    }

    public boolean canManageTeam() {
        return this.role.canManageTeam();
    }

    public boolean canManageMembers() {
        return this.role.canManageMembers();
    }

    public boolean canViewFinancials() {
        return this.role.canViewFinancials();
    }

    public boolean canApproveExpenses() {
        return this.role.canApproveExpenses();
    }

    public boolean canCreateExpenses() {
        return this.role.canCreateExpenses();
    }

    public boolean isInvitationExpired() {
        return invitationExpiresAt != null && LocalDateTime.now().isAfter(invitationExpiresAt);
    }

    public boolean isPendingInvitation() {
        return !isActive && invitationExpiresAt != null && !isInvitationExpired();
    }

    public void acceptInvitation() {
        this.isActive = true;
        this.invitationAcceptedAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.invitationExpiresAt = null; // Clear expiry once accepted
    }

    public void updateLastActive() {
        this.lastActiveAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
        this.lastActiveAt = LocalDateTime.now();
    }

    public boolean canPromoteTo(TeamRole newRole) {
        // Owner cannot be changed
        if (this.role == TeamRole.OWNER) return false;
        
        // Cannot promote to owner
        if (newRole == TeamRole.OWNER) return false;
        
        return true;
    }

    public boolean canDemoteTo(TeamRole newRole) {
        // Owner cannot be changed
        if (this.role == TeamRole.OWNER) return false;
        
        // Cannot demote to owner
        if (newRole == TeamRole.OWNER) return false;
        
        return true;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public TeamRole getRole() {
		return role;
	}

	public void setRole(TeamRole role) {
		this.role = role;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Long getInvitedBy() {
		return invitedBy;
	}

	public void setInvitedBy(Long invitedBy) {
		this.invitedBy = invitedBy;
	}

	public LocalDateTime getInvitationAcceptedAt() {
		return invitationAcceptedAt;
	}

	public void setInvitationAcceptedAt(LocalDateTime invitationAcceptedAt) {
		this.invitationAcceptedAt = invitationAcceptedAt;
	}

	public LocalDateTime getInvitationExpiresAt() {
		return invitationExpiresAt;
	}

	public void setInvitationExpiresAt(LocalDateTime invitationExpiresAt) {
		this.invitationExpiresAt = invitationExpiresAt;
	}

	public LocalDateTime getLastActiveAt() {
		return lastActiveAt;
	}

	public void setLastActiveAt(LocalDateTime lastActiveAt) {
		this.lastActiveAt = lastActiveAt;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public void setJoinedAt(LocalDateTime joinedAt) {
		this.joinedAt = joinedAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public TeamMember() {
	
	}

	public TeamMember(Long id, Team team, User user, TeamRole role, Boolean isActive, Long invitedBy,
			LocalDateTime invitationAcceptedAt, LocalDateTime invitationExpiresAt, LocalDateTime lastActiveAt,
			String notes, LocalDateTime joinedAt, LocalDateTime updatedAt) {
		super();
		this.id = id;
		this.team = team;
		this.user = user;
		this.role = role;
		this.isActive = isActive;
		this.invitedBy = invitedBy;
		this.invitationAcceptedAt = invitationAcceptedAt;
		this.invitationExpiresAt = invitationExpiresAt;
		this.lastActiveAt = lastActiveAt;
		this.notes = notes;
		this.joinedAt = joinedAt;
		this.updatedAt = updatedAt;
	}
    
	
    
}