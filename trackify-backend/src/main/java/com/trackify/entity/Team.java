package com.trackify.entity;

import com.trackify.enums.TeamRole;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
@EntityListeners(AuditingEntityListener.class)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "max_members")
    private Integer maxMembers = 50;

    @Column(name = "invite_code", unique = true, length = 50)
    private String inviteCode;

    @Column(name = "auto_approve_members")
    private Boolean autoApproveMembers = false;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TeamMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Expense> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Budget> budgets = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor (REQUIRED by Hibernate)
    public Team() {
    }

    // All args constructor
    public Team(Long id, String name, String description, User owner, Boolean isActive, Integer maxMembers,
               String inviteCode, Boolean autoApproveMembers, String currency, List<TeamMember> members,
               List<Expense> expenses, List<Budget> budgets, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.isActive = isActive;
        this.maxMembers = maxMembers;
        this.inviteCode = inviteCode;
        this.autoApproveMembers = autoApproveMembers;
        this.currency = currency;
        this.members = members;
        this.expenses = expenses;
        this.budgets = budgets;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Custom constructors
    public Team(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.isActive = true;
        this.autoApproveMembers = false;
        this.currency = "USD";
        this.maxMembers = 50;
    }

    public Team(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.isActive = true;
        this.autoApproveMembers = false;
        this.currency = "USD";
        this.maxMembers = 50;
    }

    // Getters and Setters
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
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

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }

    public List<Budget> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<Budget> budgets) {
        this.budgets = budgets;
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

    // Helper methods
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }

    public boolean isOwner(User user) {
        return this.owner != null && this.owner.getId().equals(user.getId());
    }

    public boolean isMember(User user) {
        return members != null && members.stream()
                .anyMatch(member -> member.getUser().getId().equals(user.getId()));
    }

    public TeamMember getTeamMember(User user) {
        return members != null ? members.stream()
                .filter(member -> member.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null) : null;
    }

    public boolean hasRole(User user, TeamRole role) {
        TeamMember member = getTeamMember(user);
        return member != null && member.getRole() == role;
    }

    public boolean canUserManage(User user) {
        if (isOwner(user)) return true;
        TeamMember member = getTeamMember(user);
        return member != null && member.getRole().canManageTeam();
    }

    public boolean canUserManageMembers(User user) {
        if (isOwner(user)) return true;
        TeamMember member = getTeamMember(user);
        return member != null && member.getRole().canManageMembers();
    }

    public boolean canUserViewFinancials(User user) {
        if (isOwner(user)) return true;
        TeamMember member = getTeamMember(user);
        return member != null && member.getRole().canViewFinancials();
    }

    public boolean canUserApproveExpenses(User user) {
        if (isOwner(user)) return true;
        TeamMember member = getTeamMember(user);
        return member != null && member.getRole().canApproveExpenses();
    }

    public boolean isAtMaxCapacity() {
        return getMemberCount() >= maxMembers;
    }

    public void addMember(TeamMember member) {
        if (members == null) {
            members = new ArrayList<>();
        }
        members.add(member);
        member.setTeam(this);
    }

    public void removeMember(TeamMember member) {
        if (members != null) {
            members.remove(member);
            member.setTeam(null);
        }
    }

    public List<TeamMember> getActiveMembers() {
        return members != null ? members.stream()
                .filter(TeamMember::getIsActive)
                .toList() : new ArrayList<>();
    }

    public List<TeamMember> getMembersByRole(TeamRole role) {
        return members != null ? members.stream()
                .filter(member -> member.getRole() == role)
                .toList() : new ArrayList<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Team team = (Team) obj;
        
        if (id != null ? !id.equals(team.id) : team.id != null) return false;
        if (name != null ? !name.equals(team.name) : team.name != null) return false;
        if (description != null ? !description.equals(team.description) : team.description != null) return false;
        if (owner != null ? !owner.equals(team.owner) : team.owner != null) return false;
        if (isActive != null ? !isActive.equals(team.isActive) : team.isActive != null) return false;
        if (maxMembers != null ? !maxMembers.equals(team.maxMembers) : team.maxMembers != null) return false;
        if (inviteCode != null ? !inviteCode.equals(team.inviteCode) : team.inviteCode != null) return false;
        if (autoApproveMembers != null ? !autoApproveMembers.equals(team.autoApproveMembers) : team.autoApproveMembers != null) return false;
        return currency != null ? currency.equals(team.currency) : team.currency == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (isActive != null ? isActive.hashCode() : 0);
        result = 31 * result + (maxMembers != null ? maxMembers.hashCode() : 0);
        result = 31 * result + (inviteCode != null ? inviteCode.hashCode() : 0);
        result = 31 * result + (autoApproveMembers != null ? autoApproveMembers.hashCode() : 0);
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                ", maxMembers=" + maxMembers +
                ", inviteCode='" + inviteCode + '\'' +
                ", autoApproveMembers=" + autoApproveMembers +
                ", currency='" + currency + '\'' +
                ", memberCount=" + getMemberCount() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    
}