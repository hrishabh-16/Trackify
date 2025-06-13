package com.trackify.service.impl;
import com.trackify.dto.request.CategoryRequest;
import com.trackify.dto.response.CategoryResponse;
import com.trackify.entity.Category;
import com.trackify.exception.BadRequestException;
import com.trackify.exception.ForbiddenException;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.repository.CategoryRepository;
import com.trackify.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryServiceImpl implements CategoryService {
	
	private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);
	
    @Autowired
    private  CategoryRepository categoryRepository;
    
    @Override
    public CategoryResponse createCategory(CategoryRequest categoryRequest, Long userId) {
    	logger.info("Creating personal category '{}' for user: {}", categoryRequest.getName(), userId);
        
        // Validate category name uniqueness for user
        if (categoryNameExists(categoryRequest.getName(), userId, null)) {
            throw new BadRequestException("Category with name '" + categoryRequest.getName() + "' already exists");
        }
        
        // Create category entity
        Category category = convertToEntity(categoryRequest, userId);
        category.setTeamId(null); // Ensure it's a personal category
        category.setSortOrder(getNextSortOrder(userId, null));
        
        Category savedCategory = categoryRepository.save(category);
        
        logger.info("Personal category created successfully with id: {}", savedCategory.getId());
        return convertToResponse(savedCategory);
    }
    
    @Override
    public CategoryResponse createTeamCategory(CategoryRequest categoryRequest, Long teamId, Long userId) {
    	logger.info("Creating team category '{}' for team: {} by user: {}", 
                 categoryRequest.getName(), teamId, userId);
        
        // Validate category name uniqueness for team
        if (categoryNameExists(categoryRequest.getName(), null, teamId)) {
            throw new BadRequestException("Category with name '" + categoryRequest.getName() + "' already exists in this team");
        }
        
        // Create category entity
        Category category = convertToEntity(categoryRequest, userId);
        category.setTeamId(teamId);
        category.setSortOrder(getNextSortOrder(null, teamId));
        
        Category savedCategory = categoryRepository.save(category);
        
        logger.info("Team category created successfully with id: {}", savedCategory.getId());
        return convertToResponse(savedCategory);
    }
    
    @Override
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest categoryRequest, Long userId) {
    	logger.info("Updating category {} by user: {}", categoryId, userId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        
        // Validate ownership
        validateCategoryOwnership(categoryId, userId);
        
        // Check if system category
        if (category.getIsSystem()) {
            throw new ForbiddenException("System categories cannot be modified");
        }
        
        // Validate name uniqueness if name is being changed
        if (!category.getName().equals(categoryRequest.getName())) {
            if (categoryNameExists(categoryRequest.getName(), userId, category.getTeamId())) {
                throw new BadRequestException("Category with name '" + categoryRequest.getName() + "' already exists");
            }
        }
        
        // Update category fields
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        category.setColor(categoryRequest.getColor());
        category.setIcon(categoryRequest.getIcon());
        category.setIsActive(categoryRequest.getIsActive());
        
        Category updatedCategory = categoryRepository.save(category);
        
        logger.info("Category updated successfully: {}", categoryId);
        return convertToResponse(updatedCategory);
    }
    
    @Override
    public void deleteCategory(Long categoryId, Long userId) {
    	logger.info("Deleting category {} by user: {}", categoryId, userId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        
        // Validate ownership
        validateCategoryOwnership(categoryId, userId);
        
        // Check if system category
        if (category.getIsSystem()) {
            throw new ForbiddenException("System categories cannot be deleted");
        }
        
        categoryRepository.deleteById(categoryId);
        
        logger.info("Category deleted successfully: {}", categoryId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        
        // Validate access
        if (!isCategoryAccessibleToUser(categoryId, userId)) {
            throw new ForbiddenException("You don't have access to this category");
        }
        
        return convertToResponse(category);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesForUser(Long userId) {
        List<Category> personalCategories = categoryRepository.findByCreatedByAndTeamIdIsNullOrderBySortOrderAsc(userId);
        return convertToResponseList(personalCategories);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getPersonalCategories(Long userId) {
        List<Category> categories = categoryRepository.findByCreatedByAndTeamIdIsNullOrderBySortOrderAsc(userId);
        return convertToResponseList(categories);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getTeamCategories(Long teamId) {
        List<Category> categories = categoryRepository.findByTeamIdOrderBySortOrderAsc(teamId);
        return convertToResponseList(categories);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAccessibleCategories(Long userId, List<Long> teamIds) {
        List<Category> categories = categoryRepository.findAccessibleCategories(userId, teamIds);
        return convertToResponseList(categories);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategoriesPaginated(Long userId, Pageable pageable) {
        Page<Category> categories = categoryRepository.findByCreatedByAndTeamIdIsNull(userId, pageable);
        return categories.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> searchCategories(String keyword, Long userId, Pageable pageable) {
        Page<Category> categories = categoryRepository.searchByName(keyword, pageable);
        return categories.map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories(Long userId) {
        List<Category> categories = categoryRepository.findByCreatedByAndTeamIdIsNullOrderBySortOrderAsc(userId)
                .stream()
                .filter(Category::getIsActive)
                .collect(Collectors.toList());
        return convertToResponseList(categories);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getInactiveCategories(Long userId) {
        List<Category> categories = categoryRepository.findByCreatedByAndTeamIdIsNullOrderBySortOrderAsc(userId)
                .stream()
                .filter(category -> !category.getIsActive())
                .collect(Collectors.toList());
        return convertToResponseList(categories);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSystemCategories() {
        List<Category> categories = categoryRepository.findByIsSystemTrueOrderBySortOrderAsc();
        return convertToResponseList(categories);
    }
    
    @Override
    public CategoryResponse activateCategory(Long categoryId, Long userId) {
    	logger.info("Activating category {} by user: {}", categoryId, userId);
        
        validateCategoryOwnership(categoryId, userId);
        categoryRepository.updateActiveStatus(categoryId, true);
        
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        return convertToResponse(category);
    }
    
    @Override
    public CategoryResponse deactivateCategory(Long categoryId, Long userId) {
    	logger.info("Deactivating category {} by user: {}", categoryId, userId);
        
        validateCategoryOwnership(categoryId, userId);
        categoryRepository.updateActiveStatus(categoryId, false);
        
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        return convertToResponse(category);
    }
    
    @Override
    public void reorderCategories(List<Long> categoryIds, Long userId) {
    	logger.info("Reordering categories for user: {}", userId);
        
        for (int i = 0; i < categoryIds.size(); i++) {
            Long categoryId = categoryIds.get(i);
            validateCategoryOwnership(categoryId, userId);
            categoryRepository.updateSortOrder(categoryId, i);
        }
        
        logger.info("Categories reordered successfully for user: {}", userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isCategoryAccessibleToUser(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return false;
        }
        
        return category.getCreatedBy().equals(userId) || category.getIsSystem();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean categoryNameExists(String name, Long userId, Long teamId) {
        if (teamId != null) {
            return categoryRepository.existsByNameAndTeamId(name, teamId);
        } else {
            return categoryRepository.existsByNameAndUserId(name, userId);
        }
    }
    
    @Override
    public void validateCategoryOwnership(Long categoryId, Long userId) {
        if (!isCategoryAccessibleToUser(categoryId, userId)) {
            throw new ForbiddenException("You don't have access to this category");
        }
    }
    
    @Override
    public void createDefaultCategories(Long userId) {
    	logger.info("Creating default categories for user: {}", userId);
        
        String[][] defaultCategories = {
            {"Food & Dining", "Restaurant meals, groceries, takeout", "#FF6B6B", "🍽️"},
            {"Transportation", "Gas, public transport, ride-sharing", "#4ECDC4", "🚗"},
            {"Shopping", "Clothing, electronics, general purchases", "#45B7D1", "🛍️"},
            {"Entertainment", "Movies, games, subscriptions", "#96CEB4", "🎬"},
            {"Health & Medical", "Doctor visits, pharmacy, fitness", "#FFEAA7", "🏥"},
            {"Bills & Utilities", "Electricity, water, internet, phone", "#DDA0DD", "💡"},
            {"Travel", "Flights, hotels, vacation expenses", "#98D8C8", "✈️"},
            {"Education", "Books, courses, school fees", "#F7DC6F", "📚"},
            {"Personal Care", "Haircuts, cosmetics, spa", "#BB8FCE", "💄"},
            {"Other", "Miscellaneous expenses", "#AED6F1", "📦"}
        };
        
        for (int i = 0; i < defaultCategories.length; i++) {
            String[] categoryData = defaultCategories[i];
            
            if (!categoryNameExists(categoryData[0], userId, null)) {
                Category category = new Category();
                category.setName(categoryData[0]);
                category.setDescription(categoryData[1]);
                category.setColor(categoryData[2]);
                category.setIcon(categoryData[3]);
                category.setIsActive(true);
                category.setIsSystem(false);
                category.setCreatedBy(userId);
                category.setTeamId(null);
                category.setSortOrder(i);
                
                categoryRepository.save(category);
            }
        }
        
        logger.info("Default categories created for user: {}", userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getUserCategoryCount(Long userId) {
        return categoryRepository.countByCreatedByAndTeamIdIsNull(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getTeamCategoryCount(Long teamId) {
        return categoryRepository.countByTeamId(teamId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesWithStats(Long userId) {
        return getAllCategoriesForUser(userId);
    }
    
    @Override
    public CategoryResponse convertToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setColor(category.getColor());
        response.setIcon(category.getIcon());
        response.setIsActive(category.getIsActive());
        response.setIsSystem(category.getIsSystem());
        response.setCreatedBy(category.getCreatedBy());
        response.setTeamId(category.getTeamId());
        response.setSortOrder(category.getSortOrder());
        response.setIsPersonalCategory(category.isPersonalCategory());
        response.setIsTeamCategory(category.isTeamCategory());
        response.setCanBeDeleted(category.canBeDeleted());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }
    
    @Override
    public List<CategoryResponse> convertToResponseList(List<Category> categories) {
        return categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Category convertToEntity(CategoryRequest request, Long userId) {
        Category category = new Category();
        category.setName(request.getName().trim());
        category.setDescription(StringUtils.hasText(request.getDescription()) ? 
                               request.getDescription().trim() : null);
        category.setColor(StringUtils.hasText(request.getColor()) ? 
                         request.getColor() : "#6C757D");
        category.setIcon(StringUtils.hasText(request.getIcon()) ? 
                        request.getIcon() : "📁");
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        category.setIsSystem(false);
        category.setCreatedBy(userId);
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        return category;
    }
    
    private Integer getNextSortOrder(Long userId, Long teamId) {
        if (teamId != null) {
            return categoryRepository.getMaxSortOrderForTeam(teamId) + 1;
        } else {
            return categoryRepository.getMaxSortOrderForUser(userId) + 1;
        }
    }
}