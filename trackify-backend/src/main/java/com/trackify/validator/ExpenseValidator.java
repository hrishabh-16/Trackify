package com.trackify.validator;

import com.trackify.dto.request.ExpenseRequest;
import com.trackify.entity.Category;
import com.trackify.entity.Expense;
import com.trackify.entity.Team;
import com.trackify.entity.User;
import com.trackify.enums.ExpenseStatus;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ForbiddenException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.CategoryRepository;
import com.trackify.repository.ExpenseRepository;
import com.trackify.repository.TeamRepository;
import com.trackify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Set;

@Component
public class ExpenseValidator {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Set<String> VALID_CURRENCIES = Set.of(
            "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR", "BRL"
    );

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");

    public void validateExpenseCreation(ExpenseRequest request, Long userId) {
        validateBasicExpenseData(request);
        validateUserExists(userId);
        validateCategoryAccess(request.getCategoryId(), userId);
        
        if (request.getTeamId() != null) {
            validateTeamAccess(request.getTeamId(), userId);
        }
        
        validateBusinessRules(request);
    }

    public void validateExpenseUpdate(Long expenseId, ExpenseRequest request, Long userId) {
        Expense existingExpense = validateExpenseExists(expenseId);
        validateExpenseOwnership(existingExpense, userId);
        validateExpenseCanBeEdited(existingExpense);
        
        validateBasicExpenseData(request);
        
        // If category is being changed, validate new category access
        if (!existingExpense.getCategoryId().equals(request.getCategoryId())) {
            validateCategoryAccess(request.getCategoryId(), userId);
        }
        
        // If team is being changed, validate new team access
        if (request.getTeamId() != null && 
            !request.getTeamId().equals(existingExpense.getTeamId())) {
            validateTeamAccess(request.getTeamId(), userId);
        }
        
        validateBusinessRules(request);
        validateStatusTransition(existingExpense.getStatus(), request.getStatus());
    }

    public void validateExpenseDeletion(Long expenseId, Long userId) {
        Expense expense = validateExpenseExists(expenseId);
        validateExpenseOwnership(expense, userId);
        validateExpenseCanBeDeleted(expense);
    }

    public void validateExpenseApproval(Long expenseId, Long approverId) {
        Expense expense = validateExpenseExists(expenseId);
        validateExpenseCanBeApproved(expense);
        
        if (expense.getTeamId() != null) {
            validateApprovalPermission(expense.getTeamId(), approverId);
        }
        
        // Cannot approve own expense
        if (expense.getUserId().equals(approverId)) {
            throw new BadRequestException("You cannot approve your own expense");
        }
    }

    public void validateExpenseRejection(Long expenseId, Long rejectedBy, String rejectionReason) {
        Expense expense = validateExpenseExists(expenseId);
        validateExpenseCanBeRejected(expense);
        
        if (expense.getTeamId() != null) {
            validateApprovalPermission(expense.getTeamId(), rejectedBy);
        }
        
        // Cannot reject own expense
        if (expense.getUserId().equals(rejectedBy)) {
            throw new BadRequestException("You cannot reject your own expense");
        }
        
        if (!StringUtils.hasText(rejectionReason)) {
            throw new BadRequestException("Rejection reason is required");
        }
        
        if (rejectionReason.length() > 500) {
            throw new BadRequestException("Rejection reason cannot exceed 500 characters");
        }
    }

    public void validateBulkExpenseOperation(List<Long> expenseIds, Long userId) {
        if (expenseIds == null || expenseIds.isEmpty()) {
            throw new BadRequestException("Expense IDs list cannot be empty");
        }
        
        if (expenseIds.size() > 100) {
            throw new BadRequestException("Cannot process more than 100 expenses at once");
        }
        
        for (Long expenseId : expenseIds) {
            validateExpenseExists(expenseId);
            validateExpenseOwnership(expenseId, userId);
        }
    }

    public void validateReimbursement(Long expenseId, Long userId) {
        Expense expense = validateExpenseExists(expenseId);
        validateExpenseOwnership(expense, userId);
        
        if (!expense.getIsReimbursable()) {
            throw new BadRequestException("This expense is not marked as reimbursable");
        }
        
        if (expense.getReimbursed()) {
            throw new BadRequestException("This expense has already been reimbursed");
        }
        
        if (expense.getStatus() != ExpenseStatus.APPROVED) {
            throw new BadRequestException("Only approved expenses can be marked as reimbursed");
        }
    }

    public void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        
        if (startDate.isBefore(LocalDate.now().minusYears(5))) {
            throw new BadRequestException("Start date cannot be more than 5 years in the past");
        }
        
        if (endDate.isAfter(LocalDate.now().plusDays(1))) {
            throw new BadRequestException("End date cannot be in the future");
        }
    }

    public void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount == null || maxAmount == null) {
            throw new BadRequestException("Min amount and max amount are required");
        }
        
        if (minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Min amount cannot be negative");
        }
        
        if (maxAmount.compareTo(minAmount) < 0) {
            throw new BadRequestException("Max amount cannot be less than min amount");
        }
        
        if (maxAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new BadRequestException("Max amount cannot exceed " + MAX_AMOUNT);
        }
    }

    // Private validation methods

    private void validateBasicExpenseData(ExpenseRequest request) {
        // Title validation
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BadRequestException("Expense title is required");
        }
        
        if (request.getTitle().trim().length() < 3) {
            throw new BadRequestException("Expense title must be at least 3 characters long");
        }
        
        if (request.getTitle().length() > 200) {
            throw new BadRequestException("Expense title cannot exceed 200 characters");
        }

        // Description validation
        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            throw new BadRequestException("Description cannot exceed 1000 characters");
        }

        // Amount validation
        validateAmount(request.getAmount());

        // Date validation
        if (request.getExpenseDate() == null) {
            throw new BadRequestException("Expense date is required");
        }
        
        if (request.getExpenseDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Expense date cannot be in the future");
        }
        
        if (request.getExpenseDate().isBefore(LocalDate.now().minusYears(2))) {
            throw new BadRequestException("Expense date cannot be more than 2 years in the past");
        }

        // Category validation
        if (request.getCategoryId() == null) {
            throw new BadRequestException("Category is required");
        }

        // Currency validation
        if (StringUtils.hasText(request.getCurrencyCode())) {
            validateCurrency(request.getCurrencyCode());
        }

        // Exchange rate validation
        if (request.getExchangeRate() != null) {
            validateExchangeRate(request.getExchangeRate());
        }

        // Original amount validation
        if (request.getOriginalAmount() != null) {
            validateAmount(request.getOriginalAmount());
            
            if (request.getExchangeRate() == null) {
                throw new BadRequestException("Exchange rate is required when original amount is provided");
            }
            
            if (!StringUtils.hasText(request.getOriginalCurrency())) {
                throw new BadRequestException("Original currency is required when original amount is provided");
            }
            
            validateCurrency(request.getOriginalCurrency());
        }

        // String field length validations
        validateStringField(request.getMerchantName(), "Merchant name", 200);
        validateStringField(request.getLocation(), "Location", 500);
        validateStringField(request.getTags(), "Tags", 500);
        validateStringField(request.getNotes(), "Notes", 1000);
        validateStringField(request.getReferenceNumber(), "Reference number", 100);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Amount is required");
        }
        
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            throw new BadRequestException("Amount must be at least " + MIN_AMOUNT);
        }
        
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BadRequestException("Amount cannot exceed " + MAX_AMOUNT);
        }
        
        if (amount.scale() > 2) {
            throw new BadRequestException("Amount cannot have more than 2 decimal places");
        }
    }

    private void validateCurrency(String currencyCode) {
        if (!VALID_CURRENCIES.contains(currencyCode.toUpperCase())) {
            try {
                Currency.getInstance(currencyCode);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid currency code: " + currencyCode);
            }
        }
    }

    private void validateExchangeRate(BigDecimal exchangeRate) {
        if (exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Exchange rate must be positive");
        }
        
        if (exchangeRate.compareTo(new BigDecimal("9999.9999")) > 0) {
            throw new BadRequestException("Exchange rate is too high");
        }
    }

    private void validateStringField(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new BadRequestException(fieldName + " cannot exceed " + maxLength + " characters");
        }
    }

    private Expense validateExpenseExists(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + expenseId));
    }

    private void validateExpenseOwnership(Expense expense, Long userId) {
        if (!expense.getUserId().equals(userId)) {
            throw new ForbiddenException("You don't have access to this expense");
        }
    }

    private void validateExpenseOwnership(Long expenseId, Long userId) {
        Expense expense = validateExpenseExists(expenseId);
        validateExpenseOwnership(expense, userId);
    }

    private void validateExpenseCanBeEdited(Expense expense) {
        if (!expense.canBeEdited()) {
            throw new BadRequestException("This expense cannot be edited in its current status: " + 
                    expense.getStatus().getDisplayName());
        }
    }

    private void validateExpenseCanBeDeleted(Expense expense) {
        if (!expense.canBeDeleted()) {
            throw new BadRequestException("This expense cannot be deleted in its current status: " + 
                    expense.getStatus().getDisplayName());
        }
    }

    private void validateExpenseCanBeApproved(Expense expense) {
        if (expense.getStatus() != ExpenseStatus.PENDING && expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new BadRequestException("Only pending or submitted expenses can be approved");
        }
    }

    private void validateExpenseCanBeRejected(Expense expense) {
        if (expense.getStatus() != ExpenseStatus.PENDING && expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new BadRequestException("Only pending or submitted expenses can be rejected");
        }
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
    }

    private void validateCategoryAccess(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        
        // Check if user has access to this category (personal, team, or system category)
        if (!category.getCreatedBy().equals(userId) && !category.getIsSystem()) {
            // For team categories, check if user is member of the team
            // This logic would depend on your Category entity structure
            throw new ForbiddenException("You don't have access to this category");
        }
    }

    private void validateTeamAccess(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + teamId));
        
        if (!team.getIsActive()) {
            throw new BadRequestException("Cannot add expenses to inactive team");
        }
        
        if (!teamRepository.isUserMemberOfTeam(teamId, userId)) {
            throw new ForbiddenException("You are not a member of this team");
        }
    }

    private void validateApprovalPermission(Long teamId, Long userId) {
        if (!teamRepository.isUserMemberOfTeam(teamId, userId)) {
            throw new ForbiddenException("You are not a member of this team");
        }
        
        // Additional logic to check if user has approval permissions in the team
        // This would require checking the user's role in the team
    }

    private void validateBusinessRules(ExpenseRequest request) {
        // Business expense validation
        if (request.getIsBusinessExpense() && request.getTeamId() == null) {
            throw new BadRequestException("Business expenses must be associated with a team");
        }
        
        // Reimbursable expense validation
        if (request.getIsReimbursable() && !request.getIsBusinessExpense()) {
            throw new BadRequestException("Only business expenses can be marked as reimbursable");
        }
        
        // Recurring expense validation
        if (request.getIsRecurring() && request.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            throw new BadRequestException("Recurring expenses cannot exceed $10,000");
        }
        
        // Receipt validation
        if (request.getReceiptIds() != null && request.getReceiptIds().size() > 10) {
            throw new BadRequestException("Cannot attach more than 10 receipts to an expense");
        }
    }

    private void validateStatusTransition(ExpenseStatus currentStatus, ExpenseStatus newStatus) {
        if (newStatus == null || currentStatus == newStatus) {
            return;
        }
        
        // Define valid status transitions
        boolean isValidTransition = switch (currentStatus) {
            case DRAFT -> newStatus == ExpenseStatus.PENDING || newStatus == ExpenseStatus.SUBMITTED;
            case PENDING -> newStatus == ExpenseStatus.APPROVED || newStatus == ExpenseStatus.REJECTED || 
                          newStatus == ExpenseStatus.DRAFT;
            case SUBMITTED -> newStatus == ExpenseStatus.APPROVED || newStatus == ExpenseStatus.REJECTED;
            case APPROVED -> newStatus == ExpenseStatus.PAID;
            case REJECTED -> newStatus == ExpenseStatus.DRAFT || newStatus == ExpenseStatus.PENDING;
            case PAID -> false; // Paid expenses cannot be changed
            case CANCELLED -> false; // Cancelled expenses cannot be changed
        };
        
        if (!isValidTransition) {
            throw new BadRequestException("Invalid status transition from " + 
                    currentStatus.getDisplayName() + " to " + newStatus.getDisplayName());
        }
    }
}