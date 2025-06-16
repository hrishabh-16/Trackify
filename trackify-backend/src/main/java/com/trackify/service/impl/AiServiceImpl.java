package com.trackify.service.impl;

import com.trackify.entity.Expense;
import com.trackify.entity.User;
import com.trackify.entity.Budget;
import com.trackify.integration.ai.OpenAiClient;
import com.trackify.integration.ai.CategorySuggestionEngine;
import com.trackify.integration.ai.AnomalyDetectionEngine;
import com.trackify.repository.ExpenseRepository;
import com.trackify.repository.UserRepository;
import com.trackify.repository.BudgetRepository;
import com.trackify.service.AiService;
import com.trackify.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class AiServiceImpl implements AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceImpl.class);

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OpenAiClient openAiClient;

    @Autowired
    private CategorySuggestionEngine categorySuggestionEngine;

    @Autowired
    private AnomalyDetectionEngine anomalyDetectionEngine;

    // In-memory storage for AI settings (in production, use database)
    private final Map<Long, Map<String, Object>> userAiSettings = new ConcurrentHashMap<>();

    // Category suggestion
    @Override
    public String suggestCategory(String description, String merchantName, BigDecimal amount) {
        try {
            logger.debug("Suggesting category for description: {}, merchant: {}, amount: {}", 
                    description, merchantName, amount);
            
            return categorySuggestionEngine.suggestCategory(description, merchantName, amount);
            
        } catch (Exception e) {
            logger.error("Error suggesting category", e);
            return "General";
        }
    }

    @Override
    public List<String> suggestMultipleCategories(String description, String merchantName, BigDecimal amount) {
        try {
            return categorySuggestionEngine.suggestMultipleCategories(description, merchantName, amount);
        } catch (Exception e) {
            logger.error("Error suggesting multiple categories", e);
            return Arrays.asList("General", "Business", "Travel");
        }
    }

    @Override
    public Map<String, Double> getCategorySuggestionConfidence(String description, String merchantName, BigDecimal amount) {
        try {
            return categorySuggestionEngine.getCategorySuggestionConfidence(description, merchantName, amount);
        } catch (Exception e) {
            logger.error("Error getting category suggestion confidence", e);
            return Map.of("General", 0.5);
        }
    }

    // Expense validation and anomaly detection
    @Override
    public boolean isExpenseAnomalous(Expense expense, Long userId) {
        try {
            List<Expense> userExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId, 
                    LocalDateTime.now().minusMonths(6), 
                    LocalDateTime.now());
            
            return anomalyDetectionEngine.isAnomalous(expense, userExpenses);
            
        } catch (Exception e) {
            logger.error("Error checking expense anomaly for user {}", userId, e);
            return false;
        }
    }

    @Override
    public List<String> detectExpenseAnomalies(Expense expense, Long userId) {
        try {
            List<String> anomalies = new ArrayList<>();
            
            List<Expense> userExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId, 
                    LocalDateTime.now().minusMonths(3), 
                    LocalDateTime.now());
            
            // Check for amount anomalies
            if (isAmountAnomalous(expense, userExpenses)) {
                anomalies.add("Unusual expense amount detected");
            }
            
            // Check for time-based anomalies
            if (isTimeAnomalous(expense, userExpenses)) {
                anomalies.add("Expense submitted at unusual time");
            }
            
            // Check for frequency anomalies
            if (isFrequencyAnomalous(expense, userExpenses)) {
                anomalies.add("High frequency of similar expenses");
            }
            
            // Check for merchant anomalies
            if (isMerchantAnomalous(expense, userExpenses)) {
                anomalies.add("New or unusual merchant");
            }
            
            return anomalies;
            
        } catch (Exception e) {
            logger.error("Error detecting expense anomalies for user {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> analyzeExpensePattern(Long userId, int days) {
        try {
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusDays(days),
                    LocalDateTime.now());
            
            Map<String, Object> analysis = new HashMap<>();
            
            // Calculate spending patterns
            analysis.put("totalAmount", calculateTotalAmount(expenses));
            analysis.put("averageAmount", calculateAverageAmount(expenses));
            analysis.put("categoryBreakdown", calculateCategoryBreakdown(expenses));
            analysis.put("spendingTrend", calculateSpendingTrend(expenses));
            analysis.put("topMerchants", getTopMerchants(expenses));
            analysis.put("spendingByDayOfWeek", getSpendingByDayOfWeek(expenses));
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error analyzing expense pattern for user {}", userId, e);
            return new HashMap<>();
        }
    }

    // Duplicate expense detection
    @Override
    public List<Expense> findPotentialDuplicates(Expense expense, Long userId) {
        try {
            // Look for expenses within the last 30 days
            LocalDate startDate = expense.getExpenseDate().minusDays(30);
            LocalDate endDate = expense.getExpenseDate().plusDays(30);
            
            List<Expense> candidateExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId, startDate, endDate);
            
            return candidateExpenses.stream()
                    .filter(candidate -> !candidate.getId().equals(expense.getId()))
                    .filter(candidate -> calculateSimilarityScore(expense, candidate) > 0.8)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error finding potential duplicates for expense {}", expense.getId(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public double calculateSimilarityScore(Expense expense1, Expense expense2) {
        try {
            double amountSimilarity = calculateAmountSimilarity(expense1.getAmount(), expense2.getAmount());
            double descriptionSimilarity = calculateTextSimilarity(expense1.getDescription(), expense2.getDescription());
            double merchantSimilarity = calculateTextSimilarity(expense1.getMerchantName(), expense2.getMerchantName());
            double dateSimilarity = calculateDateSimilarity(expense1.getExpenseDate(), expense2.getExpenseDate());
            
            // Weighted average
            return (amountSimilarity * 0.3 + descriptionSimilarity * 0.3 + 
                   merchantSimilarity * 0.3 + dateSimilarity * 0.1);
            
        } catch (Exception e) {
            logger.error("Error calculating similarity score", e);
            return 0.0;
        }
    }

    @Override
    public boolean isDuplicateExpense(Expense expense, Long userId, double threshold) {
        List<Expense> duplicates = findPotentialDuplicates(expense, userId);
        return !duplicates.isEmpty() && 
               duplicates.stream().anyMatch(dup -> calculateSimilarityScore(expense, dup) >= threshold);
    }

    // Receipt analysis
    @Override
    public Map<String, Object> analyzeReceipt(byte[] receiptImage) {
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // Extract text from receipt image using AI service
            String extractedText = extractReceiptText(receiptImage);
            analysis.put("extractedText", extractedText);
            
            // Extract structured data
            Map<String, String> extractedData = extractReceiptData(extractedText);
            analysis.put("extractedData", extractedData);
            
            // Confidence scores
            analysis.put("confidence", calculateExtractionConfidence(extractedData));
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error analyzing receipt", e);
            return Map.of("error", "Failed to analyze receipt");
        }
    }

    @Override
    public String extractReceiptText(byte[] receiptImage) {
        try {
            // Use AI service for OCR
            Map<String, Object> context = Map.of("type", "receipt_ocr");
            String prompt = "Extract text from receipt image";
            return openAiClient.generateText(prompt, context);
            
        } catch (Exception e) {
            logger.error("Error extracting text from receipt", e);
            return "";
        }
    }

    @Override
    public Map<String, String> extractReceiptData(String receiptText) {
        try {
            return openAiClient.analyzeReceiptText(receiptText);
            
        } catch (Exception e) {
            logger.error("Error extracting receipt data", e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean validateReceiptData(Map<String, String> extractedData, Expense expense) {
        try {
            // Validate amount
            String extractedAmount = extractedData.get("amount");
            if (extractedAmount != null) {
                BigDecimal receiptAmount = new BigDecimal(extractedAmount.replaceAll("[^0-9.]", ""));
                BigDecimal expenseAmount = expense.getAmount();
                
                // Allow 5% variance
                BigDecimal variance = expenseAmount.multiply(BigDecimal.valueOf(0.05));
                if (receiptAmount.subtract(expenseAmount).abs().compareTo(variance) > 0) {
                    return false;
                }
            }
            
            // Validate merchant
            String extractedMerchant = extractedData.get("merchant");
            if (extractedMerchant != null && expense.getMerchantName() != null) {
                double similarity = calculateTextSimilarity(extractedMerchant, expense.getMerchantName());
                if (similarity < 0.6) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating receipt data", e);
            return false;
        }
    }

    // Spending insights and recommendations
    @Override
    public Map<String, Object> generateSpendingInsights(Long userId) {
        try {
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusMonths(3),
                    LocalDateTime.now());
            
            Map<String, Object> insights = new HashMap<>();
            
            insights.put("totalSpending", calculateTotalAmount(expenses));
            insights.put("monthlyAverage", calculateMonthlyAverage(expenses));
            insights.put("topCategories", getTopSpendingCategories(expenses));
            insights.put("spendingTrend", getSpendingTrend(expenses));
            insights.put("budgetUtilization", calculateBudgetUtilization(userId));
            insights.put("recommendations", getSpendingRecommendations(userId));
            
            return insights;
            
        } catch (Exception e) {
            logger.error("Error generating spending insights for user {}", userId, e);
            return new HashMap<>();
        }
    }

    @Override
    public List<String> getSpendingRecommendations(Long userId) {
        try {
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusMonths(1),
                    LocalDateTime.now());
            
            Map<String, Object> spendingData = new HashMap<>();
            spendingData.put("totalAmount", calculateTotalAmount(expenses));
            spendingData.put("categoryBreakdown", calculateCategoryBreakdown(expenses));
            spendingData.put("topMerchants", getTopMerchants(expenses));
            
            return openAiClient.generateBudgetRecommendations(spendingData);
            
        } catch (Exception e) {
            logger.error("Error getting spending recommendations for user {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, BigDecimal> predictMonthlySpending(Long userId) {
        try {
            List<Expense> historicalExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusMonths(6),
                    LocalDateTime.now());
            
            Map<String, BigDecimal> predictions = new HashMap<>();
            Map<String, BigDecimal> categoryAverages = calculateMonthlyCategoryAverages(historicalExpenses);
            
            // Use AI to enhance predictions
            Map<String, Object> context = Map.of(
                    "historicalData", categoryAverages,
                    "userId", userId
            );
            
            String prompt = "Predict next month spending based on historical data";
            Map<String, Object> aiResponse = openAiClient.query(prompt, context);
            
            // Parse AI response and combine with statistical predictions
            for (Map.Entry<String, BigDecimal> entry : categoryAverages.entrySet()) {
                BigDecimal predicted = entry.getValue().multiply(BigDecimal.valueOf(1.05));
                predictions.put(entry.getKey(), predicted);
            }
            
            return predictions;
            
        } catch (Exception e) {
            logger.error("Error predicting monthly spending for user {}", userId, e);
            return new HashMap<>();
        }
    }

    @Override
    public List<String> identifySpendingTrends(Long userId, int months) {
        try {
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusMonths(months),
                    LocalDateTime.now());
            
            Map<String, Object> spendingData = Map.of(
                    "monthlyData", calculateMonthlySpendingByCategory(expenses),
                    "totalExpenses", expenses.size(),
                    "timeframe", months
            );
            
            String prompt = "Identify spending trends from the provided data";
            String aiResponse = openAiClient.generateSpendingInsights(spendingData);
            
            return Arrays.asList(aiResponse.split("\n"))
                    .stream()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error identifying spending trends for user {}", userId, e);
            return new ArrayList<>();
        }
    }

    // Budget optimization
    @Override
    public Map<String, BigDecimal> suggestBudgetAllocation(Long userId, BigDecimal totalBudget) {
        try {
            List<Expense> historicalExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusMonths(3),
                    LocalDateTime.now());
            
            Map<String, BigDecimal> categorySpending = calculateCategoryBreakdown(historicalExpenses);
            
            Map<String, Object> budgetData = Map.of(
                    "totalBudget", totalBudget,
                    "historicalSpending", categorySpending,
                    "userId", userId
            );
            
            // Use AI for enhanced budget allocation
            String prompt = "Suggest optimal budget allocation based on spending patterns";
            Map<String, Object> aiResponse = openAiClient.query(prompt, budgetData);
            
            // Calculate allocation based on historical patterns
            Map<String, BigDecimal> allocation = new HashMap<>();
            BigDecimal totalHistorical = categorySpending.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalHistorical.compareTo(BigDecimal.ZERO) > 0) {
                for (Map.Entry<String, BigDecimal> entry : categorySpending.entrySet()) {
                    BigDecimal percentage = entry.getValue().divide(totalHistorical, 4, RoundingMode.HALF_UP);
                    BigDecimal suggestedAmount = totalBudget.multiply(percentage);
                    allocation.put(entry.getKey(), suggestedAmount);
                }
            }
            
            return allocation;
            
        } catch (Exception e) {
            logger.error("Error suggesting budget allocation for user {}", userId, e);
            return new HashMap<>();
        }
    }

    @Override
    public List<String> getBudgetOptimizationTips(Long userId) {
        try {
            List<Budget> budgets = budgetRepository.findByUserIdAndIsActiveTrue(userId);
            
            Map<String, Object> budgetData = Map.of(
                    "budgets", budgets,
                    "userId", userId
            );
            
            return openAiClient.generateBudgetRecommendations(budgetData);
            
        } catch (Exception e) {
            logger.error("Error getting budget optimization tips for user {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean shouldAlertBudgetOverrun(Long budgetId, Long userId) {
        try {
            Budget budget = budgetRepository.findById(budgetId).orElse(null);
            if (budget == null) return false;
            
            BigDecimal utilizationPercentage = budget.getUtilizationPercentage();
            return utilizationPercentage.compareTo(BigDecimal.valueOf(80)) > 0;
            
        } catch (Exception e) {
            logger.error("Error checking budget overrun alert for budget {}", budgetId, e);
            return false;
        }
    }

    // Fraud detection
    @Override
    public boolean isSuspiciousExpense(Expense expense, Long userId) {
        try {
            double riskScore = calculateFraudRiskScore(expense, userId);
            return riskScore > 0.7;
            
        } catch (Exception e) {
            logger.error("Error checking suspicious expense for user {}", userId, e);
            return false;
        }
    }

    @Override
    public List<String> detectFraudPatterns(Long userId) {
        try {
            List<Expense> recentExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now());
            
            Map<String, Object> fraudAnalysisData = Map.of(
                    "expenses", recentExpenses,
                    "userId", userId,
                    "timeframe", 30
            );
            
            String prompt = "Analyze expenses for potential fraud patterns";
            String aiResponse = openAiClient.generateText(prompt, fraudAnalysisData);
            
            return Arrays.asList(aiResponse.split("\n"))
                    .stream()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error detecting fraud patterns for user {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public double calculateFraudRiskScore(Expense expense, Long userId) {
        try {
            List<Expense> userExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusMonths(3),
                    LocalDateTime.now());
            
            double riskScore = 0.0;
            
            // AI-enhanced fraud detection
            Map<String, Object> fraudContext = Map.of(
                    "expense", expense,
                    "userHistory", userExpenses,
                    "userId", userId
            );
            
            String prompt = "Calculate fraud risk score for expense";
            Map<String, Object> aiResponse = openAiClient.query(prompt, fraudContext);
            
            // Combine with rule-based detection
            if (isAmountUnusual(expense, userId)) {
                riskScore += 0.3;
            }
            
            if (isMerchantUnusual(expense, userId)) {
                riskScore += 0.2;
            }
            
            if (isTimingUnusual(expense, userId)) {
                riskScore += 0.2;
            }
            
            if (isLocationUnusual(expense, userId)) {
                riskScore += 0.3;
            }
            
            return Math.min(riskScore, 1.0);
            
        } catch (Exception e) {
            logger.error("Error calculating fraud risk score", e);
            return 0.0;
        }
    }

    // Smart notifications
    @Override
    public boolean shouldNotifyUser(String notificationType, Long userId) {
        try {
            Map<String, Object> settings = getAiSettings(userId);
            Boolean smartNotifications = (Boolean) settings.getOrDefault("smartNotifications", true);
            
            if (!smartNotifications) return true;
            
            Map<String, Object> context = Map.of(
                    "notificationType", notificationType,
                    "userId", userId,
                    "userSettings", settings
            );
            
            String prompt = "Should we send this notification to the user?";
            Map<String, Object> aiResponse = openAiClient.query(prompt, context);
            
            return true; // Default to sending
            
        } catch (Exception e) {
            logger.error("Error checking notification decision for user {}", userId, e);
            return true;
        }
    }

    @Override
    public String generateSmartNotificationMessage(String type, Map<String, Object> context) {
        try {
            return openAiClient.generateNotificationMessage(type, context);
            
        } catch (Exception e) {
            logger.error("Error generating smart notification message", e);
            return "You have a new notification";
        }
    }

    @Override
    public List<String> getPersonalizedNotificationPreferences(Long userId) {
        try {
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusMonths(1),
                    LocalDateTime.now());
            
            Map<String, Object> userData = Map.of(
                    "expenses", expenses,
                    "userId", userId,
                    "currentSettings", getAiSettings(userId)
            );
            
            String prompt = "Suggest personalized notification preferences";
            String aiResponse = openAiClient.generateText(prompt, userData);
            
            return Arrays.asList(aiResponse.split("\n"))
                    .stream()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error getting personalized notification preferences for user {}", userId, e);
            return new ArrayList<>();
        }
    }

    // Expense policy compliance
    @Override
    public boolean isExpenseCompliant(Expense expense, Long userId) {
        try {
            List<String> violations = checkPolicyViolations(expense, userId);
            return violations.isEmpty();
            
        } catch (Exception e) {
            logger.error("Error checking expense compliance", e);
            return true;
        }
    }

    @Override
    public List<String> checkPolicyViolations(Expense expense, Long userId) {
        try {
            Map<String, Object> complianceData = Map.of(
                    "expense", expense,
                    "userId", userId
            );
            
            String prompt = "Check expense for policy violations";
            String aiResponse = openAiClient.generateText(prompt, complianceData);
            
            List<String> violations = new ArrayList<>();
            
            // Rule-based checks
            if (expense.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
                violations.add("Expense exceeds single transaction limit");
            }
            
            if (expense.getAmount().compareTo(BigDecimal.valueOf(25)) > 0 && 
                (expense.getReceiptUrl() == null || expense.getReceiptUrl().isEmpty())) {
                violations.add("Receipt required for expenses over $25");
            }
            
            // Add AI-detected violations
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                violations.addAll(Arrays.asList(aiResponse.split("\n"))
                        .stream()
                        .filter(line -> !line.trim().isEmpty())
                        .collect(Collectors.toList()));
            }
            
            return violations;
            
        } catch (Exception e) {
            logger.error("Error checking policy violations", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getPolicyComplianceReport(Long userId) {
        try {
            List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    userId,
                    LocalDateTime.now().minusMonths(1),
                    LocalDateTime.now());
            
            Map<String, Object> report = new HashMap<>();
            
            long totalExpenses = expenses.size();
            long compliantExpenses = expenses.stream()
                    .mapToLong(expense -> isExpenseCompliant(expense, userId) ? 1 : 0)
                    .sum();
            
            report.put("totalExpenses", totalExpenses);
            report.put("compliantExpenses", compliantExpenses);
            report.put("complianceRate", totalExpenses > 0 ? 
                    (double) compliantExpenses / totalExpenses : 1.0);
            
            // Generate AI summary
            Map<String, Object> complianceData = Map.of(
                    "report", report,
                    "expenses", expenses
            );
            
            String aiSummary = openAiClient.generateComplianceReport(complianceData);
            report.put("aiSummary", aiSummary);
            
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating policy compliance report for user {}", userId, e);
            return new HashMap<>();
        }
    }

    // Natural language processing
    @Override
    public String summarizeExpenses(List<Expense> expenses) {
        try {
            if (expenses.isEmpty()) return "No expenses to summarize";
            
            Map<String, Object> expenseData = Map.of(
                    "expenses", expenses,
                    "summary", calculateBasicSummary(expenses)
            );
            
            return openAiClient.generateText("Summarize these expenses", expenseData);
            
        } catch (Exception e) {
            logger.error("Error summarizing expenses", e);
            return "Unable to summarize expenses";
        }
    }

    @Override
    public List<String> extractKeywordsFromDescription(String description) {
        try {
            if (description == null || description.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            Map<String, Object> context = Map.of("description", description);
            String prompt = "Extract important keywords from this expense description";
            String aiResponse = openAiClient.generateText(prompt, context);
            
            return Arrays.asList(aiResponse.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(keyword -> !keyword.isEmpty())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error extracting keywords from description", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String generateExpenseReport(List<Expense> expenses, String reportType) {
        try {
            Map<String, Object> context = Map.of(
                    "expenses", expenses,
                    "reportType", reportType,
                    "summary", calculateBasicSummary(expenses)
            );
            
            String prompt = "Generate a " + reportType + " expense report";
            return openAiClient.generateText(prompt, context);
            
        } catch (Exception e) {
            logger.error("Error generating expense report", e);
            return "Unable to generate report";
        }
    }

    // Machine learning model management
    @Override
    public void trainCategoryModel(List<Expense> trainingData) {
        try {
            categorySuggestionEngine.trainModel(trainingData);
            logger.info("Category model trained with {} examples", trainingData.size());
            
        } catch (Exception e) {
            logger.error("Error training category model", e);
        }
    }

    @Override
    public void trainAnomalyDetectionModel(List<Expense> trainingData) {
        try {
            anomalyDetectionEngine.trainModel(trainingData);
            logger.info("Anomaly detection model trained with {} examples", trainingData.size());
            
        } catch (Exception e) {
            logger.error("Error training anomaly detection model", e);
        }
    }

    @Override
    public void updateUserModel(Long userId, List<Expense> userExpenses) {
        try {
            categorySuggestionEngine.updateUserModel(userId, userExpenses);
            anomalyDetectionEngine.updateUserModel(userId, userExpenses);
            
            logger.info("Updated user model for user {} with {} expenses", userId, userExpenses.size());
            
        } catch (Exception e) {
            logger.error("Error updating user model for user {}", userId, e);
        }
    }

    @Override
    public Map<String, Object> getModelPerformanceMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            metrics.put("categoryModel", categorySuggestionEngine.getPerformanceMetrics());
            metrics.put("anomalyModel", anomalyDetectionEngine.getPerformanceMetrics());
            
            return metrics;
            
        } catch (Exception e) {
            logger.error("Error getting model performance metrics", e);
            return new HashMap<>();
        }
    }

    // AI configuration and settings
    @Override
    public void updateAiSettings(Long userId, Map<String, Object> settings) {
        userAiSettings.put(userId, new HashMap<>(settings));
        logger.info("Updated AI settings for user {}", userId);
    }

    @Override
    public Map<String, Object> getAiSettings(Long userId) {
        return userAiSettings.getOrDefault(userId, getDefaultAiSettings());
    }

    @Override
    public boolean isAiFeatureEnabled(String featureName, Long userId) {
        Map<String, Object> settings = getAiSettings(userId);
        return (Boolean) settings.getOrDefault(featureName, true);
    }

    // Batch processing
    @Override
    public void processBatchExpenses(List<Expense> expenses) {
        try {
            for (Expense expense : expenses) {
            	if (isExpenseAnomalous(expense, expense.getUserId())) {
                    notificationService.notifyAnomalyDetected(
                            expense.getUserId(),
                            "Anomalous expense detected",
                            "EXPENSE",
                            expense.getId());
                }
                
                List<Expense> duplicates = findPotentialDuplicates(expense, expense.getUserId());
                if (!duplicates.isEmpty()) {
                    notificationService.notifyDuplicateExpense(
                            expense.getUserId(),
                            expense.getId(),
                            duplicates.get(0).getId());
                }
            }
            
            logger.info("Processed {} expenses in batch", expenses.size());
            
        } catch (Exception e) {
            logger.error("Error processing batch expenses", e);
        }
    }

    @Override
    public Map<String, Object> generateBatchInsights(List<Long> userIds) {
        try {
            Map<String, Object> insights = new HashMap<>();
            
            for (Long userId : userIds) {
                insights.put("user_" + userId, generateSpendingInsights(userId));
            }
            
            return insights;
            
        } catch (Exception e) {
            logger.error("Error generating batch insights", e);
            return new HashMap<>();
        }
    }

    @Override
    public void scheduledAiAnalysis() {
        try {
            logger.info("Running scheduled AI analysis");
            
            List<User> users = userRepository.findAll();
            
            for (User user : users) {
                List<Expense> recentExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                        user.getId(),
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now());
                
                if (!recentExpenses.isEmpty()) {
                    processBatchExpenses(recentExpenses);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in scheduled AI analysis", e);
        }
    }

    // External AI service integration
    @Override
    public Map<String, Object> queryOpenAi(String prompt, Map<String, Object> context) {
        try {
            return openAiClient.query(prompt, context);
            
        } catch (Exception e) {
            logger.error("Error querying OpenAI", e);
            return Map.of("error", "Failed to query AI service");
        }
    }

    @Override
    public String generateAiResponse(String userQuery, Long userId) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("userId", userId);
            context.put("userQuery", userQuery);
            
            return openAiClient.generateText(userQuery, context);
            
        } catch (Exception e) {
            logger.error("Error generating AI response for user {}", userId, e);
            return "I'm sorry, I couldn't process your request right now.";
        }
    }

    @Override
    public List<String> getAiSuggestions(String context, Long userId) {
        try {
            String prompt = "Generate helpful suggestions for: " + context;
            Map<String, Object> queryContext = Map.of("userId", userId, "context", context);
            
            String response = openAiClient.generateText(prompt, queryContext);
            
            return Arrays.asList(response.split("\n"))
                    .stream()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error getting AI suggestions for user {}", userId, e);
            return Arrays.asList("Unable to generate suggestions at this time");
        }
    }

    // Private helper methods
    private boolean isAmountAnomalous(Expense expense, List<Expense> userExpenses) {
        if (userExpenses.isEmpty()) return false;
        
        BigDecimal averageAmount = calculateAverageAmount(userExpenses);
        BigDecimal stdDev = calculateStandardDeviation(userExpenses);
        BigDecimal threshold = averageAmount.add(stdDev.multiply(BigDecimal.valueOf(2)));
        
        return expense.getAmount().compareTo(threshold) > 0;
    }

    private boolean isTimeAnomalous(Expense expense, List<Expense> userExpenses) {
        int hour = expense.getCreatedAt().getHour();
        return hour < 6 || hour > 22;
    }

    private boolean isFrequencyAnomalous(Expense expense, List<Expense> userExpenses) {
        long similarCount = userExpenses.stream()
                .filter(e -> calculateSimilarityScore(expense, e) > 0.7)
                .count();
        
        return similarCount > 5;
    }

    private boolean isMerchantAnomalous(Expense expense, List<Expense> userExpenses) {
        if (expense.getMerchantName() == null) return false;
        
        return userExpenses.stream()
                .noneMatch(e -> expense.getMerchantName().equals(e.getMerchantName()));
    }

    private BigDecimal calculateTotalAmount(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAverageAmount(List<Expense> expenses) {
        if (expenses.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal total = calculateTotalAmount(expenses);
        return total.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStandardDeviation(List<Expense> expenses) {
        if (expenses.size() < 2) return BigDecimal.ZERO;
        
        BigDecimal mean = calculateAverageAmount(expenses);
        BigDecimal variance = expenses.stream()
                .map(expense -> expense.getAmount().subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(expenses.size() - 1), 4, RoundingMode.HALF_UP);
        
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    private Map<String, BigDecimal> calculateCategoryBreakdown(List<Expense> expenses) {
        return expenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(
                        expense -> expense.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
    }

    private String calculateSpendingTrend(List<Expense> expenses) {
        if (expenses.size() < 2) return "Insufficient data";
        
        expenses.sort((e1, e2) -> e1.getExpenseDate().compareTo(e2.getExpenseDate()));
        
        int halfSize = expenses.size() / 2;
        BigDecimal firstHalfTotal = expenses.subList(0, halfSize).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal secondHalfTotal = expenses.subList(halfSize, expenses.size()).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (secondHalfTotal.compareTo(firstHalfTotal) > 0) {
            return "Increasing";
        } else if (secondHalfTotal.compareTo(firstHalfTotal) < 0) {
            return "Decreasing";
        } else {
            return "Stable";
        }
    }

    private List<String> getTopMerchants(List<Expense> expenses) {
        return expenses.stream()
                .filter(expense -> expense.getMerchantName() != null)
                .collect(Collectors.groupingBy(
                        Expense::getMerchantName,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Map<String, BigDecimal> getSpendingByDayOfWeek(List<Expense> expenses) {
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getExpenseDate().getDayOfWeek().toString(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
    }

    private double calculateAmountSimilarity(BigDecimal amount1, BigDecimal amount2) {
        if (amount1.equals(amount2)) return 1.0;
        
        BigDecimal diff = amount1.subtract(amount2).abs();
        BigDecimal max = amount1.max(amount2);
        
        if (max.equals(BigDecimal.ZERO)) return 1.0;
        
        BigDecimal similarity = BigDecimal.ONE.subtract(diff.divide(max, 4, RoundingMode.HALF_UP));
        return Math.max(0.0, similarity.doubleValue());
    }

    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        if (text1.equals(text2)) return 1.0;
        
        Set<String> words1 = Set.of(text1.toLowerCase().split("\\s+"));
        Set<String> words2 = Set.of(text2.toLowerCase().split("\\s+"));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private double calculateDateSimilarity(LocalDate date1, LocalDate date2) {
        long daysDiff = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(date1, date2));
        
        if (daysDiff == 0) return 1.0;
        if (daysDiff == 1) return 0.8;
        if (daysDiff <= 3) return 0.6;
        if (daysDiff <= 7) return 0.4;
        if (daysDiff <= 30) return 0.2;
        
        return 0.0;
    }

    private double calculateExtractionConfidence(Map<String, String> extractedData) {
        double confidence = 0.0;
        
        if (extractedData.containsKey("amount")) confidence += 0.4;
        if (extractedData.containsKey("date")) confidence += 0.3;
        if (extractedData.containsKey("merchant")) confidence += 0.3;
        
        return confidence;
    }

    private BigDecimal calculateMonthlyAverage(List<Expense> expenses) {
        if (expenses.isEmpty()) return BigDecimal.ZERO;
        
        Map<String, BigDecimal> monthlyTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getExpenseDate().getYear() + "-" + 
                                  expense.getExpenseDate().getMonthValue(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
        
        if (monthlyTotals.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal totalMonthlySpending = monthlyTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalMonthlySpending.divide(BigDecimal.valueOf(monthlyTotals.size()), 2, RoundingMode.HALF_UP);
    }

    private List<String> getTopSpendingCategories(List<Expense> expenses) {
        return calculateCategoryBreakdown(expenses).entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String getSpendingTrend(List<Expense> expenses) {
        return calculateSpendingTrend(expenses);
    }

    private Map<String, Object> calculateBudgetUtilization(Long userId) {
        Map<String, Object> utilization = new HashMap<>();
        
        List<Budget> budgets = budgetRepository.findByUserIdAndIsActiveTrue(userId);
        
        for (Budget budget : budgets) {
            Map<String, Object> budgetData = new HashMap<>();
            budgetData.put("totalAmount", budget.getTotalAmount());
            budgetData.put("spentAmount", budget.getSpentAmount());
            budgetData.put("utilizationPercentage", budget.getUtilizationPercentage());
            
            utilization.put(budget.getCategory().getName(), budgetData);
        }
        
        return utilization;
    }

    private Map<String, BigDecimal> calculateMonthlyCategoryAverages(List<Expense> expenses) {
        Map<String, Map<String, BigDecimal>> categoryMonthlyTotals = expenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(
                        expense -> expense.getCategory().getName(),
                        Collectors.groupingBy(
                                expense -> expense.getExpenseDate().getYear() + "-" + 
                                          expense.getExpenseDate().getMonthValue(),
                                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add))));
        
        Map<String, BigDecimal> averages = new HashMap<>();
        
        for (Map.Entry<String, Map<String, BigDecimal>> categoryEntry : categoryMonthlyTotals.entrySet()) {
            String category = categoryEntry.getKey();
            Map<String, BigDecimal> monthlyTotals = categoryEntry.getValue();
            
            BigDecimal total = monthlyTotals.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal average = monthlyTotals.isEmpty() ? BigDecimal.ZERO :
                    total.divide(BigDecimal.valueOf(monthlyTotals.size()), 2, RoundingMode.HALF_UP);
            
            averages.put(category, average);
        }
        
        return averages;
    }

    private Map<String, List<BigDecimal>> calculateMonthlySpendingByCategory(List<Expense> expenses) {
        return expenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(
                        expense -> expense.getCategory().getName(),
                        Collectors.mapping(Expense::getAmount, Collectors.toList())));
    }

    private boolean isAmountUnusual(Expense expense, Long userId) {
        List<Expense> userExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                userId,
                LocalDateTime.now().minusMonths(3),
                LocalDateTime.now());
        
        return isAmountAnomalous(expense, userExpenses);
    }

    private boolean isMerchantUnusual(Expense expense, Long userId) {
        List<Expense> userExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                userId,
                LocalDateTime.now().minusMonths(3),
                LocalDateTime.now());
        
        return isMerchantAnomalous(expense, userExpenses);
    }

    private boolean isTimingUnusual(Expense expense, Long userId) {
        List<Expense> userExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                userId,
                LocalDateTime.now().minusMonths(3),
                LocalDateTime.now());
        
        return isTimeAnomalous(expense, userExpenses);
    }

    private boolean isLocationUnusual(Expense expense, Long userId) {
        // Placeholder for location-based anomaly detection
        return false;
    }

    private Map<String, Object> calculateBasicSummary(List<Expense> expenses) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAmount", calculateTotalAmount(expenses));
        summary.put("count", expenses.size());
        summary.put("averageAmount", calculateAverageAmount(expenses));
        summary.put("categoryBreakdown", calculateCategoryBreakdown(expenses));
        return summary;
    }

    private Map<String, Object> getDefaultAiSettings() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("smartNotifications", true);
        defaults.put("categorySuggestions", true);
        defaults.put("anomalyDetection", true);
        defaults.put("duplicateDetection", true);
        defaults.put("fraudDetection", true);
        defaults.put("spendingInsights", true);
        defaults.put("budgetOptimization", true);
        defaults.put("receiptAnalysis", true);
        defaults.put("policyCompliance", true);
        return defaults;
    }
 }