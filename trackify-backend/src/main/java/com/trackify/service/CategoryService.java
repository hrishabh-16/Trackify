package com.trackify.service;

import com.trackify.dto.request.CategoryRequest;
import com.trackify.dto.response.CategoryResponse;
import com.trackify.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    
    // Category CRUD operations
    CategoryResponse createCategory(CategoryRequest categoryRequest, Long userId);
    CategoryResponse createTeamCategory(CategoryRequest categoryRequest, Long teamId, Long userId);
    CategoryResponse updateCategory(Long categoryId, CategoryRequest categoryRequest, Long userId);
    void deleteCategory(Long categoryId, Long userId);
    
    // Category retrieval
    CategoryResponse getCategoryById(Long categoryId, Long userId);
    List<CategoryResponse> getAllCategoriesForUser(Long userId);
    List<CategoryResponse> getPersonalCategories(Long userId);
    List<CategoryResponse> getTeamCategories(Long teamId);
    List<CategoryResponse> getAccessibleCategories(Long userId, List<Long> teamIds);
    Page<CategoryResponse> getCategoriesPaginated(Long userId, Pageable pageable);
    
    // Category search and filtering
    Page<CategoryResponse> searchCategories(String keyword, Long userId, Pageable pageable);
    List<CategoryResponse> getActiveCategories(Long userId);
    List<CategoryResponse> getInactiveCategories(Long userId);
    List<CategoryResponse> getSystemCategories();
    
    // Category management
    CategoryResponse activateCategory(Long categoryId, Long userId);
    CategoryResponse deactivateCategory(Long categoryId, Long userId);
    void reorderCategories(List<Long> categoryIds, Long userId);
    
    // Category validation and utilities
    boolean isCategoryAccessibleToUser(Long categoryId, Long userId);
    boolean categoryNameExists(String name, Long userId, Long teamId);
    void validateCategoryOwnership(Long categoryId, Long userId);
    
    // Default categories
    void createDefaultCategories(Long userId);
    
    // Statistics
    long getUserCategoryCount(Long userId);
    long getTeamCategoryCount(Long teamId);
    List<CategoryResponse> getCategoriesWithStats(Long userId);
    
    // Conversion utilities
    CategoryResponse convertToResponse(Category category);
    List<CategoryResponse> convertToResponseList(List<Category> categories);
    Category convertToEntity(CategoryRequest request, Long userId);
}