package com.trackify.repository;

import com.trackify.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // Find by name
    Optional<Category> findByNameIgnoreCase(String name);
    
    // FIXED: Changed from userId to createdBy (matches Category entity field)
    Optional<Category> findByNameAndCreatedBy(String name, Long createdBy);
    
    // FIXED: Changed from UserIdIsNull to CreatedByIsNull (matches Category entity field)
    Optional<Category> findByNameAndCreatedByIsNull(String name);
    
    // FIXED: Changed from UserId to CreatedBy (matches Category entity field)
    List<Category> findByCreatedByOrCreatedByIsNull(Long createdBy);
    
    // Find by active status
    List<Category> findByIsActiveTrue();
    List<Category> findByIsActiveFalse();
    Page<Category> findByIsActive(Boolean isActive, Pageable pageable);
    
    // Find by user (personal categories) - already correct
    List<Category> findByCreatedByAndTeamIdIsNullOrderBySortOrderAsc(Long userId);
    Page<Category> findByCreatedByAndTeamIdIsNull(Long userId, Pageable pageable);
    
    // Find by team
    List<Category> findByTeamIdOrderBySortOrderAsc(Long teamId);
    Page<Category> findByTeamId(Long teamId, Pageable pageable);
    
    // Find by user or team (all accessible categories for a user)
    @Query("SELECT c FROM Category c WHERE (c.createdBy = :userId AND c.teamId IS NULL) OR c.teamId IN :teamIds ORDER BY c.sortOrder ASC")
    List<Category> findAccessibleCategories(@Param("userId") Long userId, @Param("teamIds") List<Long> teamIds);
    
    // Find system categories
    List<Category> findByIsSystemTrueOrderBySortOrderAsc();
    
    // Search categories by name
    @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword% AND c.isActive = true")
    Page<Category> searchByName(@Param("keyword") String keyword, Pageable pageable);
    
    // Check if category name exists for user
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND c.createdBy = :userId AND c.teamId IS NULL")
    boolean existsByNameAndUserId(@Param("name") String name, @Param("userId") Long userId);
    
    // Check if category name exists for team
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND c.teamId = :teamId")
    boolean existsByNameAndTeamId(@Param("name") String name, @Param("teamId") Long teamId);
    
    // Update active status
    @Modifying
    @Query("UPDATE Category c SET c.isActive = :isActive WHERE c.id = :categoryId")
    void updateActiveStatus(@Param("categoryId") Long categoryId, @Param("isActive") Boolean isActive);
    
    // Update sort order
    @Modifying
    @Query("UPDATE Category c SET c.sortOrder = :sortOrder WHERE c.id = :categoryId")
    void updateSortOrder(@Param("categoryId") Long categoryId, @Param("sortOrder") Integer sortOrder);
    
    // Count categories by user
    long countByCreatedByAndTeamIdIsNull(Long userId);
    
    // Count categories by team
    long countByTeamId(Long teamId);
    
    // Find categories with expense count
    @Query("SELECT c, COUNT(e.id) as expenseCount FROM Category c LEFT JOIN Expense e ON e.categoryId = c.id WHERE c.createdBy = :userId GROUP BY c.id ORDER BY c.sortOrder ASC")
    List<Object[]> findCategoriesWithExpenseCount(@Param("userId") Long userId);
    
    // Get max sort order
    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Category c WHERE c.createdBy = :userId AND c.teamId IS NULL")
    Integer getMaxSortOrderForUser(@Param("userId") Long userId);
    
    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Category c WHERE c.teamId = :teamId")
    Integer getMaxSortOrderForTeam(@Param("teamId") Long teamId);
    
    // ADDED: Additional helper methods that might be useful
    
    // Find all categories for a user (personal + system categories)
    @Query("SELECT c FROM Category c WHERE c.createdBy = :userId OR c.createdBy IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findUserAccessibleCategories(@Param("userId") Long userId);
    
    // Find system categories only
    List<Category> findByCreatedByIsNullAndIsSystemTrue();
    
    // Find personal categories for user
    List<Category> findByCreatedByAndTeamIdIsNull(Long createdBy);
    
    // Find categories by name (for any user or system)
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    List<Category> findByNameIgnoreCaseAll(@Param("name") String name);
}