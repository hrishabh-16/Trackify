package com.trackify.validator;

import com.trackify.dto.request.BudgetRequest;
import com.trackify.entity.Budget;
import com.trackify.entity.Category;
import com.trackify.entity.Team;
import com.trackify.entity.User;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.BudgetRepository;
import com.trackify.repository.CategoryRepository;
import com.trackify.repository.TeamRepository;
import com.trackify.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Component
public class BudgetValidator {

    private static final Logger logger = LoggerFactory.getLogger(BudgetValidator.class);

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Value("${app.validation.expense.max-amount:999999.99}")
    private BigDecimal maxBudgetAmount;

    @Value("${app.validation.expense.min-amount:0.01}")
    private BigDecimal minBudgetAmount;

    // Supported currencies
    private static final Set<String> SUPPORTED_CURRENCIES = new HashSet<>(Arrays.asList(
            "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD",
            "MXN", "SGD", "HKD", "NOK", "TRY", "ZAR", "BRL", "INR", "KRW", "RUB"
    ));

    // Supported recurrence periods
    private static final Set<String> SUPPORTED_RECURRENCE_PERIODS = new HashSet<>(Arrays.asList(
            "MONTHLY", "QUARTERLY", "YEARLY"
    ));

    // Maximum budget duration in days
    private static final long MAX_BUDGET_DURATION_DAYS = 1825; // 5 years

    // Minimum budget duration in days
    private static final long MIN_BUDGET_DURATION_DAYS = 1; // 1 day

    /**
     * Validates a budget creation request
     */
    public void validateBudgetCreation(BudgetRequest budgetRequest, String username) {
        logger.debug("Validating budget creation for user: {}", username);

        // Validate user exists
        User user = validateUserExists(username);

        // Validate basic budget fields
        validateBasicBudgetFields(budgetRequest);

        // Validate date range
        validateDateRange(budgetRequest.getStartDate(), budgetRequest.getEndDate());

        // Validate currency
        validateCurrency(budgetRequest.getCurrency());

        // Validate amount
        validateAmount(budgetRequest.getTotalAmount());

        // Validate alert threshold
        validateAlertThreshold(budgetRequest.getAlertThreshold());

        // Validate recurrence settings
        if (budgetRequest.getIsRecurring() != null && budgetRequest.getIsRecurring()) {
            validateRecurrenceSettings(budgetRequest);
        }

        // Validate category if provided
        if (budgetRequest.getCategoryId() != null) {
            validateCategoryAccess(budgetRequest.getCategoryId(), user.getId());
            
            // Check for overlapping budgets for the same category
            validateNoBudgetOverlap(user.getId(), budgetRequest.getCategoryId(),
                    budgetRequest.getStartDate(), budgetRequest.getEndDate(), null);
        }

        // Validate team if provided
        if (budgetRequest.getTeamId() != null) {
            validateTeamAccess(budgetRequest.getTeamId(), user.getId());
        }

        logger.debug("Budget creation validation passed for user: {}", username);
    }

    /**
     * Validates a budget update request
     */
    public void validateBudgetUpdate(Long budgetId, BudgetRequest.UpdateBudgetRequest updateRequest, String username) {
        logger.debug("Validating budget update for budget: {} by user: {}", budgetId, username);

        // Validate user exists
        User user = validateUserExists(username);

        // Validate budget exists and user has access
        Budget existingBudget = validateBudgetExists(budgetId);
        validateBudgetAccess(existingBudget, user.getId());

        // Validate fields if they are being updated
        if (updateRequest.getName() != null) {
            validateBudgetName(updateRequest.getName());
        }

        if (updateRequest.getDescription() != null) {
            validateBudgetDescription(updateRequest.getDescription());
        }

        if (updateRequest.getTotalAmount() != null) {
            validateAmount(updateRequest.getTotalAmount());
            validateAmountUpdate(existingBudget, updateRequest.getTotalAmount());
        }

        if (updateRequest.getCurrency() != null) {
            validateCurrency(updateRequest.getCurrency());
        }

        if (updateRequest.getAlertThreshold() != null) {
            validateAlertThreshold(updateRequest.getAlertThreshold());
        }

        // Validate date range if either date is being updated
        LocalDate newStartDate = updateRequest.getStartDate() != null ? 
                updateRequest.getStartDate() : existingBudget.getStartDate();
        LocalDate newEndDate = updateRequest.getEndDate() != null ? 
                updateRequest.getEndDate() : existingBudget.getEndDate();
        
        validateDateRange(newStartDate, newEndDate);

        // Validate recurrence settings
        Boolean isRecurring = updateRequest.getIsRecurring() != null ? 
                updateRequest.getIsRecurring() : existingBudget.getIsRecurring();
        
        if (isRecurring) {
            String recurrencePeriod = updateRequest.getRecurrencePeriod() != null ? 
                    updateRequest.getRecurrencePeriod() : existingBudget.getRecurrencePeriod();
            validateRecurrencePeriod(recurrencePeriod);
        }

        // Validate category if being updated
        if (updateRequest.getCategoryId() != null) {
            validateCategoryAccess(updateRequest.getCategoryId(), user.getId());
            
            // Check for overlapping budgets if category or dates are changing
            Long newCategoryId = updateRequest.getCategoryId();
            if (!newCategoryId.equals(existingBudget.getCategoryId()) || 
                !newStartDate.equals(existingBudget.getStartDate()) || 
                !newEndDate.equals(existingBudget.getEndDate())) {
                
                validateNoBudgetOverlap(user.getId(), newCategoryId, newStartDate, newEndDate, budgetId);
            }
        }

        // Validate team if being updated
        if (updateRequest.getTeamId() != null) {
            validateTeamAccess(updateRequest.getTeamId(), user.getId());
        }

        logger.debug("Budget update validation passed for budget: {} by user: {}", budgetId, username);
    }

    /**
     * Validates budget transfer request
     */
    public void validateBudgetTransfer(BudgetRequest.BudgetTransferRequest transferRequest, String username) {
        logger.debug("Validating budget transfer for user: {}", username);

        // Validate user exists
        User user = validateUserExists(username);

        // Validate both budgets exist and user has access
        Budget sourceBudget = validateBudgetExists(transferRequest.getSourceBudgetId());
        Budget targetBudget = validateBudgetExists(transferRequest.getTargetBudgetId());

        validateBudgetAccess(sourceBudget, user.getId());
        validateBudgetAccess(targetBudget, user.getId());

        // Validate transfer amount
        validateTransferAmount(transferRequest.getAmount(), sourceBudget);

        // Validate budgets are different
        if (transferRequest.getSourceBudgetId().equals(transferRequest.getTargetBudgetId())) {
            throw new BadRequestException("Source and target budgets cannot be the same");
        }

        // Validate both budgets are active
        if (!sourceBudget.getIsActive()) {
            throw new BadRequestException("Source budget must be active for transfer");
        }

        if (!targetBudget.getIsActive()) {
            throw new BadRequestException("Target budget must be active for transfer");
        }

        // Validate currency compatibility
        if (!sourceBudget.getCurrency().equals(targetBudget.getCurrency())) {
            throw new BadRequestException("Source and target budgets must have the same currency");
        }

        logger.debug("Budget transfer validation passed for user: {}", username);
    }

    /**
     * Validates recurring budget creation request
     */
    public void validateRecurringBudgetCreation(BudgetRequest.RecurringBudgetRequest recurringRequest, String username) {
        logger.debug("Validating recurring budget creation for user: {}", username);

        // Validate user exists
        User user = validateUserExists(username);

        // Validate source budget exists and user has access
        Budget sourceBudget = validateBudgetExists(recurringRequest.getBudgetId());
        validateBudgetAccess(sourceBudget, user.getId());

        // Validate recurrence period
        validateRecurrencePeriod(recurringRequest.getRecurrencePeriod());

        // Validate occurrences if specified
        if (recurringRequest.getOccurrences() != null) {
            if (recurringRequest.getOccurrences() <= 0) {
                throw new BadRequestException("Number of occurrences must be positive");
            }
            if (recurringRequest.getOccurrences() > 120) { // Max 10 years for monthly
                throw new BadRequestException("Number of occurrences cannot exceed 120");
            }
        }

        // Validate next start date
        if (recurringRequest.getNextStartDate() != null) {
            if (recurringRequest.getNextStartDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Next start date cannot be in the past");
            }
        }

        logger.debug("Recurring budget creation validation passed for user: {}", username);
    }

    // Private validation methods

    private User validateUserExists(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Budget validateBudgetExists(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + budgetId));
    }

    private void validateBudgetAccess(Budget budget, Long userId) {
        if (!budget.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have access to this budget");
        }
    }

    private void validateBasicBudgetFields(BudgetRequest budgetRequest) {
        validateBudgetName(budgetRequest.getName());
        validateBudgetDescription(budgetRequest.getDescription());
    }

    private void validateBudgetName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Budget name is required");
        }
        if (name.length() < 2) {
            throw new BadRequestException("Budget name must be at least 2 characters long");
        }
        if (name.length() > 200) {
            throw new BadRequestException("Budget name cannot exceed 200 characters");
        }
    }

    private void validateBudgetDescription(String description) {
        if (description != null && description.length() > 500) {
            throw new BadRequestException("Budget description cannot exceed 500 characters");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new BadRequestException("Start date is required");
        }
        if (endDate == null) {
            throw new BadRequestException("End date is required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date cannot be before start date");
        }
        if (startDate.equals(endDate)) {
            throw new BadRequestException("Start date and end date cannot be the same");
        }

        // Validate duration
        long durationDays = ChronoUnit.DAYS.between(startDate, endDate);
        if (durationDays < MIN_BUDGET_DURATION_DAYS) {
            throw new BadRequestException("Budget duration must be at least " + MIN_BUDGET_DURATION_DAYS + " day(s)");
        }
        if (durationDays > MAX_BUDGET_DURATION_DAYS) {
            throw new BadRequestException("Budget duration cannot exceed " + MAX_BUDGET_DURATION_DAYS + " days");
        }

        // Validate start date is not too far in the past
        if (startDate.isBefore(LocalDate.now().minusYears(1))) {
            throw new BadRequestException("Start date cannot be more than 1 year in the past");
        }

        // Validate end date is not too far in the future
     // Validate end date is not too far in the future
        if (endDate.isAfter(LocalDate.now().plusYears(5))) {
            throw new BadRequestException("End date cannot be more than 5 years in the future");
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new BadRequestException("Currency is required");
        }
        if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
            throw new BadRequestException("Unsupported currency: " + currency + 
                    ". Supported currencies: " + String.join(", ", SUPPORTED_CURRENCIES));
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Budget amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Budget amount must be greater than zero");
        }
        if (amount.compareTo(minBudgetAmount) < 0) {
            throw new BadRequestException("Budget amount must be at least " + minBudgetAmount);
        }
        if (amount.compareTo(maxBudgetAmount) > 0) {
            throw new BadRequestException("Budget amount cannot exceed " + maxBudgetAmount);
        }

        // Validate decimal places (max 2)
        if (amount.scale() > 2) {
            throw new BadRequestException("Budget amount cannot have more than 2 decimal places");
        }
    }

    private void validateAlertThreshold(BigDecimal alertThreshold) {
        if (alertThreshold == null) {
            return; // Optional field
        }
        if (alertThreshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Alert threshold cannot be negative");
        }
        if (alertThreshold.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Alert threshold cannot exceed 100%");
        }
    }

    private void validateRecurrenceSettings(BudgetRequest budgetRequest) {
        if (budgetRequest.getRecurrencePeriod() == null || budgetRequest.getRecurrencePeriod().trim().isEmpty()) {
            throw new BadRequestException("Recurrence period is required for recurring budgets");
        }
        validateRecurrencePeriod(budgetRequest.getRecurrencePeriod());
    }

    private void validateRecurrencePeriod(String recurrencePeriod) {
        if (recurrencePeriod == null || recurrencePeriod.trim().isEmpty()) {
            throw new BadRequestException("Recurrence period is required");
        }
        if (!SUPPORTED_RECURRENCE_PERIODS.contains(recurrencePeriod.toUpperCase())) {
            throw new BadRequestException("Invalid recurrence period: " + recurrencePeriod + 
                    ". Supported periods: " + String.join(", ", SUPPORTED_RECURRENCE_PERIODS));
        }
    }

    private void validateCategoryAccess(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
        
        if (!category.getId().equals(userId)) {
            throw new BadRequestException("You do not have access to this category");
        }
    }

    private void validateTeamAccess(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + teamId));
        
        // Check if user is a member of the team (this would depend on your team membership logic)
        // For now, we'll just check if the team exists
        // You might want to add team membership validation here
    }

    private void validateNoBudgetOverlap(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate, Long excludeBudgetId) {
        Long excludeId = excludeBudgetId != null ? excludeBudgetId : 0L;
        
        boolean hasOverlap = budgetRepository.hasOverlappingBudget(userId, categoryId, startDate, endDate, excludeId);
        
        if (hasOverlap) {
            throw new BadRequestException(
                    "Budget period overlaps with an existing active budget for this category. " +
                    "Please choose a different date range or category.");
        }
    }

    private void validateAmountUpdate(Budget existingBudget, BigDecimal newAmount) {
        // Ensure new amount is not less than already spent amount
        if (newAmount.compareTo(existingBudget.getSpentAmount()) < 0) {
            throw new BadRequestException(
                    String.format("New budget amount (%s) cannot be less than already spent amount (%s)",
                            newAmount, existingBudget.getSpentAmount()));
        }

        // Warn if new amount is significantly different (more than 50% change)
        BigDecimal currentAmount = existingBudget.getTotalAmount();
        BigDecimal changePercentage = newAmount.subtract(currentAmount)
                .divide(currentAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .abs();

        if (changePercentage.compareTo(BigDecimal.valueOf(50)) > 0) {
            logger.warn("Significant budget amount change detected for budget {}: from {} to {} ({}% change)",
                    existingBudget.getId(), currentAmount, newAmount, changePercentage);
        }
    }

    private void validateTransferAmount(BigDecimal transferAmount, Budget sourceBudget) {
        if (transferAmount == null) {
            throw new BadRequestException("Transfer amount is required");
        }
        if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Transfer amount must be greater than zero");
        }
        if (transferAmount.compareTo(sourceBudget.getRemainingAmount()) > 0) {
            throw new BadRequestException(
                    String.format("Transfer amount (%s) cannot exceed remaining budget amount (%s)",
                            transferAmount, sourceBudget.getRemainingAmount()));
        }

        // Validate decimal places (max 2)
        if (transferAmount.scale() > 2) {
            throw new BadRequestException("Transfer amount cannot have more than 2 decimal places");
        }
    }

    /**
     * Validates budget deletion
     */
    public void validateBudgetDeletion(Long budgetId, String username) {
        logger.debug("Validating budget deletion for budget: {} by user: {}", budgetId, username);

        // Validate user exists
        User user = validateUserExists(username);

        // Validate budget exists and user has access
        Budget budget = validateBudgetExists(budgetId);
        validateBudgetAccess(budget, user.getId());

        // Warn if budget has expenses
        if (budget.getSpentAmount().compareTo(BigDecimal.ZERO) > 0) {
            logger.warn("Deleting budget {} with existing expenses: {}", budgetId, budget.getSpentAmount());
        }

        // Prevent deletion if budget is part of an active recurring series
        if (budget.getIsRecurring() && budget.getIsActive() && !budget.isExpired()) {
            throw new BadRequestException("Cannot delete an active recurring budget. Please deactivate it first.");
        }

        logger.debug("Budget deletion validation passed for budget: {} by user: {}", budgetId, username);
    }

    /**
     * Validates budget activation/deactivation
     */
    public void validateBudgetStatusChange(Long budgetId, String username, boolean activating) {
        logger.debug("Validating budget status change for budget: {} by user: {} (activating: {})", 
                budgetId, username, activating);

        // Validate user exists
        User user = validateUserExists(username);

        // Validate budget exists and user has access
        Budget budget = validateBudgetExists(budgetId);
        validateBudgetAccess(budget, user.getId());

        if (activating) {
            // Validate budget can be activated
            if (budget.isExpired()) {
                throw new BadRequestException("Cannot activate an expired budget");
            }

            // Check for overlapping active budgets if this budget has a category
            if (budget.getCategoryId() != null) {
                validateNoBudgetOverlap(user.getId(), budget.getCategoryId(),
                        budget.getStartDate(), budget.getEndDate(), budgetId);
            }
        } else {
            // Validate budget can be deactivated
            if (!budget.getIsActive()) {
                throw new BadRequestException("Budget is already inactive");
            }
        }

        logger.debug("Budget status change validation passed for budget: {} by user: {}", budgetId, username);
    }

    /**
     * Validates bulk budget operations
     */
    public void validateBulkBudgetOperation(List<Long> budgetIds, String username, String operation) {
        logger.debug("Validating bulk budget operation: {} for {} budgets by user: {}", 
                operation, budgetIds.size(), username);

        if (budgetIds == null || budgetIds.isEmpty()) {
            throw new BadRequestException("Budget IDs list cannot be empty");
        }

        if (budgetIds.size() > 50) {
            throw new BadRequestException("Cannot perform bulk operation on more than 50 budgets at once");
        }

        // Validate user exists
        User user = validateUserExists(username);

        // Validate all budgets exist and user has access
        for (Long budgetId : budgetIds) {
            Budget budget = validateBudgetExists(budgetId);
            validateBudgetAccess(budget, user.getId());

            // Additional validations based on operation
            switch (operation.toUpperCase()) {
                case "DELETE":
                    validateBudgetDeletion(budgetId, username);
                    break;
                case "ACTIVATE":
                    if (!budget.getIsActive()) {
                        validateBudgetStatusChange(budgetId, username, true);
                    }
                    break;
                case "DEACTIVATE":
                    if (budget.getIsActive()) {
                        validateBudgetStatusChange(budgetId, username, false);
                    }
                    break;
                default:
                    throw new BadRequestException("Unsupported bulk operation: " + operation);
            }
        }

        logger.debug("Bulk budget operation validation passed for user: {}", username);
    }

    /**
     * Validates budget search parameters
     */
    public void validateBudgetSearchParameters(String keyword, LocalDate startDate, LocalDate endDate, 
                                             BigDecimal minAmount, BigDecimal maxAmount) {
        if (keyword != null && keyword.length() > 100) {
            throw new BadRequestException("Search keyword cannot exceed 100 characters");
        }

        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new BadRequestException("End date cannot be before start date");
            }
            
            // Validate search range is not too broad
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 1825) { // 5 years
                throw new BadRequestException("Search date range cannot exceed 5 years");
            }
        }

        if (minAmount != null && maxAmount != null) {
            if (maxAmount.compareTo(minAmount) < 0) {
                throw new BadRequestException("Maximum amount cannot be less than minimum amount");
            }
        }

        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum amount cannot be negative");
        }

        if (maxAmount != null && maxAmount.compareTo(maxBudgetAmount) > 0) {
            throw new BadRequestException("Maximum amount cannot exceed " + maxBudgetAmount);
        }
    }

    /**
     * Validates export parameters
     */
    public void validateExportParameters(String format, LocalDate startDate, LocalDate endDate) {
        if (format == null || format.trim().isEmpty()) {
            throw new BadRequestException("Export format is required");
        }

        Set<String> supportedFormats = new HashSet<>(Arrays.asList("CSV", "PDF", "XLSX"));
        if (!supportedFormats.contains(format.toUpperCase())) {
            throw new BadRequestException("Unsupported export format: " + format + 
                    ". Supported formats: " + String.join(", ", supportedFormats));
        }

        if (startDate != null && endDate != null) {
            validateDateRange(startDate, endDate);
        }
    }

    /**
     * Gets validation summary for UI display
     */
    public ValidationSummary getValidationSummary() {
        return new ValidationSummary(
                minBudgetAmount,
                maxBudgetAmount,
                SUPPORTED_CURRENCIES,
                SUPPORTED_RECURRENCE_PERIODS,
                MIN_BUDGET_DURATION_DAYS,
                MAX_BUDGET_DURATION_DAYS
        );
    }

    /**
     * Inner class for validation summary
     */
    public static class ValidationSummary {
        private final BigDecimal minAmount;
        private final BigDecimal maxAmount;
        private final Set<String> supportedCurrencies;
        private final Set<String> supportedRecurrencePeriods;
        private final long minDurationDays;
        private final long maxDurationDays;

        public ValidationSummary(BigDecimal minAmount, BigDecimal maxAmount, 
                               Set<String> supportedCurrencies, Set<String> supportedRecurrencePeriods,
                               long minDurationDays, long maxDurationDays) {
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.supportedCurrencies = supportedCurrencies;
            this.supportedRecurrencePeriods = supportedRecurrencePeriods;
            this.minDurationDays = minDurationDays;
            this.maxDurationDays = maxDurationDays;
        }

        // Getters
        public BigDecimal getMinAmount() { return minAmount; }
        public BigDecimal getMaxAmount() { return maxAmount; }
        public Set<String> getSupportedCurrencies() { return supportedCurrencies; }
        public Set<String> getSupportedRecurrencePeriods() { return supportedRecurrencePeriods; }
        public long getMinDurationDays() { return minDurationDays; }
        public long getMaxDurationDays() { return maxDurationDays; }
    }
 }