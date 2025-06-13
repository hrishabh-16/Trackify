package com.trackify.controller;

import com.trackify.dto.request.ExpenseRequest;
import com.trackify.dto.response.ApiResponse;
import com.trackify.dto.response.ExpenseResponse;
import com.trackify.enums.ExpenseStatus;
import com.trackify.security.UserPrincipal;
import com.trackify.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Expense Management", description = "APIs for managing expenses")
public class ExpenseController {
    
    @Autowired
    private ExpenseService expenseService;
    
    @PostMapping
    @Operation(summary = "Create expense", description = "Create a new expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody ExpenseRequest expenseRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        ExpenseResponse expense = expenseService.createExpense(expenseRequest, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Expense created successfully", expense));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID", description = "Retrieve expense details by ID")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpenseById(
            @Parameter(description = "Expense ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        ExpenseResponse expense = expenseService.getExpenseById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Expense retrieved successfully", expense));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update expense", description = "Update an existing expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @Parameter(description = "Expense ID") @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest expenseRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        ExpenseResponse expense = expenseService.updateExpense(id, expenseRequest, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully", expense));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete expense", description = "Delete an expense")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @Parameter(description = "Expense ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        expenseService.deleteExpense(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully", null));
    }
    
    @GetMapping
    @Operation(summary = "Get user expenses", description = "Retrieve expenses for the current user with pagination")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getUserExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        Sort sort = sortDirection.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ExpenseResponse> expenses = expenseService.getUserExpensesPaginated(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Expenses retrieved successfully", expenses));
    }
    
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get expenses by category", description = "Retrieve expenses by category")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpensesByCategory(
            @Parameter(description = "Category ID") @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        Page<ExpenseResponse> expenses = expenseService.getExpensesByCategoryPaginated(
            categoryId, currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Category expenses retrieved successfully", expenses));
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get expenses by status", description = "Retrieve expenses by status")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpensesByStatus(
            @Parameter(description = "Expense status") @PathVariable ExpenseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        Page<ExpenseResponse> expenses = expenseService.getExpensesByStatusPaginated(
            currentUser.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Status-filtered expenses retrieved successfully", expenses));
    }
    
    @GetMapping("/date-range")
    @Operation(summary = "Get expenses by date range", description = "Retrieve expenses within a date range")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpensesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        Page<ExpenseResponse> expenses = expenseService.getExpensesByDateRangePaginated(
            currentUser.getId(), startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Date-filtered expenses retrieved successfully", expenses));
    }
    
    @GetMapping("/amount-range")
    @Operation(summary = "Get expenses by amount range", description = "Retrieve expenses within an amount range")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getExpensesByAmountRange(
            @RequestParam BigDecimal minAmount,
            @RequestParam BigDecimal maxAmount,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<ExpenseResponse> expenses = expenseService.getExpensesByAmountRange(
            currentUser.getId(), minAmount, maxAmount);
        return ResponseEntity.ok(ApiResponse.success("Amount-filtered expenses retrieved successfully", expenses));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search expenses", description = "Search expenses by keyword")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> searchExpenses(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        Page<ExpenseResponse> expenses = expenseService.searchExpenses(currentUser.getId(), keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", expenses));
    }
    
    @GetMapping("/team/{teamId}")
    @Operation(summary = "Get team expenses", description = "Retrieve expenses for a specific team")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getTeamExpenses(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        Page<ExpenseResponse> expenses = expenseService.getTeamExpensesPaginated(teamId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Team expenses retrieved successfully", expenses));
    }
    
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve expense", description = "Approve a pending expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> approveExpense(
            @Parameter(description = "Expense ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        ExpenseResponse expense = expenseService.approveExpense(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Expense approved successfully", expense));
    }
    
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject expense", description = "Reject a pending expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> rejectExpense(
            @Parameter(description = "Expense ID") @PathVariable Long id,
            @RequestParam String rejectionReason,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        ExpenseResponse expense = expenseService.rejectExpense(id, currentUser.getId(), rejectionReason);
        return ResponseEntity.ok(ApiResponse.success("Expense rejected successfully", expense));
    }
    
    @GetMapping("/pending-approval")
    @Operation(summary = "Get pending expenses", description = "Retrieve expenses pending approval")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getPendingExpensesForApproval() {
        
        List<ExpenseResponse> expenses = expenseService.getPendingExpensesForApproval();
        return ResponseEntity.ok(ApiResponse.success("Pending expenses retrieved successfully", expenses));
    }
    
    @GetMapping("/reimbursable")
    @Operation(summary = "Get reimbursable expenses", description = "Retrieve reimbursable expenses")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getReimbursableExpenses(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<ExpenseResponse> expenses = expenseService.getReimbursableExpenses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Reimbursable expenses retrieved successfully", expenses));
    }
    
    @GetMapping("/unreimbursed")
    @Operation(summary = "Get unreimbursed expenses", description = "Retrieve unreimbursed expenses")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getUnreimbursedExpenses(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<ExpenseResponse> expenses = expenseService.getUnreimbursedExpenses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Unreimbursed expenses retrieved successfully", expenses));
    }
    
    @PostMapping("/{id}/mark-reimbursed")
    @Operation(summary = "Mark as reimbursed", description = "Mark an expense as reimbursed")
    public ResponseEntity<ApiResponse<ExpenseResponse>> markAsReimbursed(
            @Parameter(description = "Expense ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        ExpenseResponse expense = expenseService.markAsReimbursed(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Expense marked as reimbursed successfully", expense));
    }
    
    @GetMapping("/recent")
    @Operation(summary = "Get recent expenses", description = "Retrieve recent expenses")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getRecentExpenses(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<ExpenseResponse> expenses = expenseService.getRecentExpenses(currentUser.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success("Recent expenses retrieved successfully", expenses));
    }
    
    @GetMapping("/recurring")
    @Operation(summary = "Get recurring expenses", description = "Retrieve recurring expenses")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getRecurringExpenses(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<ExpenseResponse> expenses = expenseService.getRecurringExpenses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Recurring expenses retrieved successfully", expenses));
    }
    
    @GetMapping("/statistics/total")
    @Operation(summary = "Get total expense amount", description = "Get total expense amount for user")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalExpenseAmount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        BigDecimal total = expenseService.getTotalExpenseAmount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Total expense amount retrieved successfully", total));
    }
    
    @GetMapping("/statistics/total-by-date")
    @Operation(summary = "Get total by date range", description = "Get total expense amount by date range")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalExpenseAmountByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        BigDecimal total = expenseService.getTotalExpenseAmountByDateRange(currentUser.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Total expense amount by date range retrieved successfully", total));
    }
    
    @GetMapping("/statistics/count")
    @Operation(summary = "Get expense count", description = "Get total expense count for user")
    public ResponseEntity<ApiResponse<Long>> getExpenseCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        long count = expenseService.getExpenseCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Expense count retrieved successfully", count));
    }
    
    @GetMapping("/statistics/monthly-summary")
    @Operation(summary = "Get monthly summary", description = "Get monthly expense summary")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMonthlySummary(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<Map<String, Object>> summary = expenseService.getMonthlySummary(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Monthly summary retrieved successfully", summary));
    }
    
    @GetMapping("/statistics/category-summary")
    @Operation(summary = "Get category summary", description = "Get category-wise expense summary")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategorySummary(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<Map<String, Object>> summary = expenseService.getCategorySummary(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Category summary retrieved successfully", summary));
    }
    
    @DeleteMapping("/bulk")
    @Operation(summary = "Delete multiple expenses", description = "Delete multiple expenses")
    public ResponseEntity<ApiResponse<Void>> deleteMultipleExpenses(
            @RequestBody List<Long> expenseIds,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        expenseService.deleteMultipleExpenses(expenseIds, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Expenses deleted successfully", null));
    }
    
    @PutMapping("/bulk/status")
    @Operation(summary = "Update multiple expense status", description = "Update status for multiple expenses")
    public ResponseEntity<ApiResponse<Void>> updateMultipleExpenseStatus(
            @RequestBody List<Long> expenseIds,
            @RequestParam ExpenseStatus status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        expenseService.updateMultipleExpenseStatus(expenseIds, status, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Expense statuses updated successfully", null));
    }
    
    @GetMapping("/export/csv")
    @Operation(summary = "Export to CSV", description = "Export expenses to CSV format")
    public ResponseEntity<byte[]> exportExpensesToCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        byte[] csvData = expenseService.exportExpensesToCsv(currentUser.getId(), startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "expenses.csv");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(csvData);
    }
    
    @GetMapping("/export/pdf")
    @Operation(summary = "Export to PDF", description = "Export expenses to PDF format")
    public ResponseEntity<byte[]> exportExpensesToPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        byte[] pdfData = expenseService.exportExpensesToPdf(currentUser.getId(), startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "expenses.pdf");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfData);
    }
}