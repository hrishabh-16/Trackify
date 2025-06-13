package com.trackify.service.impl;

import com.trackify.dto.request.BudgetRequest;
import com.trackify.dto.response.BudgetResponse;
import com.trackify.entity.Budget;
import com.trackify.entity.Category;
import com.trackify.entity.Expense;
import com.trackify.entity.User;
import com.trackify.enums.ExpenseStatus;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ForbiddenException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.BudgetRepository;
import com.trackify.repository.CategoryRepository;
import com.trackify.repository.ExpenseRepository;
import com.trackify.repository.UserRepository;
import com.trackify.service.BudgetService;
import com.trackify.service.WebSocketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BudgetServiceImpl implements BudgetService {

    private static final Logger logger = LoggerFactory.getLogger(BudgetServiceImpl.class);

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Override
    public BudgetResponse createBudget(BudgetRequest budgetRequest, String username) {
        try {
            logger.info("Creating budget: {} by user: {}", budgetRequest.getName(), username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            // Validate date range
            if (budgetRequest.getEndDate().isBefore(budgetRequest.getStartDate())) {
                throw new BadRequestException("End date cannot be before start date");
            }

            // Check for overlapping budgets if category is specified
            if (budgetRequest.getCategoryId() != null) {
                boolean hasOverlap = budgetRepository.hasOverlappingBudget(
                        user.getId(), budgetRequest.getCategoryId(),
                        budgetRequest.getStartDate(), budgetRequest.getEndDate(), 0L);
                
                if (hasOverlap) {
                    throw new BadRequestException("Budget period overlaps with existing budget for this category");
                }
            }

            // Create budget entity
            Budget budget = new Budget();
            budget.setName(budgetRequest.getName());
            budget.setDescription(budgetRequest.getDescription());
            budget.setTotalAmount(budgetRequest.getTotalAmount());
            budget.setStartDate(budgetRequest.getStartDate());
            budget.setEndDate(budgetRequest.getEndDate());
            budget.setCurrency(budgetRequest.getCurrency());
            budget.setAlertThreshold(budgetRequest.getAlertThreshold());
            budget.setIsActive(budgetRequest.getIsActive());
            budget.setIsRecurring(budgetRequest.getIsRecurring());
            budget.setRecurrencePeriod(budgetRequest.getRecurrencePeriod());
            budget.setUserId(user.getId());
            budget.setCategoryId(budgetRequest.getCategoryId());
            budget.setTeamId(budgetRequest.getTeamId());
            budget.setSpentAmount(BigDecimal.ZERO);
            budget.setRemainingAmount(budgetRequest.getTotalAmount());

            budget = budgetRepository.save(budget);

            // Calculate initial spending if category is specified
            if (budget.getCategoryId() != null) {
                recalculateBudgetSpending(budget.getId());
            }

            logger.info("Successfully created budget: {} with ID: {}", budget.getName(), budget.getId());

            // Send notification
            webSocketService.sendNotificationToUser(username,
                new com.trackify.dto.websocket.NotificationMessage(
                    "Budget Created",
                    "Budget '" + budget.getName() + "' has been created successfully",
                    "SUCCESS",
                    username,
                    LocalDateTime.now()
                ));

            return convertToBudgetResponse(budget);

        } catch (Exception e) {
            logger.error("Error creating budget for user: {}", username, e);
            throw e;
        }
    }

    @Override
    public BudgetResponse updateBudget(Long budgetId, BudgetRequest.UpdateBudgetRequest updateRequest, String username) {
        try {
            logger.info("Updating budget: {} by user: {}", budgetId, username);

            validateBudgetAccess(budgetId, username);
            Budget budget = getBudgetEntity(budgetId);

            // Update fields if provided
            if (updateRequest.getName() != null) {
                budget.setName(updateRequest.getName());
            }
            if (updateRequest.getDescription() != null) {
                budget.setDescription(updateRequest.getDescription());
            }
            if (updateRequest.getTotalAmount() != null) {
                budget.setTotalAmount(updateRequest.getTotalAmount());
                budget.setRemainingAmount(updateRequest.getTotalAmount().subtract(budget.getSpentAmount()));
            }
            if (updateRequest.getStartDate() != null) {
                budget.setStartDate(updateRequest.getStartDate());
            }
            if (updateRequest.getEndDate() != null) {
                budget.setEndDate(updateRequest.getEndDate());
            }
            if (updateRequest.getCurrency() != null) {
                budget.setCurrency(updateRequest.getCurrency());
            }
            if (updateRequest.getAlertThreshold() != null) {
                budget.setAlertThreshold(updateRequest.getAlertThreshold());
            }
            if (updateRequest.getIsActive() != null) {
                budget.setIsActive(updateRequest.getIsActive());
            }
            if (updateRequest.getIsRecurring() != null) {
                budget.setIsRecurring(updateRequest.getIsRecurring());
            }
            if (updateRequest.getRecurrencePeriod() != null) {
                budget.setRecurrencePeriod(updateRequest.getRecurrencePeriod());
            }
            if (updateRequest.getCategoryId() != null) {
                budget.setCategoryId(updateRequest.getCategoryId());
            }
            if (updateRequest.getTeamId() != null) {
                budget.setTeamId(updateRequest.getTeamId());
            }

            // Validate date range if both dates are provided
            if (budget.getEndDate().isBefore(budget.getStartDate())) {
                throw new BadRequestException("End date cannot be before start date");
            }

            budget = budgetRepository.save(budget);

            // Recalculate spending
            recalculateBudgetSpending(budget.getId());

            logger.info("Successfully updated budget: {}", budgetId);

            return convertToBudgetResponse(budget);

        } catch (Exception e) {
            logger.error("Error updating budget: {} by user: {}", budgetId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long budgetId, String username) {
        try {
            validateBudgetAccess(budgetId, username);
            Budget budget = getBudgetEntity(budgetId);
            return convertToBudgetResponse(budget);
        } catch (Exception e) {
            logger.error("Error getting budget: {} by user: {}", budgetId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getUserBudgets(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Budget> budgets = budgetRepository.findByUserId(user.getId());
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting budgets for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BudgetResponse> getUserBudgets(String username, Pageable pageable) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            Page<Budget> budgets = budgetRepository.findByUserId(user.getId(), pageable);
            
            return budgets.map(this::convertToBudgetResponse);

        } catch (Exception e) {
            logger.error("Error getting budgets page for user: {}", username, e);
            throw e;
        }
    }

    @Override
    public void deleteBudget(Long budgetId, String username) {
        try {
            logger.info("Deleting budget: {} by user: {}", budgetId, username);

            validateBudgetAccess(budgetId, username);
            Budget budget = getBudgetEntity(budgetId);

            budgetRepository.delete(budget);

            logger.info("Successfully deleted budget: {}", budgetId);

            // Send notification
            webSocketService.sendNotificationToUser(username,
                new com.trackify.dto.websocket.NotificationMessage(
                    "Budget Deleted",
                    "Budget '" + budget.getName() + "' has been deleted",
                    "INFO",
                    username,
                    LocalDateTime.now()
                ));

        } catch (Exception e) {
            logger.error("Error deleting budget: {} by user: {}", budgetId, username, e);
            throw e;
        }
    }

    @Override
    public BudgetResponse activateBudget(Long budgetId, String username) {
        try {
            validateBudgetAccess(budgetId, username);
            Budget budget = getBudgetEntity(budgetId);
            
            budget.setIsActive(true);
            budget = budgetRepository.save(budget);
            
            logger.info("Activated budget: {}", budgetId);
            return convertToBudgetResponse(budget);
            
        } catch (Exception e) {
            logger.error("Error activating budget: {} by user: {}", budgetId, username, e);
            throw e;
        }
    }

    @Override
    public BudgetResponse deactivateBudget(Long budgetId, String username) {
        try {
            validateBudgetAccess(budgetId, username);
            Budget budget = getBudgetEntity(budgetId);
            
            budget.setIsActive(false);
            budget = budgetRepository.save(budget);
            
            logger.info("Deactivated budget: {}", budgetId);
            return convertToBudgetResponse(budget);
            
        } catch (Exception e) {
            logger.error("Error deactivating budget: {} by user: {}", budgetId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getActiveBudgets(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Budget> budgets = budgetRepository.findActiveBudgetsByUser(user.getId(), LocalDate.now());
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting active budgets for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByCategory(Long categoryId, String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Budget> budgets = budgetRepository.findByUserIdAndCategoryId(user.getId(), categoryId);
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting budgets by category: {} for user: {}", categoryId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByTeam(Long teamId, String username) {
        try {
            List<Budget> budgets = budgetRepository.findByTeamIdAndIsActiveTrue(teamId);
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting budgets by team: {} for user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByDateRange(String username, LocalDate startDate, LocalDate endDate) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Budget> budgets = budgetRepository.findByUserIdAndPeriodOverlap(user.getId(), startDate, endDate);
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting budgets by date range for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> searchBudgets(String username, String keyword) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Budget> budgets = budgetRepository.searchBudgets(user.getId(), keyword);
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error searching budgets for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse.BudgetAnalytics getBudgetAnalytics(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            BigDecimal totalBudgeted = budgetRepository.getTotalBudgetAmountByUser(user.getId());
            if (totalBudgeted == null) totalBudgeted = BigDecimal.ZERO;

            BigDecimal totalSpent = budgetRepository.getTotalSpentAmountByUser(user.getId());
            if (totalSpent == null) totalSpent = BigDecimal.ZERO;

            long activeBudgetsCount = budgetRepository.countActiveBudgetsByUser(user.getId());

            List<Budget> overBudgets = budgetRepository.findOverBudgetsByUser(user.getId());
            List<Budget> nearThreshold = budgetRepository.findBudgetsNearThresholdByUser(user.getId());

            return new BudgetResponse.BudgetAnalytics(
                    totalBudgeted,
                    totalSpent,
                    (int) activeBudgetsCount,
                    overBudgets.size()
            );

        } catch (Exception e) {
            logger.error("Error getting budget analytics for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsNearThreshold(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Budget> budgets = budgetRepository.findBudgetsNearThresholdByUser(user.getId());
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting budgets near threshold for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getOverBudgets(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Budget> budgets = budgetRepository.findOverBudgetsByUser(user.getId());
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting over budgets for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getExpiredBudgets(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Budget> budgets = budgetRepository.findExpiredBudgetsByUser(user.getId(), LocalDate.now());
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting expired budgets for user: {}", username, e);
            throw e;
        }
    }

    @Override
    public void checkBudgetAlerts(String username) {
        try {
            List<BudgetResponse> nearThresholdBudgets = getBudgetsNearThreshold(username);
            List<BudgetResponse> overBudgets = getOverBudgets(username);

            for (BudgetResponse budget : nearThresholdBudgets) {
                webSocketService.sendBudgetAlert(username, 
                    "Budget '" + budget.getName() + "' is " + budget.getUsedPercentage() + "% used", 
                    "WARNING");
            }

            for (BudgetResponse budget : overBudgets) {
                webSocketService.sendBudgetAlert(username, 
                    "Budget '" + budget.getName() + "' has exceeded its limit", 
                    "ERROR");
            }

        } catch (Exception e) {
            logger.error("Error checking budget alerts for user: {}", username, e);
        }
    }

    @Override
    public void sendBudgetAlert(Long budgetId, String username) {
        try {
            BudgetResponse budget = getBudgetById(budgetId, username);
            
            String alertType = budget.getUsedPercentage().compareTo(BigDecimal.valueOf(100)) >= 0 ? "ERROR" : "WARNING";
            String message = String.format("Budget '%s' is %.2f%% used", 
                    budget.getName(), budget.getUsedPercentage());

            webSocketService.sendBudgetAlert(username, message, alertType);

        } catch (Exception e) {
            logger.error("Error sending budget alert for budget: {} to user: {}", budgetId, username, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse.BudgetAlert getBudgetAlert(Long budgetId, String username) {
        try {
            BudgetResponse budget = getBudgetById(budgetId, username);
            
            String alertType = "INFO";
            String alertMessage = "Budget is on track";
            boolean shouldAlert = false;

            if (budget.getUsedPercentage().compareTo(BigDecimal.valueOf(100)) >= 0) {
                alertType = "DANGER";
                alertMessage = "Budget exceeded";
                shouldAlert = true;
            } else if (budget.getUsedPercentage().compareTo(budget.getAlertThreshold()) >= 0) {
                alertType = "WARNING";
                alertMessage = "Budget nearing threshold";
                shouldAlert = true;
            }

            return new BudgetResponse.BudgetAlert(
                    alertType, alertMessage, budget.getUsedPercentage(), 
                    budget.getAlertThreshold(), shouldAlert);

        } catch (Exception e) {
            logger.error("Error getting budget alert for budget: {} by user: {}", budgetId, username, e);
            throw e;
        }
    }

    @Override
    public BudgetResponse createRecurringBudget(BudgetRequest.RecurringBudgetRequest request, String username) {
        try {
            logger.info("Creating recurring budget from budget: {} by user: {}", request.getBudgetId(), username);

            validateBudgetAccess(request.getBudgetId(), username);
            Budget originalBudget = getBudgetEntity(request.getBudgetId());

            // Create new budget based on original
            Budget newBudget = new Budget();
            newBudget.setName(originalBudget.getName() + " (Recurring)");
            newBudget.setDescription(originalBudget.getDescription());
            newBudget.setTotalAmount(originalBudget.getTotalAmount());
            newBudget.setCurrency(originalBudget.getCurrency());
            newBudget.setAlertThreshold(originalBudget.getAlertThreshold());
            newBudget.setIsActive(true);
            newBudget.setIsRecurring(true);
            newBudget.setRecurrencePeriod(request.getRecurrencePeriod());
            newBudget.setUserId(originalBudget.getUserId());
            newBudget.setCategoryId(originalBudget.getCategoryId());
            newBudget.setTeamId(originalBudget.getTeamId());
            newBudget.setSpentAmount(BigDecimal.ZERO);
            newBudget.setRemainingAmount(originalBudget.getTotalAmount());

            // Set dates based on recurrence period
            LocalDate startDate = request.getNextStartDate() != null ? 
                    request.getNextStartDate() : LocalDate.now();
            newBudget.setStartDate(startDate);

            switch (request.getRecurrencePeriod()) {
                case "MONTHLY":
                    newBudget.setEndDate(startDate.plusMonths(1).minusDays(1));
                    break;
                case "QUARTERLY":
                    newBudget.setEndDate(startDate.plusMonths(3).minusDays(1));
                    break;
                case "YEARLY":
                    newBudget.setEndDate(startDate.plusYears(1).minusDays(1));
                    break;
            }

            newBudget = budgetRepository.save(newBudget);

            logger.info("Successfully created recurring budget: {}", newBudget.getId());
            return convertToBudgetResponse(newBudget);

        } catch (Exception e) {
            logger.error("Error creating recurring budget for user: {}", username, e);
            throw e;
        }
    }

    @Override
    public void processRecurringBudgets() {
        try {
            logger.info("Processing recurring budgets");

            List<Budget> recurringBudgets = budgetRepository.findByIsRecurringTrue();
            LocalDate today = LocalDate.now();

            for (Budget budget : recurringBudgets) {
                if (budget.getEndDate().isBefore(today)) {
                    // Create next recurring budget
                    createNextRecurringBudget(budget);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing recurring budgets", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getRecurringBudgets(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            List<Budget> budgets = budgetRepository.findByUserIdAndIsRecurringTrue(user.getId());
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting recurring budgets for user: {}", username, e);
            throw e;
        }
    }

    @Override
    public void transferBudgetAmount(BudgetRequest.BudgetTransferRequest request, String username) {
        try {
            logger.info("Transferring budget amount from {} to {} by user: {}", 
                    request.getSourceBudgetId(), request.getTargetBudgetId(), username);

            validateBudgetAccess(request.getSourceBudgetId(), username);
            validateBudgetAccess(request.getTargetBudgetId(), username);

            Budget sourceBudget = getBudgetEntity(request.getSourceBudgetId());
            Budget targetBudget = getBudgetEntity(request.getTargetBudgetId());

            // Validate transfer amount
            if (sourceBudget.getRemainingAmount().compareTo(request.getAmount()) < 0) {
                throw new BadRequestException("Insufficient budget amount for transfer");
            }

            // Update budget amounts
            sourceBudget.setTotalAmount(sourceBudget.getTotalAmount().subtract(request.getAmount()));
            sourceBudget.setRemainingAmount(sourceBudget.getRemainingAmount().subtract(request.getAmount()));

            targetBudget.setTotalAmount(targetBudget.getTotalAmount().add(request.getAmount()));
            targetBudget.setRemainingAmount(targetBudget.getRemainingAmount().add(request.getAmount()));

            budgetRepository.save(sourceBudget);
            budgetRepository.save(targetBudget);

            logger.info("Successfully transferred {} from budget {} to budget {}", 
                    request.getAmount(), request.getSourceBudgetId(), request.getTargetBudgetId());

        } catch (Exception e) {
            logger.error("Error transferring budget amount by user: {}", username, e);
            throw e;
        }
    }

    @Override
    public BudgetResponse adjustBudgetAmount(Long budgetId, BigDecimal newAmount, String username) {
        try {
            validateBudgetAccess(budgetId, username);
            Budget budget = getBudgetEntity(budgetId);

            budget.setTotalAmount(newAmount);
            budget.setRemainingAmount(newAmount.subtract(budget.getSpentAmount()));

            budget = budgetRepository.save(budget);

            logger.info("Adjusted budget {} amount to {}", budgetId, newAmount);
            return convertToBudgetResponse(budget);

        } catch (Exception e) {
            logger.error("Error adjusting budget amount for budget: {} by user: {}", budgetId, username, e);
            throw e;
        }
    }

    @Override
    public void updateBudgetSpending(Long budgetId, BigDecimal expenseAmount) {
        try {
            Budget budget = getBudgetEntity(budgetId);
            budget.addExpense(expenseAmount);
            budgetRepository.save(budget);

            logger.debug("Updated budget {} spending with amount {}", budgetId, expenseAmount);

        } catch (Exception e) {
            logger.error("Error updating budget spending for budget: {}", budgetId, e);
        }
    }

    @Override
    public void removeBudgetSpending(Long budgetId, BigDecimal expenseAmount) {
        try {
            Budget budget = getBudgetEntity(budgetId);
            budget.removeExpense(expenseAmount);
            budgetRepository.save(budget);

            logger.debug("Removed budget {} spending with amount {}", budgetId, expenseAmount);

        } catch (Exception e) {
            logger.error("Error removing budget spending for budget: {}", budgetId, e);
        }
    }

    @Override
    public void recalculateBudgetSpending(Long budgetId) {
        try {
            Budget budget = getBudgetEntity(budgetId);

            if (budget.getCategoryId() != null) {
                List<Expense> expenses = expenseRepository.findByUserIdAndCategoryIdAndExpenseDateBetween(
                        budget.getUserId(), budget.getCategoryId(),
                        budget.getStartDate().atStartOfDay(),
                        budget.getEndDate().plusDays(1).atStartOfDay());

                BigDecimal totalSpent = expenses.stream()
                        .filter(e -> ExpenseStatus.APPROVED.equals(e.getStatus()))
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                budget.updateSpentAmount(totalSpent);
                budgetRepository.save(budget);

                logger.debug("Recalculated budget {} spending: {}", budgetId, totalSpent);
            }

        } catch (Exception e) {
            logger.error("Error recalculating budget spending for budget: {}", budgetId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getTeamBudgets(Long teamId, String username) {
        try {
            List<Budget> budgets = budgetRepository.findByTeamIdAndIsActiveTrue(teamId);
            
            return budgets.stream()
                    .map(this::convertToBudgetResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting team budgets for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    public BudgetResponse createTeamBudget(Long teamId, BudgetRequest budgetRequest, String username) {
        try {
            budgetRequest.setTeamId(teamId);
            return createBudget(budgetRequest, username);

        } catch (Exception e) {
            logger.error("Error creating team budget for team: {} by user: {}", teamId, username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateBudgetOverlap(String username, Long categoryId, LocalDate startDate, LocalDate endDate, Long excludeId) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            return budgetRepository.hasOverlappingBudget(user.getId(), categoryId, startDate, endDate, excludeId);

        } catch (Exception e) {
            logger.error("Error validating budget overlap for user: {}", username, e);
            return false;
        }
    }

    @Override
    public void validateBudgetAccess(Long budgetId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Budget budget = getBudgetEntity(budgetId);

        if (!budget.getUserId().equals(user.getId())) {
            throw new ForbiddenException("You do not have access to this budget");
        }
    }

    @Override
    public Budget getBudgetEntity(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + budgetId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportBudgets(String username, String format, LocalDate startDate, LocalDate endDate) {
        try {
            List<BudgetResponse> budgets = getBudgetsByDateRange(username, startDate, endDate);
            
            // This is a placeholder - implement actual export logic based on format
            StringBuilder exportContent = new StringBuilder();
            exportContent.append("Budget Export\n");
            exportContent.append("User: ").append(username).append("\n");
            exportContent.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n\n");
            
            for (BudgetResponse budget : budgets) {
                exportContent.append("Budget: ").append(budget.getName()).append("\n");
                exportContent.append("Amount: ").append(budget.getTotalAmount()).append("\n");
                exportContent.append("Spent: ").append(budget.getSpentAmount()).append("\n");
                exportContent.append("Period: ").append(budget.getStartDate()).append(" to ").append(budget.getEndDate()).append("\n\n");
            }
            
            return exportContent.toString().getBytes();

        } catch (Exception e) {
            logger.error("Error exporting budgets for user: {}", username, e);
            throw new RuntimeException("Failed to export budgets", e);
        }
    }

    @Override
    public List<BudgetResponse> importBudgets(byte[] data, String username) {
        // Placeholder for import functionality
        throw new UnsupportedOperationException("Budget import not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse.BudgetSummary> getBudgetSummariesForDashboard(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            LocalDate now = LocalDate.now();
            List<Budget> budgets = budgetRepository.findBudgetsForDashboard(
                    user.getId(), now, now.withDayOfMonth(1));

            return budgets.stream()
                    .map(this::convertToBudgetSummary)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting budget summaries for dashboard for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse.BudgetAnalytics getBudgetAnalyticsForDashboard(String username, LocalDate startDate, LocalDate endDate) {
        try {
            return getBudgetAnalytics(username);

        } catch (Exception e) {
            logger.error("Error getting budget analytics for dashboard for user: {}", username, e);
            throw e;
        }
    }

    // Helper methods
    private void createNextRecurringBudget(Budget originalBudget) {
        try {
            Budget newBudget = new Budget();
            newBudget.setName(originalBudget.getName());
            newBudget.setDescription(originalBudget.getDescription());
            newBudget.setTotalAmount(originalBudget.getTotalAmount());
            newBudget.setCurrency(originalBudget.getCurrency());
            newBudget.setAlertThreshold(originalBudget.getAlertThreshold());
            newBudget.setIsActive(true);
            newBudget.setIsRecurring(true);
            newBudget.setRecurrencePeriod(originalBudget.getRecurrencePeriod());
            newBudget.setUserId(originalBudget.getUserId());
            newBudget.setCategoryId(originalBudget.getCategoryId());
            newBudget.setTeamId(originalBudget.getTeamId());
            newBudget.setSpentAmount(BigDecimal.ZERO);
            newBudget.setRemainingAmount(originalBudget.getTotalAmount());

            // Set new dates
            LocalDate nextStartDate = originalBudget.getEndDate().plusDays(1);
            newBudget.setStartDate(nextStartDate);

            switch (originalBudget.getRecurrencePeriod()) {
                case "MONTHLY":
                    newBudget.setEndDate(nextStartDate.plusMonths(1).minusDays(1));
                    break;
                case "QUARTERLY":
                    newBudget.setEndDate(nextStartDate.plusMonths(3).minusDays(1));
                    break;
                case "YEARLY":
                    newBudget.setEndDate(nextStartDate.plusYears(1).minusDays(1));
                    break;
            }

            budgetRepository.save(newBudget);
            logger.info("Created next recurring budget for budget: {}", originalBudget.getId());

        } catch (Exception e) {
            logger.error("Error creating next recurring budget for budget: {}", originalBudget.getId(), e);
        }
    }

    private BudgetResponse convertToBudgetResponse(Budget budget) {
        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setName(budget.getName());
        response.setDescription(budget.getDescription());
        response.setTotalAmount(budget.getTotalAmount());
        response.setSpentAmount(budget.getSpentAmount());
        response.setRemainingAmount(budget.getRemainingAmount());
        response.setUsedPercentage(budget.getUsedPercentage());
        response.setStartDate(budget.getStartDate());
        response.setEndDate(budget.getEndDate());
        response.setCurrency(budget.getCurrency());
        response.setAlertThreshold(budget.getAlertThreshold());
        response.setIsActive(budget.getIsActive());
        response.setIsRecurring(budget.getIsRecurring());
        response.setRecurrencePeriod(budget.getRecurrencePeriod());
        response.setUserId(budget.getUserId());
        response.setCategoryId(budget.getCategoryId());
        response.setTeamId(budget.getTeamId());
        response.setCreatedAt(budget.getCreatedAt());
        response.setUpdatedAt(budget.getUpdatedAt());

        // Set user info
        if (budget.getUser() != null) {
            response.setUsername(budget.getUser().getUsername());
        }

        // Set category info
        if (budget.getCategory() != null) {
            response.setCategoryName(budget.getCategory().getName());
        }

        // Set team info
        if (budget.getTeam() != null) {
            response.setTeamName(budget.getTeam().getName());
        }

        // Set status
        response.setStatus(createBudgetStatus(budget));

        // Set alert
        response.setAlert(createBudgetAlert(budget));

        return response;
    }

    private BudgetResponse.BudgetSummary convertToBudgetSummary(Budget budget) {
        String status = budget.isOverBudget() ? "OVER_BUDGET" : 
                       budget.isNearThreshold() ? "NEAR_THRESHOLD" : "ON_TRACK";

        return new BudgetResponse.BudgetSummary(
                budget.getId(),
                budget.getName(),
                budget.getTotalAmount(),
                budget.getSpentAmount(),
                status,
                budget.getEndDate()
        );
    }

    private BudgetResponse.BudgetStatus createBudgetStatus(Budget budget) {
        String status = "ACTIVE";
        String statusMessage = "Budget is active";
        boolean isOverBudget = budget.isOverBudget();
        boolean isNearThreshold = budget.isNearThreshold();
        boolean isExpired = budget.isExpired();

        if (isExpired) {
            status = "EXPIRED";
            statusMessage = "Budget has expired";
        } else if (isOverBudget) {
            status = "OVER_BUDGET";
            statusMessage = "Budget has been exceeded";
        } else if (isNearThreshold) {
            status = "NEAR_THRESHOLD";
            statusMessage = "Budget is nearing threshold";
        } else if (!budget.getIsActive()) {
            status = "INACTIVE";
            statusMessage = "Budget is inactive";
        }

        return new BudgetResponse.BudgetStatus(status, statusMessage, isOverBudget, isNearThreshold, isExpired);
    }

    private BudgetResponse.BudgetAlert createBudgetAlert(Budget budget) {
        String alertType = "INFO";
        String alertMessage = "Budget is on track";
        boolean shouldAlert = false;

        if (budget.isOverBudget()) {
            alertType = "DANGER";
            alertMessage = "Budget exceeded";
            shouldAlert = true;
        } else if (budget.isNearThreshold()) {
            alertType = "WARNING";
            alertMessage = "Budget nearing threshold";
            shouldAlert = true;
        }

        return new BudgetResponse.BudgetAlert(
                alertType, alertMessage, budget.getUsedPercentage(), 
                budget.getAlertThreshold(), shouldAlert);
    }
}