package com.trackify.dto.request;

import com.trackify.enums.TeamRole;
import jakarta.validation.constraints.*;

import java.util.List;

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

    // Default constructor
    public TeamRequest() {
    }

    // All args constructor
    public TeamRequest(String name, String description, Integer maxMembers, String currency, Boolean autoApproveMembers, String notes) {
        this.name = name;
        this.description = description;
        this.maxMembers = maxMembers;
        this.currency = currency;
        this.autoApproveMembers = autoApproveMembers;
        this.notes = notes;
    }

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

    // Getters and Setters
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TeamRequest that = (TeamRequest) obj;
        
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (maxMembers != null ? !maxMembers.equals(that.maxMembers) : that.maxMembers != null) return false;
        if (currency != null ? !currency.equals(that.currency) : that.currency != null) return false;
        if (autoApproveMembers != null ? !autoApproveMembers.equals(that.autoApproveMembers) : that.autoApproveMembers != null) return false;
        return notes != null ? notes.equals(that.notes) : that.notes == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (maxMembers != null ? maxMembers.hashCode() : 0);
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        result = 31 * result + (autoApproveMembers != null ? autoApproveMembers.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TeamRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", maxMembers=" + maxMembers +
                ", currency='" + currency + '\'' +
                ", autoApproveMembers=" + autoApproveMembers +
                ", notes='" + notes + '\'' +
                '}';
    }

    // Inner Classes
    public static class InviteMemberRequest {
        
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotNull(message = "Role is required")
        private TeamRole role = TeamRole.MEMBER;

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        private String notes;

        // Default constructor
        public InviteMemberRequest() {
        }

        // All args constructor
        public InviteMemberRequest(String email, TeamRole role, String notes) {
            this.email = email;
            this.role = role;
            this.notes = notes;
        }

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

        // Getters and Setters
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            InviteMemberRequest that = (InviteMemberRequest) obj;
            
            if (email != null ? !email.equals(that.email) : that.email != null) return false;
            if (role != that.role) return false;
            return notes != null ? notes.equals(that.notes) : that.notes == null;
        }

        @Override
        public int hashCode() {
            int result = email != null ? email.hashCode() : 0;
            result = 31 * result + (role != null ? role.hashCode() : 0);
            result = 31 * result + (notes != null ? notes.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "InviteMemberRequest{" +
                    "email='" + email + '\'' +
                    ", role=" + role +
                    ", notes='" + notes + '\'' +
                    '}';
        }
    }

    public static class UpdateMemberRequest {
        
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Role is required")
        private TeamRole role;

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        private String notes;

        private Boolean isActive = true;

        // Default constructor
        public UpdateMemberRequest() {
        }

        // All args constructor
        public UpdateMemberRequest(Long userId, TeamRole role, String notes, Boolean isActive) {
            this.userId = userId;
            this.role = role;
            this.notes = notes;
            this.isActive = isActive;
        }

        // Constructor for role update
        public UpdateMemberRequest(Long userId, TeamRole role) {
            this.userId = userId;
            this.role = role;
            this.isActive = true;
        }

        // Getters and Setters
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            UpdateMemberRequest that = (UpdateMemberRequest) obj;
            
            if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
            if (role != that.role) return false;
            if (notes != null ? !notes.equals(that.notes) : that.notes != null) return false;
            return isActive != null ? isActive.equals(that.isActive) : that.isActive == null;
        }

        @Override
        public int hashCode() {
            int result = userId != null ? userId.hashCode() : 0;
            result = 31 * result + (role != null ? role.hashCode() : 0);
            result = 31 * result + (notes != null ? notes.hashCode() : 0);
            result = 31 * result + (isActive != null ? isActive.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "UpdateMemberRequest{" +
                    "userId=" + userId +
                    ", role=" + role +
                    ", notes='" + notes + '\'' +
                    ", isActive=" + isActive +
                    '}';
        }
    }

    public static class JoinTeamRequest {
        
        @NotBlank(message = "Invite code is required")
        @Size(min = 6, max = 50, message = "Invalid invite code")
        private String inviteCode;

        @Size(max = 500, message = "Message cannot exceed 500 characters")
        private String message;

        // Default constructor
        public JoinTeamRequest() {
        }

        // All args constructor
        public JoinTeamRequest(String inviteCode, String message) {
            this.inviteCode = inviteCode;
            this.message = message;
        }

        // Constructor for simple join
        public JoinTeamRequest(String inviteCode) {
            this.inviteCode = inviteCode;
        }

        // Getters and Setters
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            JoinTeamRequest that = (JoinTeamRequest) obj;
            
            if (inviteCode != null ? !inviteCode.equals(that.inviteCode) : that.inviteCode != null) return false;
            return message != null ? message.equals(that.message) : that.message == null;
        }

        @Override
        public int hashCode() {
            int result = inviteCode != null ? inviteCode.hashCode() : 0;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "JoinTeamRequest{" +
                    "inviteCode='" + inviteCode + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    public static class TransferOwnershipRequest {
        
        @NotNull(message = "New owner user ID is required")
        private Long newOwnerId;

        @NotBlank(message = "Confirmation password is required")
        private String confirmationPassword;

        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        private String reason;

        // Default constructor
        public TransferOwnershipRequest() {
        }

        // All args constructor
        public TransferOwnershipRequest(Long newOwnerId, String confirmationPassword, String reason) {
            this.newOwnerId = newOwnerId;
            this.confirmationPassword = confirmationPassword;
            this.reason = reason;
        }

        // Constructor
        public TransferOwnershipRequest(Long newOwnerId, String confirmationPassword) {
            this.newOwnerId = newOwnerId;
            this.confirmationPassword = confirmationPassword;
        }

        // Getters and Setters
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            TransferOwnershipRequest that = (TransferOwnershipRequest) obj;
            
            if (newOwnerId != null ? !newOwnerId.equals(that.newOwnerId) : that.newOwnerId != null) return false;
            if (confirmationPassword != null ? !confirmationPassword.equals(that.confirmationPassword) : that.confirmationPassword != null) return false;
            return reason != null ? reason.equals(that.reason) : that.reason == null;
        }

        @Override
        public int hashCode() {
            int result = newOwnerId != null ? newOwnerId.hashCode() : 0;
            result = 31 * result + (confirmationPassword != null ? confirmationPassword.hashCode() : 0);
            result = 31 * result + (reason != null ? reason.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TransferOwnershipRequest{" +
                    "newOwnerId=" + newOwnerId +
                    ", confirmationPassword='[PROTECTED]'" +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }

    public static class BulkInviteRequest {
        
        @NotEmpty(message = "At least one invitation is required")
        @Size(max = 50, message = "Cannot invite more than 50 members at once")
        private List<InviteMemberRequest> invitations;

        @Size(max = 500, message = "Message cannot exceed 500 characters")
        private String message;

        // Default constructor
        public BulkInviteRequest() {
        }

        // All args constructor
        public BulkInviteRequest(List<InviteMemberRequest> invitations, String message) {
            this.invitations = invitations;
            this.message = message;
        }

        // Constructor
        public BulkInviteRequest(List<InviteMemberRequest> invitations) {
            this.invitations = invitations;
        }

        // Getters and Setters
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            BulkInviteRequest that = (BulkInviteRequest) obj;
            
            if (invitations != null ? !invitations.equals(that.invitations) : that.invitations != null) return false;
            return message != null ? message.equals(that.message) : that.message == null;
        }

        @Override
        public int hashCode() {
            int result = invitations != null ? invitations.hashCode() : 0;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "BulkInviteRequest{" +
                    "invitations=" + invitations +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

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

        // Default constructor
        public TeamSettingsRequest() {
        }

        // All args constructor
        public TeamSettingsRequest(String description, Integer maxMembers, String currency, Boolean autoApproveMembers, Boolean isActive) {
            this.description = description;
            this.maxMembers = maxMembers;
            this.currency = currency;
            this.autoApproveMembers = autoApproveMembers;
            this.isActive = isActive;
        }

        // Constructor for basic settings
        public TeamSettingsRequest(String description, Integer maxMembers, String currency) {
            this.description = description;
            this.maxMembers = maxMembers;
            this.currency = currency;
        }

        // Getters and Setters
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            TeamSettingsRequest that = (TeamSettingsRequest) obj;
            
            if (description != null ? !description.equals(that.description) : that.description != null) return false;
            if (maxMembers != null ? !maxMembers.equals(that.maxMembers) : that.maxMembers != null) return false;
            if (currency != null ? !currency.equals(that.currency) : that.currency != null) return false;
            if (autoApproveMembers != null ? !autoApproveMembers.equals(that.autoApproveMembers) : that.autoApproveMembers != null) return false;
            return isActive != null ? isActive.equals(that.isActive) : that.isActive == null;
        }

        @Override
        public int hashCode() {
            int result = description != null ? description.hashCode() : 0;
            result = 31 * result + (maxMembers != null ? maxMembers.hashCode() : 0);
            result = 31 * result + (currency != null ? currency.hashCode() : 0);
            result = 31 * result + (autoApproveMembers != null ? autoApproveMembers.hashCode() : 0);
            result = 31 * result + (isActive != null ? isActive.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TeamSettingsRequest{" +
                    "description='" + description + '\'' +
                    ", maxMembers=" + maxMembers +
                    ", currency='" + currency + '\'' +
                    ", autoApproveMembers=" + autoApproveMembers +
                    ", isActive=" + isActive +
                    '}';
        }
    }
}