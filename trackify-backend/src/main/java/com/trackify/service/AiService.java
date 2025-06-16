package com.trackify.service;

import com.trackify.entity.Expense;
import com.trackify.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AiService {

    // Category suggestion
    String suggestCategory(String description, String merchantName, BigDecimal amount);
    List<String> suggestMultipleCategories(String description, String merchantName, BigDecimal amount);
    Map<String, Double> getCategorySuggestionConfidence(String description, String merchantName, BigDecimal amount);
    
    // Expense validation and anomaly detection
    boolean isExpenseAnomalous(Expense expense, Long userId);
    List<String> detectExpenseAnomalies(Expense expense, Long userId);
    Map<String, Object> analyzeExpensePattern(Long userId, int days);
    
    // Duplicate expense detection
    List<Expense> findPotentialDuplicates(Expense expense, Long userId);
    double calculateSimilarityScore(Expense expense1, Expense expense2);
    boolean isDuplicateExpense(Expense expense, Long userId, double threshold);
    
    // Receipt analysis
    Map<String, Object> analyzeReceipt(byte[] receiptImage);
    String extractReceiptText(byte[] receiptImage);
    Map<String, String> extractReceiptData(String receiptText);
    boolean validateReceiptData(Map<String, String> extractedData, Expense expense);
    
    // Spending insights and recommendations
    Map<String, Object> generateSpendingInsights(Long userId);
    List<String> getSpendingRecommendations(Long userId);
    Map<String, BigDecimal> predictMonthlySpending(Long userId);
    List<String> identifySpendingTrends(Long userId, int months);
    
    // Budget optimization
    Map<String, BigDecimal> suggestBudgetAllocation(Long userId, BigDecimal totalBudget);
    List<String> getBudgetOptimizationTips(Long userId);
    boolean shouldAlertBudgetOverrun(Long budgetId, Long userId);
    
    // Fraud detection
    boolean isSuspiciousExpense(Expense expense, Long userId);
    List<String> detectFraudPatterns(Long userId);
    double calculateFraudRiskScore(Expense expense, Long userId);
    
    // Smart notifications
    boolean shouldNotifyUser(String notificationType, Long userId);
    String generateSmartNotificationMessage(String type, Map<String, Object> context);
    List<String> getPersonalizedNotificationPreferences(Long userId);
    
    // Expense policy compliance
    boolean isExpenseCompliant(Expense expense, Long userId);
    List<String> checkPolicyViolations(Expense expense, Long userId);
    Map<String, Object> getPolicyComplianceReport(Long userId);
    
    // Natural language processing
    String summarizeExpenses(List<Expense> expenses);
    List<String> extractKeywordsFromDescription(String description);
    String generateExpenseReport(List<Expense> expenses, String reportType);
    
    // Machine learning model management
    void trainCategoryModel(List<Expense> trainingData);
    void trainAnomalyDetectionModel(List<Expense> trainingData);
    void updateUserModel(Long userId, List<Expense> userExpenses);
    Map<String, Object> getModelPerformanceMetrics();
    
    // AI configuration and settings
    void updateAiSettings(Long userId, Map<String, Object> settings);
    Map<String, Object> getAiSettings(Long userId);
    boolean isAiFeatureEnabled(String featureName, Long userId);
    
    // Batch processing
    void processBatchExpenses(List<Expense> expenses);
    Map<String, Object> generateBatchInsights(List<Long> userIds);
    void scheduledAiAnalysis();
    
    // External AI service integration
    Map<String, Object> queryOpenAi(String prompt, Map<String, Object> context);
    String generateAiResponse(String userQuery, Long userId);
    List<String> getAiSuggestions(String context, Long userId);
}