package com.trackify.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trackify.enums.TeamRole;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    
    private Long id;
    private String name;
    private String description;
    private OwnerInfo owner;
    private Boolean isActive;
    private Integer maxMembers;
    private Integer currentMemberCount;
    private String inviteCode;
    private Boolean autoApproveMembers;
    private String currency;
    private List<TeamMemberInfo> members;
    private TeamStatistics statistics;
    private UserRole currentUserRole;
    private UserPermissions currentUserPermissions;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    

    public TeamResponse(Long id, String name, String description, OwnerInfo owner, Boolean isActive, Integer maxMembers,
			Integer currentMemberCount, String inviteCode, Boolean autoApproveMembers, String currency,
			List<TeamMemberInfo> members, TeamStatistics statistics, UserRole currentUserRole,
			UserPermissions currentUserPermissions, LocalDateTime createdAt, LocalDateTime updatedAt) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
		this.owner = owner;
		this.isActive = isActive;
		this.maxMembers = maxMembers;
		this.currentMemberCount = currentMemberCount;
		this.inviteCode = inviteCode;
		this.autoApproveMembers = autoApproveMembers;
		this.currency = currency;
		this.members = members;
		this.statistics = statistics;
		this.currentUserRole = currentUserRole;
		this.currentUserPermissions = currentUserPermissions;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}
    
    

	public TeamResponse(Long id, String name, String description, OwnerInfo owner, Boolean isActive, Integer maxMembers,
			Integer currentMemberCount, String inviteCode, Boolean autoApproveMembers, String currency,
			LocalDateTime createdAt, LocalDateTime updatedAt) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
		this.owner = owner;
		this.isActive = isActive;
		this.maxMembers = maxMembers;
		this.currentMemberCount = currentMemberCount;
		this.inviteCode = inviteCode;
		this.autoApproveMembers = autoApproveMembers;
		this.currency = currency;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}



	// Constructor for basic team info
    public TeamResponse(Long id, String name, String description, OwnerInfo owner, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.isActive = isActive;
    }

    // Constructor for detailed team info
    public TeamResponse(Long id, String name, String description, OwnerInfo owner, 
                       Boolean isActive, Integer maxMembers, Integer currentMemberCount,
                       String currency, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.isActive = isActive;
        this.maxMembers = maxMembers;
        this.currentMemberCount = currentMemberCount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public TeamResponse(Long id, String name, String description, Boolean isActive, Integer maxMembers,
			Integer currentMemberCount, String inviteCode, Boolean autoApproveMembers, String currency,
			LocalDateTime createdAt, LocalDateTime updatedAt) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
		this.isActive = isActive;
		this.maxMembers = maxMembers;
		this.currentMemberCount = currentMemberCount;
		this.inviteCode = inviteCode;
		this.autoApproveMembers = autoApproveMembers;
		this.currency = currency;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public TeamResponse() {
		
	}

	@Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfo {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String fullName;
        
        
        
        public OwnerInfo(Long id, String username, String firstName, String lastName, String email, String fullName) {
			super();
			this.id = id;
			this.username = username;
			this.firstName = firstName;
			this.lastName = lastName;
			this.email = email;
			this.fullName = fullName;
		}

		// Constructor for basic owner info
        public OwnerInfo(Long id, String username, String firstName, String lastName) {
            this.id = id;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.fullName = firstName + " " + lastName;
        }

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberInfo {
        private Long id;
        private Long userId;
        private String username;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private TeamRole role;
        private Boolean isActive;
        private Long invitedBy;
        private String invitedByUsername;
        
        
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime joinedAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastActiveAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime invitationExpiresAt;
        
        private String notes;
        private Boolean isPending;
        
        
        
        public TeamMemberInfo(Long id, Long userId, String username, String firstName,
        		String lastName, String fullName, String email, TeamRole role,
        		Boolean isActive, LocalDateTime joinedAt, LocalDateTime lastActiveAt,
        		LocalDateTime invitationExpiresAt, String notes, Boolean isPending) {
        	this.id = id;
        	this.userId = userId;
        	this.username = username;
        	this.firstName = firstName;
        	this.lastName = lastName;
        	this.fullName = fullName;
        	this.email = email;
        	this.role = role;
        	this.isActive = isActive;
        	this.joinedAt = joinedAt;
        	this.lastActiveAt = lastActiveAt;
        	this.invitationExpiresAt = invitationExpiresAt;
        	this.notes = notes;
        	this.isPending = isPending;
        }


        // Constructor for active member
        public TeamMemberInfo(Long id, Long userId, String username, String firstName, 
                             String lastName, String email, TeamRole role, Boolean isActive, 
                             LocalDateTime joinedAt) {
            this.id = id;
            this.userId = userId;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.fullName = firstName + " " + lastName;
            this.email = email;
            this.role = role;
            this.isActive = isActive;
            this.joinedAt = joinedAt;
            this.isPending = !isActive;
        }

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
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

		public String getInvitedByUsername() {
			return invitedByUsername;
		}

		public void setInvitedByUsername(String invitedByUsername) {
			this.invitedByUsername = invitedByUsername;
		}

		public LocalDateTime getJoinedAt() {
			return joinedAt;
		}

		public void setJoinedAt(LocalDateTime joinedAt) {
			this.joinedAt = joinedAt;
		}

		public LocalDateTime getLastActiveAt() {
			return lastActiveAt;
		}

		public void setLastActiveAt(LocalDateTime lastActiveAt) {
			this.lastActiveAt = lastActiveAt;
		}

		public LocalDateTime getInvitationExpiresAt() {
			return invitationExpiresAt;
		}

		public void setInvitationExpiresAt(LocalDateTime invitationExpiresAt) {
			this.invitationExpiresAt = invitationExpiresAt;
		}

		public String getNotes() {
			return notes;
		}

		public void setNotes(String notes) {
			this.notes = notes;
		}

		public Boolean getIsPending() {
			return isPending;
		}

		public void setIsPending(Boolean isPending) {
			this.isPending = isPending;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamStatistics {
        private Integer totalMembers;
        private Integer activeMembers;
        private Integer pendingInvitations;
        private BigDecimal totalExpenses;
        private Long totalExpenseCount;
        private BigDecimal monthlyExpenses;
        private Integer activeBudgets;
        private RoleBreakdown roleBreakdown;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastActivity;

        // Constructor
        public TeamStatistics(Integer totalMembers, Integer activeMembers, 
                             Integer pendingInvitations, BigDecimal totalExpenses) {
            this.totalMembers = totalMembers;
            this.activeMembers = activeMembers;
            this.pendingInvitations = pendingInvitations;
            this.totalExpenses = totalExpenses;
        }

		public Integer getTotalMembers() {
			return totalMembers;
		}

		public void setTotalMembers(Integer totalMembers) {
			this.totalMembers = totalMembers;
		}

		public Integer getActiveMembers() {
			return activeMembers;
		}

		public void setActiveMembers(Integer activeMembers) {
			this.activeMembers = activeMembers;
		}

		public Integer getPendingInvitations() {
			return pendingInvitations;
		}

		public void setPendingInvitations(Integer pendingInvitations) {
			this.pendingInvitations = pendingInvitations;
		}

		public BigDecimal getTotalExpenses() {
			return totalExpenses;
		}

		public void setTotalExpenses(BigDecimal totalExpenses) {
			this.totalExpenses = totalExpenses;
		}

		public Long getTotalExpenseCount() {
			return totalExpenseCount;
		}

		public void setTotalExpenseCount(Long totalExpenseCount) {
			this.totalExpenseCount = totalExpenseCount;
		}

		public BigDecimal getMonthlyExpenses() {
			return monthlyExpenses;
		}

		public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
			this.monthlyExpenses = monthlyExpenses;
		}

		public Integer getActiveBudgets() {
			return activeBudgets;
		}

		public void setActiveBudgets(Integer activeBudgets) {
			this.activeBudgets = activeBudgets;
		}

		public RoleBreakdown getRoleBreakdown() {
			return roleBreakdown;
		}

		public void setRoleBreakdown(RoleBreakdown roleBreakdown) {
			this.roleBreakdown = roleBreakdown;
		}

		public LocalDateTime getLastActivity() {
			return lastActivity;
		}

		public void setLastActivity(LocalDateTime lastActivity) {
			this.lastActivity = lastActivity;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleBreakdown {
        private Integer owners;
        private Integer admins;
        private Integer managers;
        private Integer members;
        private Integer viewers;

        // Constructor
        public RoleBreakdown(Integer owners, Integer admins, Integer managers, 
                            Integer members, Integer viewers) {
            this.owners = owners;
            this.admins = admins;
            this.managers = managers;
            this.members = members;
            this.viewers = viewers;
        }

		public Integer getOwners() {
			return owners;
		}

		public void setOwners(Integer owners) {
			this.owners = owners;
		}

		public Integer getAdmins() {
			return admins;
		}

		public void setAdmins(Integer admins) {
			this.admins = admins;
		}

		public Integer getManagers() {
			return managers;
		}

		public void setManagers(Integer managers) {
			this.managers = managers;
		}

		public Integer getMembers() {
			return members;
		}

		public void setMembers(Integer members) {
			this.members = members;
		}

		public Integer getViewers() {
			return viewers;
		}

		public void setViewers(Integer viewers) {
			this.viewers = viewers;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRole {
        private TeamRole role;
        private String roleDisplayName;
        private Boolean isOwner;
        private Boolean isAdmin;
        private Boolean isManager;
        private Boolean isMember;
        private Boolean isViewer;

        // Constructor
        public UserRole(TeamRole role) {
            this.role = role;
            this.roleDisplayName = role.getDisplayName();
            this.isOwner = role == TeamRole.OWNER;
            this.isAdmin = role == TeamRole.ADMIN;
            this.isManager = role == TeamRole.MANAGER;
            this.isMember = role == TeamRole.MEMBER;
            this.isViewer = role == TeamRole.VIEWER;
        }

		public TeamRole getRole() {
			return role;
		}

		public void setRole(TeamRole role) {
			this.role = role;
		}

		public String getRoleDisplayName() {
			return roleDisplayName;
		}

		public void setRoleDisplayName(String roleDisplayName) {
			this.roleDisplayName = roleDisplayName;
		}

		public Boolean getIsOwner() {
			return isOwner;
		}

		public void setIsOwner(Boolean isOwner) {
			this.isOwner = isOwner;
		}

		public Boolean getIsAdmin() {
			return isAdmin;
		}

		public void setIsAdmin(Boolean isAdmin) {
			this.isAdmin = isAdmin;
		}

		public Boolean getIsManager() {
			return isManager;
		}

		public void setIsManager(Boolean isManager) {
			this.isManager = isManager;
		}

		public Boolean getIsMember() {
			return isMember;
		}

		public void setIsMember(Boolean isMember) {
			this.isMember = isMember;
		}

		public Boolean getIsViewer() {
			return isViewer;
		}

		public void setIsViewer(Boolean isViewer) {
			this.isViewer = isViewer;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPermissions {
        private Boolean canManageTeam;
        private Boolean canManageMembers;
        private Boolean canViewFinancials;
        private Boolean canApproveExpenses;
        private Boolean canCreateExpenses;
        private Boolean canInviteMembers;
        private Boolean canRemoveMembers;
        private Boolean canChangeSettings;
        private Boolean canDeleteTeam;
        private Boolean canTransferOwnership;

        // Constructor from role
        public UserPermissions(TeamRole role, Boolean isOwner) {
            this.canManageTeam = role.canManageTeam();
            this.canManageMembers = role.canManageMembers();
            this.canViewFinancials = role.canViewFinancials();
            this.canApproveExpenses = role.canApproveExpenses();
            this.canCreateExpenses = role.canCreateExpenses();
            this.canInviteMembers = role.canManageMembers();
            this.canRemoveMembers = role.canManageMembers();
            this.canChangeSettings = role.canManageTeam();
            this.canDeleteTeam = isOwner;
            this.canTransferOwnership = isOwner;
        }

		public Boolean getCanManageTeam() {
			return canManageTeam;
		}

		public void setCanManageTeam(Boolean canManageTeam) {
			this.canManageTeam = canManageTeam;
		}

		public Boolean getCanManageMembers() {
			return canManageMembers;
		}

		public void setCanManageMembers(Boolean canManageMembers) {
			this.canManageMembers = canManageMembers;
		}

		public Boolean getCanViewFinancials() {
			return canViewFinancials;
		}

		public void setCanViewFinancials(Boolean canViewFinancials) {
			this.canViewFinancials = canViewFinancials;
		}

		public Boolean getCanApproveExpenses() {
			return canApproveExpenses;
		}

		public void setCanApproveExpenses(Boolean canApproveExpenses) {
			this.canApproveExpenses = canApproveExpenses;
		}

		public Boolean getCanCreateExpenses() {
			return canCreateExpenses;
		}

		public void setCanCreateExpenses(Boolean canCreateExpenses) {
			this.canCreateExpenses = canCreateExpenses;
		}

		public Boolean getCanInviteMembers() {
			return canInviteMembers;
		}

		public void setCanInviteMembers(Boolean canInviteMembers) {
			this.canInviteMembers = canInviteMembers;
		}

		public Boolean getCanRemoveMembers() {
			return canRemoveMembers;
		}

		public void setCanRemoveMembers(Boolean canRemoveMembers) {
			this.canRemoveMembers = canRemoveMembers;
		}

		public Boolean getCanChangeSettings() {
			return canChangeSettings;
		}

		public void setCanChangeSettings(Boolean canChangeSettings) {
			this.canChangeSettings = canChangeSettings;
		}

		public Boolean getCanDeleteTeam() {
			return canDeleteTeam;
		}

		public void setCanDeleteTeam(Boolean canDeleteTeam) {
			this.canDeleteTeam = canDeleteTeam;
		}

		public Boolean getCanTransferOwnership() {
			return canTransferOwnership;
		}

		public void setCanTransferOwnership(Boolean canTransferOwnership) {
			this.canTransferOwnership = canTransferOwnership;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvitationResponse {
        private Long invitationId;
        private String inviteCode;
        private String invitedUserEmail;
        private TeamRole role;
        private String invitedByUsername;
        private Boolean isAccepted;
        private Boolean isExpired;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime invitedAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime expiresAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime acceptedAt;

        // Constructor
        public InvitationResponse(Long invitationId, String inviteCode, String invitedUserEmail,
                                 TeamRole role, String invitedByUsername, LocalDateTime invitedAt,
                                 LocalDateTime expiresAt) {
            this.invitationId = invitationId;
            this.inviteCode = inviteCode;
            this.invitedUserEmail = invitedUserEmail;
            this.role = role;
            this.invitedByUsername = invitedByUsername;
            this.invitedAt = invitedAt;
            this.expiresAt = expiresAt;
            this.isAccepted = false;
            this.isExpired = LocalDateTime.now().isAfter(expiresAt);
        }

		public Long getInvitationId() {
			return invitationId;
		}

		public void setInvitationId(Long invitationId) {
			this.invitationId = invitationId;
		}

		public String getInviteCode() {
			return inviteCode;
		}

		public void setInviteCode(String inviteCode) {
			this.inviteCode = inviteCode;
		}

		public String getInvitedUserEmail() {
			return invitedUserEmail;
		}

		public void setInvitedUserEmail(String invitedUserEmail) {
			this.invitedUserEmail = invitedUserEmail;
		}

		public TeamRole getRole() {
			return role;
		}

		public void setRole(TeamRole role) {
			this.role = role;
		}

		public String getInvitedByUsername() {
			return invitedByUsername;
		}

		public void setInvitedByUsername(String invitedByUsername) {
			this.invitedByUsername = invitedByUsername;
		}

		public Boolean getIsAccepted() {
			return isAccepted;
		}

		public void setIsAccepted(Boolean isAccepted) {
			this.isAccepted = isAccepted;
		}

		public Boolean getIsExpired() {
			return isExpired;
		}

		public void setIsExpired(Boolean isExpired) {
			this.isExpired = isExpired;
		}

		public LocalDateTime getInvitedAt() {
			return invitedAt;
		}

		public void setInvitedAt(LocalDateTime invitedAt) {
			this.invitedAt = invitedAt;
		}

		public LocalDateTime getExpiresAt() {
			return expiresAt;
		}

		public void setExpiresAt(LocalDateTime expiresAt) {
			this.expiresAt = expiresAt;
		}

		public LocalDateTime getAcceptedAt() {
			return acceptedAt;
		}

		public void setAcceptedAt(LocalDateTime acceptedAt) {
			this.acceptedAt = acceptedAt;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamSummary {
        private Long id;
        private String name;
        private String description;
        private Integer memberCount;
        private TeamRole userRole;
        private Boolean isActive;
        private String currency;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime joinedAt;

        // Constructor
        public TeamSummary(Long id, String name, String description, Integer memberCount,
                          TeamRole userRole, Boolean isActive, LocalDateTime joinedAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.memberCount = memberCount;
            this.userRole = userRole;
            this.isActive = isActive;
            this.joinedAt = joinedAt;
        }
    }

	public Long getId() {
		return id;
	}



	public void setId(Long id) {
		this.id = id;
	}



	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}



	public String getDescription() {
		return description;
	}



	public void setDescription(String description) {
		this.description = description;
	}



	public OwnerInfo getOwner() {
		return owner;
	}



	public void setOwner(OwnerInfo owner) {
		this.owner = owner;
	}



	public Boolean getIsActive() {
		return isActive;
	}



	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}



	public Integer getMaxMembers() {
		return maxMembers;
	}



	public void setMaxMembers(Integer maxMembers) {
		this.maxMembers = maxMembers;
	}



	public Integer getCurrentMemberCount() {
		return currentMemberCount;
	}



	public void setCurrentMemberCount(Integer currentMemberCount) {
		this.currentMemberCount = currentMemberCount;
	}



	public String getInviteCode() {
		return inviteCode;
	}



	public void setInviteCode(String inviteCode) {
		this.inviteCode = inviteCode;
	}



	public Boolean getAutoApproveMembers() {
		return autoApproveMembers;
	}



	public void setAutoApproveMembers(Boolean autoApproveMembers) {
		this.autoApproveMembers = autoApproveMembers;
	}



	public String getCurrency() {
		return currency;
	}



	public void setCurrency(String currency) {
		this.currency = currency;
	}



	public List<TeamMemberInfo> getMembers() {
		return members;
	}



	public void setMembers(List<TeamMemberInfo> members) {
		this.members = members;
	}



	public TeamStatistics getStatistics() {
		return statistics;
	}



	public void setStatistics(TeamStatistics statistics) {
		this.statistics = statistics;
	}



	public UserRole getCurrentUserRole() {
		return currentUserRole;
	}



	public void setCurrentUserRole(UserRole currentUserRole) {
		this.currentUserRole = currentUserRole;
	}



	public UserPermissions getCurrentUserPermissions() {
		return currentUserPermissions;
	}



	public void setCurrentUserPermissions(UserPermissions currentUserPermissions) {
		this.currentUserPermissions = currentUserPermissions;
	}



	public LocalDateTime getCreatedAt() {
		return createdAt;
	}



	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}



	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}



	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
    
    
}