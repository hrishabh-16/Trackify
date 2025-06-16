package com.trackify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "spent_amount", precision = 12, scale = 2)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", precision = 12, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "alert_threshold", precision = 5, scale = 2)
    private BigDecimal alertThreshold = BigDecimal.valueOf(80.0); // 80% threshold

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "recurrence_period", length = 20)
    private String recurrencePeriod; // MONTHLY, QUARTERLY, YEARLY

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "team_id")
    private Long teamId;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    private Team team;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Budget(String name, BigDecimal totalAmount, LocalDate startDate, LocalDate endDate, Long userId) {
        this.name = name;
        this.totalAmount = totalAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userId = userId;
        this.spentAmount = BigDecimal.ZERO;
        this.remainingAmount = totalAmount;
        this.isActive = true;
        this.isRecurring = false;
        this.currency = "USD";
        this.alertThreshold = BigDecimal.valueOf(80.0);
    }

    public Budget(String name, BigDecimal totalAmount, LocalDate startDate, LocalDate endDate, 
                  Long userId, Long categoryId) {
        this(name, totalAmount, startDate, endDate, userId);
        this.categoryId = categoryId;
    }

    public Budget() {
    	
    }

	// Utility methods
    public BigDecimal getUsedPercentage() {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // ADDED METHOD TO FIX ERROR - This was missing
    public BigDecimal getUtilizationPercentage() {
        return getUsedPercentage();
    }

    public boolean isOverBudget() {
        return spentAmount.compareTo(totalAmount) > 0;
    }

    public boolean isNearThreshold() {
        BigDecimal usedPercentage = getUsedPercentage();
        return usedPercentage.compareTo(alertThreshold) >= 0;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }

    public boolean isCurrentlyActive() {
        LocalDate now = LocalDate.now();
        return isActive && !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    public void updateSpentAmount(BigDecimal newSpentAmount) {
        this.spentAmount = newSpentAmount;
        this.remainingAmount = totalAmount.subtract(spentAmount);
    }

    public void addExpense(BigDecimal expenseAmount) {
        this.spentAmount = this.spentAmount.add(expenseAmount);
        this.remainingAmount = this.totalAmount.subtract(this.spentAmount);
    }

    public void removeExpense(BigDecimal expenseAmount) {
        this.spentAmount = this.spentAmount.subtract(expenseAmount);
        this.remainingAmount = this.totalAmount.subtract(this.spentAmount);
    }

    // Custom getters for totalAmount to fix error
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        this.remainingAmount = totalAmount.subtract(spentAmount != null ? spentAmount : BigDecimal.ZERO);
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

	public BigDecimal getSpentAmount() {
		return spentAmount;
	}

	public void setSpentAmount(BigDecimal spentAmount) {
		this.spentAmount = spentAmount;
	}

	public BigDecimal getRemainingAmount() {
		return remainingAmount;
	}

	public void setRemainingAmount(BigDecimal remainingAmount) {
		this.remainingAmount = remainingAmount;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getAlertThreshold() {
		return alertThreshold;
	}

	public void setAlertThreshold(BigDecimal alertThreshold) {
		this.alertThreshold = alertThreshold;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Boolean getIsRecurring() {
		return isRecurring;
	}

	public void setIsRecurring(Boolean isRecurring) {
		this.isRecurring = isRecurring;
	}

	public String getRecurrencePeriod() {
		return recurrencePeriod;
	}

	public void setRecurrencePeriod(String recurrencePeriod) {
		this.recurrencePeriod = recurrencePeriod;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
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