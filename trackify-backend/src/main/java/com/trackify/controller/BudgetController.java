package com.trackify.controller;

import com.trackify.dto.request.BudgetRequest;
import com.trackify.dto.response.ApiResponse;
import com.trackify.dto.response.BudgetResponse;
import com.trackify.service.BudgetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/budgets")
@Tag(name = "Budget Management", description = "Budget and budget tracking API")
@CrossOrigin(origins = "*")
public class BudgetController {

    private static final Logger logger = LoggerFactory.getLogger(BudgetController.class);

    @Autowired
    private BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Create budget", description = "Create a new budget")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Budget created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @Valid @RequestBody BudgetRequest budgetRequest,
            Authentication authentication) {
        try {
            logger.info("Creating budget: {} by user: {}", budgetRequest.getName(), authentication.getName());
            
            BudgetResponse budgetResponse = budgetService.createBudget(budgetRequest, authentication.getName());
            
            return ResponseEntity.status(201).body(ApiResponse.success(
                    "Budget created successfully",
                    budgetResponse
            ));
        } catch (Exception e) {
            logger.error("Error creating budget by user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to create budget: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{budgetId}")
    @Operation(summary = "Update budget", description = "Update budget information")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @Parameter(description = "Budget ID") @PathVariable Long budgetId,
            @Valid @RequestBody BudgetRequest.UpdateBudgetRequest updateRequest,
            Authentication authentication) {
        try {
            logger.info("Updating budget: {} by user: {}", budgetId, authentication.getName());
            
            BudgetResponse budgetResponse = budgetService.updateBudget(budgetId, updateRequest, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget updated successfully",
                    budgetResponse
            ));
        } catch (Exception e) {
            logger.error("Error updating budget: {} by user: {}", budgetId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to update budget: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{budgetId}")
    @Operation(summary = "Get budget by ID", description = "Retrieve budget information by ID")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(
            @Parameter(description = "Budget ID") @PathVariable Long budgetId,
            Authentication authentication) {
        try {
            BudgetResponse budgetResponse = budgetService.getBudgetById(budgetId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget retrieved successfully",
                    budgetResponse
            ));
        } catch (Exception e) {
            logger.error("Error getting budget: {} by user: {}", budgetId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budget: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    @Operation(summary = "Get user budgets", description = "Get all budgets for the authenticated user")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getUserBudgets(Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.getUserBudgets(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting budgets for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budgets: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/page")
    @Operation(summary = "Get user budgets (paginated)", description = "Get budgets for the authenticated user with pagination")
    public ResponseEntity<ApiResponse<Page<BudgetResponse>>> getUserBudgets(
            Pageable pageable,
            Authentication authentication) {
        try {
            Page<BudgetResponse> budgets = budgetService.getUserBudgets(authentication.getName(), pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting budgets page for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budgets: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{budgetId}")
    @Operation(summary = "Delete budget", description = "Delete a budget")
    public ResponseEntity<ApiResponse<String>> deleteBudget(
            @Parameter(description = "Budget ID") @PathVariable Long budgetId,
            Authentication authentication) {
        try {
            logger.info("Deleting budget: {} by user: {}", budgetId, authentication.getName());
            
            budgetService.deleteBudget(budgetId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget deleted successfully",
                    "Budget has been permanently deleted"
            ));
        } catch (Exception e) {
            logger.error("Error deleting budget: {} by user: {}", budgetId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to delete budget: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{budgetId}/activate")
    @Operation(summary = "Activate budget", description = "Activate a budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> activateBudget(
            @Parameter(description = "Budget ID") @PathVariable Long budgetId,
            Authentication authentication) {
        try {
            BudgetResponse budgetResponse = budgetService.activateBudget(budgetId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget activated successfully",
                    budgetResponse
            ));
        } catch (Exception e) {
            logger.error("Error activating budget: {} by user: {}", budgetId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to activate budget: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{budgetId}/deactivate")
    @Operation(summary = "Deactivate budget", description = "Deactivate a budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> deactivateBudget(
            @Parameter(description = "Budget ID") @PathVariable Long budgetId,
            Authentication authentication) {
        try {
            BudgetResponse budgetResponse = budgetService.deactivateBudget(budgetId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget deactivated successfully",
                    budgetResponse
            ));
        } catch (Exception e) {
            logger.error("Error deactivating budget: {} by user: {}", budgetId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to deactivate budget: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Get active budgets", description = "Get all active budgets for the user")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getActiveBudgets(Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.getActiveBudgets(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Active budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting active budgets for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve active budgets: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get budgets by category", description = "Get budgets for a specific category")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgetsByCategory(
            @Parameter(description = "Category ID") @PathVariable Long categoryId,
            Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.getBudgetsByCategory(categoryId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Category budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting budgets by category: {} for user: {}", categoryId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve category budgets: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "Get team budgets", description = "Get budgets for a specific team")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getTeamBudgets(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.getTeamBudgets(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting team budgets for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team budgets: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get budgets by date range", description = "Get budgets within a specific date range")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgetsByDateRange(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.getBudgetsByDateRange(authentication.getName(), startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Date range budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting budgets by date range for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve date range budgets: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search budgets", description = "Search budgets by keyword")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> searchBudgets(
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.searchBudgets(authentication.getName(), keyword);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget search completed successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error searching budgets for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to search budgets: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get budget analytics", description = "Get comprehensive budget analytics")
    public ResponseEntity<ApiResponse<BudgetResponse.BudgetAnalytics>> getBudgetAnalytics(
            Authentication authentication) {
        try {
            BudgetResponse.BudgetAnalytics analytics = budgetService.getBudgetAnalytics(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget analytics retrieved successfully",
                    analytics
            ));
        } catch (Exception e) {
            logger.error("Error getting budget analytics for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budget analytics: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/near-threshold")
    @Operation(summary = "Get budgets near threshold", description = "Get budgets that are near their alert threshold")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgetsNearThreshold(
            Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.getBudgetsNearThreshold(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Near threshold budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting near threshold budgets for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve near threshold budgets: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/over-budget")
    @Operation(summary = "Get over-budget budgets", description = "Get budgets that have exceeded their limit")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getOverBudgets(
            Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.getOverBudgets(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Over-budget budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting over-budget budgets for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve over-budget budgets: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/expired")
    @Operation(summary = "Get expired budgets", description = "Get budgets that have expired")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getExpiredBudgets(
            Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.getExpiredBudgets(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Expired budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting expired budgets for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve expired budgets: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/check-alerts")
    @Operation(summary = "Check budget alerts", description = "Check and send budget alerts for the user")
    public ResponseEntity<ApiResponse<String>> checkBudgetAlerts(Authentication authentication) {
        try {
            budgetService.checkBudgetAlerts(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget alerts checked successfully",
                    "All budget alerts have been processed"
            ));
        } catch (Exception e) {
            logger.error("Error checking budget alerts for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to check budget alerts: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{budgetId}/send-alert")
    @Operation(summary = "Send budget alert", description = "Send alert for a specific budget")
    public ResponseEntity<ApiResponse<String>> sendBudgetAlert(
            @Parameter(description = "Budget ID") @PathVariable Long budgetId,
            Authentication authentication) {
        try {
            budgetService.sendBudgetAlert(budgetId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget alert sent successfully",
                    "Alert has been sent for the budget"
            ));
        } catch (Exception e) {
            logger.error("Error sending budget alert for budget: {} by user: {}", budgetId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to send budget alert: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{budgetId}/alert")
    @Operation(summary = "Get budget alert", description = "Get alert information for a specific budget")
    public ResponseEntity<ApiResponse<BudgetResponse.BudgetAlert>> getBudgetAlert(
            @Parameter(description = "Budget ID") @PathVariable Long budgetId,
            Authentication authentication) {
        try {
            BudgetResponse.BudgetAlert alert = budgetService.getBudgetAlert(budgetId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget alert retrieved successfully",
                    alert
            ));
        } catch (Exception e) {
            logger.error("Error getting budget alert for budget: {} by user: {}", budgetId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budget alert: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/recurring")
    @Operation(summary = "Create recurring budget", description = "Create a recurring budget from an existing budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> createRecurringBudget(
            @Valid @RequestBody BudgetRequest.RecurringBudgetRequest request,
            Authentication authentication) {
        try {
            BudgetResponse budgetResponse = budgetService.createRecurringBudget(request, authentication.getName());
            
            return ResponseEntity.status(201).body(ApiResponse.success(
                    "Recurring budget created successfully",
                    budgetResponse
            ));
        } catch (Exception e) {
            logger.error("Error creating recurring budget by user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to create recurring budget: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/recurring")
    @Operation(summary = "Get recurring budgets", description = "Get all recurring budgets for the user")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getRecurringBudgets(
            Authentication authentication) {
        try {
            List<BudgetResponse> budgets = budgetService.getRecurringBudgets(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Recurring budgets retrieved successfully",
                    budgets
            ));
        } catch (Exception e) {
            logger.error("Error getting recurring budgets for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve recurring budgets: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer budget amount", description = "Transfer amount from one budget to another")
    public ResponseEntity<ApiResponse<String>> transferBudgetAmount(
            @Valid @RequestBody BudgetRequest.BudgetTransferRequest request,
            Authentication authentication) {
        try {
            budgetService.transferBudgetAmount(request, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget transfer completed successfully",
                    "Amount has been transferred between budgets"
            ));
        } catch (Exception e) {
            logger.error("Error transferring budget amount by user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to transfer budget amount: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{budgetId}/adjust-amount")
    @Operation(summary = "Adjust budget amount", description = "Adjust the total amount for a budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> adjustBudgetAmount(
            @Parameter(description = "Budget ID") @PathVariable Long budgetId,
            @Parameter(description = "New amount") @RequestParam BigDecimal newAmount,
            Authentication authentication) {
        try {
            BudgetResponse budgetResponse = budgetService.adjustBudgetAmount(budgetId, newAmount, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget amount adjusted successfully",
                    budgetResponse
            ));
        } catch (Exception e) {
            logger.error("Error adjusting budget amount for budget: {} by user: {}", budgetId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to adjust budget amount: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/team/{teamId}")
    @Operation(summary = "Create team budget", description = "Create a budget for a specific team")
    public ResponseEntity<ApiResponse<BudgetResponse>> createTeamBudget(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Valid @RequestBody BudgetRequest budgetRequest,
            Authentication authentication) {
        try {
            BudgetResponse budgetResponse = budgetService.createTeamBudget(teamId, budgetRequest, authentication.getName());
            
            return ResponseEntity.status(201).body(ApiResponse.success(
                    "Team budget created successfully",
                    budgetResponse
            ));
        } catch (Exception e) {
            logger.error("Error creating team budget for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to create team budget: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Export budgets", description = "Export budgets in specified format")
    public ResponseEntity<byte[]> exportBudgets(
            @Parameter(description = "Export format") @RequestParam(defaultValue = "CSV") String format,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            byte[] exportData = budgetService.exportBudgets(authentication.getName(), format, startDate, endDate);
            
            String filename = String.format("budgets_%s_%s_to_%s.%s", 
                    authentication.getName(), startDate, endDate, format.toLowerCase());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            
            MediaType mediaType = switch (format.toUpperCase()) {
                case "PDF" -> MediaType.APPLICATION_PDF;
                case "CSV" -> MediaType.parseMediaType("text/csv");
                case "XLSX" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(mediaType)
                    .body(exportData);
                    
        } catch (Exception e) {
            logger.error("Error exporting budgets for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/dashboard/summaries")
    @Operation(summary = "Get budget summaries for dashboard", description = "Get budget summaries optimized for dashboard display")
    public ResponseEntity<ApiResponse<List<BudgetResponse.BudgetSummary>>> getBudgetSummariesForDashboard(
            Authentication authentication) {
        try {
            List<BudgetResponse.BudgetSummary> summaries = budgetService.getBudgetSummariesForDashboard(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget summaries retrieved successfully",
                    summaries
            ));
        } catch (Exception e) {
            logger.error("Error getting budget summaries for dashboard for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budget summaries: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/dashboard/analytics")
    @Operation(summary = "Get budget analytics for dashboard", description = "Get budget analytics optimized for dashboard display")
    public ResponseEntity<ApiResponse<BudgetResponse.BudgetAnalytics>> getBudgetAnalyticsForDashboard(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            BudgetResponse.BudgetAnalytics analytics = budgetService.getBudgetAnalyticsForDashboard(
                    authentication.getName(), startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget analytics retrieved successfully",
                    analytics
            ));
        } catch (Exception e) {
            logger.error("Error getting budget analytics for dashboard for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budget analytics: " + e.getMessage()
            ));
        }
    }
}