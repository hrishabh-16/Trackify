package com.trackify.controller;

import com.trackify.dto.request.CategoryRequest;
import com.trackify.dto.response.ApiResponse;
import com.trackify.dto.response.CategoryResponse;
import com.trackify.security.UserPrincipal;
import com.trackify.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category Management", description = "APIs for managing expense categories")
public class CategoryController {
    
	
	private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
	@Autowired
    private  CategoryService categoryService;
    
    @PostMapping
    @Operation(summary = "Create new category", description = "Create a new personal expense category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest categoryRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
    	logger.info("Creating category '{}' for user: {}", categoryRequest.getName(), currentUser.getId());
        
        CategoryResponse category = categoryService.createCategory(categoryRequest, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", category));
    }
    
    @PostMapping("/team/{teamId}")
    @Operation(summary = "Create team category", description = "Create a new category for a team")
    public ResponseEntity<ApiResponse<CategoryResponse>> createTeamCategory(
            @PathVariable Long teamId,
            @Valid @RequestBody CategoryRequest categoryRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
    	logger.info("Creating team category '{}' for team: {} by user: {}", 
                 categoryRequest.getName(), teamId, currentUser.getId());
        
        CategoryResponse category = categoryService.createTeamCategory(categoryRequest, teamId, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Team category created successfully", category));
    }
    
    @GetMapping
    @Operation(summary = "Get all categories", description = "Get all categories accessible to the current user")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<CategoryResponse> categories = categoryService.getAllCategoriesForUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }
    
    @GetMapping("/personal")
    @Operation(summary = "Get personal categories", description = "Get personal categories for the current user")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getPersonalCategories(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<CategoryResponse> categories = categoryService.getPersonalCategories(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Personal categories retrieved successfully", categories));
    }
    
    @GetMapping("/team/{teamId}")
    @Operation(summary = "Get team categories", description = "Get categories for a specific team")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getTeamCategories(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<CategoryResponse> categories = categoryService.getTeamCategories(teamId);
        return ResponseEntity.ok(ApiResponse.success("Team categories retrieved successfully", categories));
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active categories", description = "Get all active categories for the current user")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<CategoryResponse> categories = categoryService.getActiveCategories(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Active categories retrieved successfully", categories));
    }
    
    @GetMapping("/inactive")
    @Operation(summary = "Get inactive categories", description = "Get all inactive categories for the current user")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getInactiveCategories(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<CategoryResponse> categories = categoryService.getInactiveCategories(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Inactive categories retrieved successfully", categories));
    }
    
    @GetMapping("/system")
    @Operation(summary = "Get system categories", description = "Get all system-defined categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSystemCategories() {
        
        List<CategoryResponse> categories = categoryService.getSystemCategories();
        return ResponseEntity.ok(ApiResponse.success("System categories retrieved successfully", categories));
    }
    
    @GetMapping("/paginated")
    @Operation(summary = "Get categories paginated", description = "Get categories with pagination")
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> getCategoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CategoryResponse> categories = categoryService.getCategoriesPaginated(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search categories", description = "Search categories by name")
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> searchCategories(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<CategoryResponse> categories = categoryService.searchCategories(keyword, currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Categories found successfully", categories));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Get a specific category by its ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        CategoryResponse category = categoryService.getCategoryById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", category));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update category", description = "Update an existing category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @Valid @RequestBody CategoryRequest categoryRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Updating category {} by user: {}", id, currentUser.getId());
        
        CategoryResponse category = categoryService.updateCategory(id, categoryRequest, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", category));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category", description = "Delete a category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Deleting category {} by user: {}", id, currentUser.getId());
        
        categoryService.deleteCategory(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
    
    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate category", description = "Activate a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> activateCategory(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        CategoryResponse category = categoryService.activateCategory(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Category activated successfully", category));
    }
    
    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate category", description = "Deactivate a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> deactivateCategory(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        CategoryResponse category = categoryService.deactivateCategory(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Category deactivated successfully", category));
    }
    
    @PutMapping("/reorder")
    @Operation(summary = "Reorder categories", description = "Reorder categories by providing sorted category IDs")
    public ResponseEntity<ApiResponse<Void>> reorderCategories(
            @RequestBody List<Long> categoryIds,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Reordering categories for user: {}", currentUser.getId());
        
        categoryService.reorderCategories(categoryIds, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Categories reordered successfully", null));
    }
    
    @PostMapping("/create-defaults")
    @Operation(summary = "Create default categories", description = "Create default categories for the current user")
    public ResponseEntity<ApiResponse<Void>> createDefaultCategories(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Creating default categories for user: {}", currentUser.getId());
        
        categoryService.createDefaultCategories(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Default categories created successfully", null));
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get category statistics", description = "Get statistics about user's categories")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoryStatistics(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        long totalCategories = categoryService.getUserCategoryCount(currentUser.getId());
        List<CategoryResponse> activeCategories = categoryService.getActiveCategories(currentUser.getId());
        List<CategoryResponse> inactiveCategories = categoryService.getInactiveCategories(currentUser.getId());
        
        Map<String, Object> stats = Map.of(
            "totalCategories", totalCategories,
            "activeCategories", activeCategories.size(),
            "inactiveCategories", inactiveCategories.size(),
            "systemCategories", categoryService.getSystemCategories().size()
        );
        
        return ResponseEntity.ok(ApiResponse.success("Category statistics retrieved successfully", stats));
    }
    
    @GetMapping("/with-stats")
    @Operation(summary = "Get categories with statistics", description = "Get categories with expense statistics")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoriesWithStats(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<CategoryResponse> categories = categoryService.getCategoriesWithStats(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Categories with statistics retrieved successfully", categories));
    }
    
    @GetMapping("/{id}/validate-access")
    @Operation(summary = "Validate category access", description = "Check if user has access to a category")
    public ResponseEntity<ApiResponse<Boolean>> validateCategoryAccess(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        boolean hasAccess = categoryService.isCategoryAccessibleToUser(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Category access validated", hasAccess));
    }
 }