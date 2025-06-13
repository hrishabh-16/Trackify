package com.trackify.controller;

import com.trackify.dto.response.ApiResponse;
import com.trackify.dto.response.DashboardResponse;
import com.trackify.service.DashboardService;
import com.trackify.service.WebSocketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Dashboard and Analytics API")
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private WebSocketService webSocketService;

    @GetMapping
    @Operation(summary = "Get dashboard data", description = "Retrieve comprehensive dashboard data for the authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardData(Authentication authentication) {
        try {
            logger.info("Getting dashboard data for user: {}", authentication.getName());
            
            DashboardResponse dashboardData = dashboardService.getDashboardData(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Dashboard data retrieved successfully",
                    dashboardData
            ));
        } catch (Exception e) {
            logger.error("Error getting dashboard data for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve dashboard data: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "Get team dashboard data", description = "Retrieve dashboard data for a specific team")
    public ResponseEntity<ApiResponse<DashboardResponse>> getTeamDashboardData(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            logger.info("Getting team dashboard data for team: {} by user: {}", teamId, authentication.getName());
            
            DashboardResponse teamDashboardData = dashboardService.getTeamDashboardData(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team dashboard data retrieved successfully",
                    teamDashboardData
            ));
        } catch (Exception e) {
            logger.error("Error getting team dashboard data for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team dashboard data: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/expense-summary")
    @Operation(summary = "Get expense summary", description = "Retrieve expense summary for a specific date range")
    public ResponseEntity<ApiResponse<DashboardResponse.ExpenseSummary>> getExpenseSummary(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            logger.info("Getting expense summary for user: {} from {} to {}", authentication.getName(), startDate, endDate);
            
            DashboardResponse.ExpenseSummary expenseSummary = dashboardService.getExpenseSummary(
                    authentication.getName(), startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Expense summary retrieved successfully",
                    expenseSummary
            ));
        } catch (Exception e) {
            logger.error("Error getting expense summary for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve expense summary: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/budget-summary")
    @Operation(summary = "Get budget summary", description = "Retrieve budget summary for a specific date range")
    public ResponseEntity<ApiResponse<DashboardResponse.BudgetSummary>> getBudgetSummary(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            logger.info("Getting budget summary for user: {} from {} to {}", authentication.getName(), startDate, endDate);
            
            DashboardResponse.BudgetSummary budgetSummary = dashboardService.getBudgetSummary(
                    authentication.getName(), startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget summary retrieved successfully",
                    budgetSummary
            ));
        } catch (Exception e) {
            logger.error("Error getting budget summary for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budget summary: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/monthly-expenses")
    @Operation(summary = "Get monthly expense data", description = "Retrieve monthly expense data for chart visualization")
    public ResponseEntity<ApiResponse<List<DashboardResponse.MonthlyExpense>>> getMonthlyExpenseData(
            @Parameter(description = "Number of months") @RequestParam(defaultValue = "12") int months,
            Authentication authentication) {
        try {
            logger.info("Getting monthly expense data for user: {} for {} months", authentication.getName(), months);
            
            List<DashboardResponse.MonthlyExpense> monthlyData = dashboardService.getMonthlyExpenseData(
                    authentication.getName(), months);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Monthly expense data retrieved successfully",
                    monthlyData
            ));
        } catch (Exception e) {
            logger.error("Error getting monthly expense data for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve monthly expense data: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/category-expenses")
    @Operation(summary = "Get category expense data", description = "Retrieve expense data grouped by category")
    public ResponseEntity<ApiResponse<List<DashboardResponse.CategoryExpense>>> getCategoryExpenseData(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            logger.info("Getting category expense data for user: {} from {} to {}", authentication.getName(), startDate, endDate);
            
            List<DashboardResponse.CategoryExpense> categoryData = dashboardService.getCategoryExpenseData(
                    authentication.getName(), startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Category expense data retrieved successfully",
                    categoryData
            ));
        } catch (Exception e) {
            logger.error("Error getting category expense data for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve category expense data: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/daily-expenses")
    @Operation(summary = "Get daily expense data", description = "Retrieve daily expense data for a specific date range")
    public ResponseEntity<ApiResponse<List<DashboardResponse.DailyExpense>>> getDailyExpenseData(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            logger.info("Getting daily expense data for user: {} from {} to {}", authentication.getName(), startDate, endDate);
            
            List<DashboardResponse.DailyExpense> dailyData = dashboardService.getDailyExpenseData(
                    authentication.getName(), startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Daily expense data retrieved successfully",
                    dailyData
            ));
        } catch (Exception e) {
            logger.error("Error getting daily expense data for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve daily expense data: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/expense-trend")
    @Operation(summary = "Get expense trend", description = "Retrieve expense trend analysis")
    public ResponseEntity<ApiResponse<DashboardResponse.ExpenseTrend>> getExpenseTrend(
            @Parameter(description = "Number of months") @RequestParam(defaultValue = "12") int months,
            Authentication authentication) {
        try {
            logger.info("Getting expense trend for user: {} for {} months", authentication.getName(), months);
            
            DashboardResponse.ExpenseTrend expenseTrend = dashboardService.getExpenseTrend(
                    authentication.getName(), months);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Expense trend retrieved successfully",
                    expenseTrend
            ));
        } catch (Exception e) {
            logger.error("Error getting expense trend for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve expense trend: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/top-categories")
    @Operation(summary = "Get top categories", description = "Retrieve top spending categories")
    public ResponseEntity<ApiResponse<List<DashboardResponse.TopCategory>>> getTopCategories(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Limit") @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        try {
            logger.info("Getting top categories for user: {} from {} to {} limit {}", 
                    authentication.getName(), startDate, endDate, limit);
            
            List<DashboardResponse.TopCategory> topCategories = dashboardService.getTopCategories(
                    authentication.getName(), startDate, endDate, limit);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Top categories retrieved successfully",
                    topCategories
            ));
        } catch (Exception e) {
            logger.error("Error getting top categories for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve top categories: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/recent-expenses")
    @Operation(summary = "Get recent expenses", description = "Retrieve recent expenses")
    public ResponseEntity<ApiResponse<List<DashboardResponse.RecentExpense>>> getRecentExpenses(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        try {
            logger.info("Getting recent expenses for user: {} limit {}", authentication.getName(), limit);
            
            List<DashboardResponse.RecentExpense> recentExpenses = dashboardService.getRecentExpenses(
                    authentication.getName(), limit);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Recent expenses retrieved successfully",
                    recentExpenses
            ));
        } catch (Exception e) {
            logger.error("Error getting recent expenses for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve recent expenses: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/budget-status")
    @Operation(summary = "Get budget status", description = "Retrieve budget status for all active budgets")
    public ResponseEntity<ApiResponse<List<DashboardResponse.BudgetStatus>>> getBudgetStatus(
            Authentication authentication) {
        try {
            logger.info("Getting budget status for user: {}", authentication.getName());
            
            List<DashboardResponse.BudgetStatus> budgetStatus = dashboardService.getBudgetStatusList(
                    authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget status retrieved successfully",
                    budgetStatus
            ));
        } catch (Exception e) {
            logger.error("Error getting budget status for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budget status: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/budget-alert")
    @Operation(summary = "Get budget alert", description = "Retrieve budget alert information")
    public ResponseEntity<ApiResponse<DashboardResponse.BudgetAlert>> getBudgetAlert(
            Authentication authentication) {
        try {
            logger.info("Getting budget alert for user: {}", authentication.getName());
            
            DashboardResponse.BudgetAlert budgetAlert = dashboardService.getBudgetAlert(
                    authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Budget alert retrieved successfully",
                    budgetAlert
            ));
        } catch (Exception e) {
            logger.error("Error getting budget alert for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve budget alert: " + e.getMessage()
            ));
        }
    }
    @PostMapping("/refresh")
    @Operation(summary = "Refresh dashboard", description = "Refresh dashboard data and send real-time update")
    public ResponseEntity<ApiResponse<String>> refreshDashboard(Authentication authentication) {
        try {
            logger.info("Refreshing dashboard for user: {}", authentication.getName());
            
            // Invalidate cache and refresh dashboard
            dashboardService.invalidateDashboardCache(authentication.getName());
            webSocketService.refreshDashboard(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Dashboard refreshed successfully",
                    "Dashboard data has been updated"
            ));
        } catch (Exception e) {
            logger.error("Error refreshing dashboard for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to refresh dashboard: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Export dashboard data", description = "Export dashboard data in specified format")
    public ResponseEntity<byte[]> exportDashboardData(
            @Parameter(description = "Export format") @RequestParam(defaultValue = "PDF") String format,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            logger.info("Exporting dashboard data for user: {} in format: {} from {} to {}", 
                    authentication.getName(), format, startDate, endDate);
            
            byte[] exportData = dashboardService.exportDashboardData(
                    authentication.getName(), format, startDate, endDate);
            
            String filename = String.format("dashboard_%s_%s_to_%s.%s", 
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
            logger.error("Error exporting dashboard data for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/comparison/month")
    @Operation(summary = "Get month comparison", description = "Compare current month with previous month")
    public ResponseEntity<ApiResponse<DashboardResponse.ComparisonData>> getMonthComparison(
            @Parameter(description = "Current month") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentMonth,
            @Parameter(description = "Previous month") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate previousMonth,
            Authentication authentication) {
        try {
            logger.info("Getting month comparison for user: {} between {} and {}", 
                    authentication.getName(), currentMonth, previousMonth);
            
            DashboardResponse.ComparisonData comparisonData = dashboardService.getMonthComparison(
                    authentication.getName(), currentMonth, previousMonth);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Month comparison retrieved successfully",
                    comparisonData
            ));
        } catch (Exception e) {
            logger.error("Error getting month comparison for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve month comparison: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/comparison/year")
    @Operation(summary = "Get yearly comparison", description = "Compare current year with previous year")
    public ResponseEntity<ApiResponse<DashboardResponse.YearlyComparison>> getYearlyComparison(
            @Parameter(description = "Current year") @RequestParam int currentYear,
            @Parameter(description = "Previous year") @RequestParam int previousYear,
            Authentication authentication) {
        try {
            logger.info("Getting yearly comparison for user: {} between {} and {}", 
                    authentication.getName(), currentYear, previousYear);
            
            DashboardResponse.YearlyComparison yearlyComparison = dashboardService.getYearlyComparison(
                    authentication.getName(), currentYear, previousYear);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Yearly comparison retrieved successfully",
                    yearlyComparison
            ));
        } catch (Exception e) {
            logger.error("Error getting yearly comparison for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve yearly comparison: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/websocket/status")
    @Operation(summary = "Get WebSocket status", description = "Get real-time connection status and online users")
    public ResponseEntity<ApiResponse<Object>> getWebSocketStatus(Authentication authentication) {
        try {
            logger.info("Getting WebSocket status for user: {}", authentication.getName());
            
            Set<String> onlineUsers = webSocketService.getOnlineUsers();
            boolean isUserOnline = webSocketService.isUserOnline(authentication.getName());
            int activeSessionCount = webSocketService.getActiveSessionCount();
            
            // Create a Map to hold the status data instead of using an anonymous class
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("isOnline", isUserOnline);
            statusData.put("onlineUsers", onlineUsers);
            statusData.put("totalActiveSessions", activeSessionCount);
            statusData.put("currentUser", authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "WebSocket status retrieved successfully",
                    statusData
            ));
        } catch (Exception e) {
            logger.error("Error getting WebSocket status for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve WebSocket status: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/websocket/broadcast")
    @Operation(summary = "Broadcast dashboard update", description = "Broadcast dashboard update to all connected users")
    public ResponseEntity<ApiResponse<String>> broadcastDashboardUpdate(
            @RequestBody(required = false) Object customData,
            Authentication authentication) {
        try {
            logger.info("Broadcasting dashboard update initiated by user: {}", authentication.getName());
            
            if (customData != null) {
                webSocketService.broadcastDashboardUpdate(customData);
            } else {
                // Get fresh dashboard data for broadcast
                DashboardResponse dashboardData = dashboardService.getDashboardData(authentication.getName());
                webSocketService.broadcastDashboardUpdate(dashboardData);
            }
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Dashboard update broadcasted successfully",
                    "All connected users will receive the update"
            ));
        } catch (Exception e) {
            logger.error("Error broadcasting dashboard update for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to broadcast dashboard update: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/team/{teamId}/summary")
    @Operation(summary = "Get team summary", description = "Get summary data for a specific team")
    public ResponseEntity<ApiResponse<DashboardResponse.TeamSummary>> getTeamSummary(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            Authentication authentication) {
        try {
            logger.info("Getting team summary for team: {} by user: {}", teamId, authentication.getName());
            
            DashboardResponse.TeamSummary teamSummary = dashboardService.getTeamSummary(teamId, authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team summary retrieved successfully",
                    teamSummary
            ));
        } catch (Exception e) {
            logger.error("Error getting team summary for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team summary: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/team/{teamId}/member-expenses")
    @Operation(summary = "Get team member expenses", description = "Get expense data for all team members")
    public ResponseEntity<ApiResponse<List<DashboardResponse.TeamMemberExpense>>> getTeamMemberExpenses(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        try {
            logger.info("Getting team member expenses for team: {} by user: {} from {} to {}", 
                    teamId, authentication.getName(), startDate, endDate);
            
            List<DashboardResponse.TeamMemberExpense> teamMemberExpenses = dashboardService.getTeamMemberExpenses(
                    teamId, startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Team member expenses retrieved successfully",
                    teamMemberExpenses
            ));
        } catch (Exception e) {
            logger.error("Error getting team member expenses for team: {} by user: {}", teamId, authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to retrieve team member expenses: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/cache/invalidate")
    @Operation(summary = "Invalidate cache", description = "Invalidate dashboard cache for the current user")
    public ResponseEntity<ApiResponse<String>> invalidateCache(Authentication authentication) {
        try {
            logger.info("Invalidating dashboard cache for user: {}", authentication.getName());
            
            dashboardService.invalidateDashboardCache(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Cache invalidated successfully",
                    "Dashboard cache has been cleared for user: " + authentication.getName()
            ));
        } catch (Exception e) {
            logger.error("Error invalidating cache for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to invalidate cache: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/cache/update")
    @Operation(summary = "Update cache", description = "Update dashboard cache for the current user")
    public ResponseEntity<ApiResponse<String>> updateCache(Authentication authentication) {
        try {
            logger.info("Updating dashboard cache for user: {}", authentication.getName());
            
            dashboardService.updateDashboardCache(authentication.getName());
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Cache updated successfully",
                    "Dashboard cache has been refreshed for user: " + authentication.getName()
            ));
        } catch (Exception e) {
            logger.error("Error updating cache for user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to update cache: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/notification/test")
    @Operation(summary = "Test notification", description = "Send a test notification via WebSocket")
    public ResponseEntity<ApiResponse<String>> sendTestNotification(
            @RequestParam(defaultValue = "Test notification from dashboard") String message,
            Authentication authentication) {
        try {
            logger.info("Sending test notification to user: {}", authentication.getName());
            
            webSocketService.sendSystemNotification(
                    authentication.getName(),
                    "Dashboard Test",
                    message
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Test notification sent successfully",
                    "Notification sent to user: " + authentication.getName()
            ));
        } catch (Exception e) {
            logger.error("Error sending test notification to user: {}", authentication.getName(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(
                    "Failed to send test notification: " + e.getMessage()
            ));
        }
    }
}