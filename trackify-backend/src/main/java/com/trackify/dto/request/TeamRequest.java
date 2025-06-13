package com.trackify.dto.request;

import com.trackify.enums.TeamRole;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Min(value = 1, message = "Maximum members must be at least 1")
    @Max(value = 1000, message = "Maximum members cannot exceed 1000")
    private Integer maxMembers = 50;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    private String currency = "USD";

    private Boolean autoApproveMembers = false;

    private String notes;

    // Constructor for team creation
    public TeamRequest(String name, String description) {
        this.name = name;
        this.description = description;
        this.maxMembers = 50;
        this.currency = "USD";
        this.autoApproveMembers = false;
    }

    // Constructor for team creation with settings
    public TeamRequest(String name, String description, Integer maxMembers, String currency, Boolean autoApproveMembers) {
        this.name = name;
        this.description = description;
        this.maxMembers = maxMembers;
        this.currency = currency;
        this.autoApproveMembers = autoApproveMembers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InviteMemberRequest {
        
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotNull(message = "Role is required")
        private TeamRole role = TeamRole.MEMBER;

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        private String notes;

        // Constructor for simple invitation
        public InviteMemberRequest(String email) {
            this.email = email;
            this.role = TeamRole.MEMBER;
        }

        // Constructor with role
        public InviteMemberRequest(String email, TeamRole role) {
            this.email = email;
            this.role = role;
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

		public String getNotes() {
			return notes;
		}

		public void setNotes(String notes) {
			this.notes = notes;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateMemberRequest {
        
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Role is required")
        private TeamRole role;

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        private String notes;

        private Boolean isActive = true;

        // Constructor for role update
        public UpdateMemberRequest(Long userId, TeamRole role) {
            this.userId = userId;
            this.role = role;
            this.isActive = true;
        }

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public TeamRole getRole() {
			return role;
		}

		public void setRole(TeamRole role) {
			this.role = role;
		}

		public String getNotes() {
			return notes;
		}

		public void setNotes(String notes) {
			this.notes = notes;
		}

		public Boolean getIsActive() {
			return isActive;
		}

		public void setIsActive(Boolean isActive) {
			this.isActive = isActive;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinTeamRequest {
        
        @NotBlank(message = "Invite code is required")
        @Size(min = 6, max = 50, message = "Invalid invite code")
        private String inviteCode;

        @Size(max = 500, message = "Message cannot exceed 500 characters")
        private String message;

        // Constructor for simple join
        public JoinTeamRequest(String inviteCode) {
            this.inviteCode = inviteCode;
        }

		public String getInviteCode() {
			return inviteCode;
		}

		public void setInviteCode(String inviteCode) {
			this.inviteCode = inviteCode;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferOwnershipRequest {
        
        @NotNull(message = "New owner user ID is required")
        private Long newOwnerId;

        @NotBlank(message = "Confirmation password is required")
        private String confirmationPassword;

        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        private String reason;

        // Constructor
        public TransferOwnershipRequest(Long newOwnerId, String confirmationPassword) {
            this.newOwnerId = newOwnerId;
            this.confirmationPassword = confirmationPassword;
        }

		public Long getNewOwnerId() {
			return newOwnerId;
		}

		public void setNewOwnerId(Long newOwnerId) {
			this.newOwnerId = newOwnerId;
		}

		public String getConfirmationPassword() {
			return confirmationPassword;
		}

		public void setConfirmationPassword(String confirmationPassword) {
			this.confirmationPassword = confirmationPassword;
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}
        
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkInviteRequest {
        
        @NotEmpty(message = "At least one invitation is required")
        @Size(max = 50, message = "Cannot invite more than 50 members at once")
        private List<InviteMemberRequest> invitations;

        @Size(max = 500, message = "Message cannot exceed 500 characters")
        private String message;

        // Constructor
        public BulkInviteRequest(List<InviteMemberRequest> invitations) {
            this.invitations = invitations;
        }

		public List<InviteMemberRequest> getInvitations() {
			return invitations;
		}

		public void setInvitations(List<InviteMemberRequest> invitations) {
			this.invitations = invitations;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
        
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamSettingsRequest {
        
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        private String description;

        @Min(value = 1, message = "Maximum members must be at least 1")
        @Max(value = 1000, message = "Maximum members cannot exceed 1000")
        private Integer maxMembers;

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
        private String currency;

        private Boolean autoApproveMembers;

        private Boolean isActive;

        // Constructor for basic settings
        public TeamSettingsRequest(String description, Integer maxMembers, String currency) {
            this.description = description;
            this.maxMembers = maxMembers;
            this.currency = currency;
        }

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Integer getMaxMembers() {
			return maxMembers;
		}

		public void setMaxMembers(Integer maxMembers) {
			this.maxMembers = maxMembers;
		}

		public String getCurrency() {
			return currency;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}

		public Boolean getAutoApproveMembers() {
			return autoApproveMembers;
		}

		public void setAutoApproveMembers(Boolean autoApproveMembers) {
			this.autoApproveMembers = autoApproveMembers;
		}

		public Boolean getIsActive() {
			return isActive;
		}

		public void setIsActive(Boolean isActive) {
			this.isActive = isActive;
		}
        
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

	public Integer getMaxMembers() {
		return maxMembers;
	}

	public void setMaxMembers(Integer maxMembers) {
		this.maxMembers = maxMembers;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Boolean getAutoApproveMembers() {
		return autoApproveMembers;
	}

	public void setAutoApproveMembers(Boolean autoApproveMembers) {
		this.autoApproveMembers = autoApproveMembers;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
    
    
}