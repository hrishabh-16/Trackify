package com.trackify.integration.ai;

import com.trackify.entity.Expense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class AnomalyDetectionEngine {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionEngine.class);

    // Anomaly detection thresholds
    private static final double AMOUNT_ANOMALY_THRESHOLD = 2.5; // Standard deviations
    private static final double FREQUENCY_ANOMALY_THRESHOLD = 3.0; // Times normal frequency
    private static final int MIN_HISTORICAL_DATA = 10; // Minimum expenses needed for detection
    private static final int ANALYSIS_WINDOW_DAYS = 90; // Days to look back for patterns

    // User-specific baseline models
    private final Map<Long, UserBaselineModel> userBaselines = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final Map<String, Object> performanceMetrics = new ConcurrentHashMap<>();

    /**
     * Check if an expense is anomalous based on user's historical patterns
     */
    public boolean isAnomalous(Expense expense, List<Expense> userHistoricalExpenses) {
        try {
            if (userHistoricalExpenses.size() < MIN_HISTORICAL_DATA) {
                logger.debug("Insufficient historical data for anomaly detection (need {}, have {})", 
                        MIN_HISTORICAL_DATA, userHistoricalExpenses.size());
                return false;
            }

            List<AnomalyType> anomalies = detectAnomalies(expense, userHistoricalExpenses);
            return !anomalies.isEmpty();

        } catch (Exception e) {
            logger.error("Error detecting anomaly for expense {}", expense.getId(), e);
            return false;
        }
    }

    /**
     * Detect specific types of anomalies in an expense
     */
    public List<AnomalyType> detectAnomalies(Expense expense, List<Expense> userHistoricalExpenses) {
        List<AnomalyType> anomalies = new ArrayList<>();

        try {
            // Amount-based anomalies
            if (isAmountAnomalous(expense, userHistoricalExpenses)) {
                anomalies.add(AnomalyType.UNUSUAL_AMOUNT);
            }

            // Time-based anomalies
            if (isTimeAnomalous(expense, userHistoricalExpenses)) {
                anomalies.add(AnomalyType.UNUSUAL_TIME);
            }

            // Frequency-based anomalies
            if (isFrequencyAnomalous(expense, userHistoricalExpenses)) {
                anomalies.add(AnomalyType.HIGH_FREQUENCY);
            }

            // Category-based anomalies
            if (isCategoryAnomalous(expense, userHistoricalExpenses)) {
                anomalies.add(AnomalyType.UNUSUAL_CATEGORY);
            }

            // Merchant-based anomalies
            if (isMerchantAnomalous(expense, userHistoricalExpenses)) {
                anomalies.add(AnomalyType.NEW_MERCHANT);
            }

            // Pattern-based anomalies
            if (isPatternAnomalous(expense, userHistoricalExpenses)) {
                anomalies.add(AnomalyType.UNUSUAL_PATTERN);
            }

        } catch (Exception e) {
            logger.error("Error detecting anomalies for expense {}", expense.getId(), e);
        }

        return anomalies;
    }

    /**
     * Calculate anomaly score (0.0 to 1.0, where 1.0 is most anomalous)
     */
    public double calculateAnomalyScore(Expense expense, List<Expense> userHistoricalExpenses) {
        try {
            if (userHistoricalExpenses.size() < MIN_HISTORICAL_DATA) {
                return 0.0;
            }

            double totalScore = 0.0;
            int scoreCount = 0;

            // Amount anomaly score (30% weight)
            double amountScore = calculateAmountAnomalyScore(expense, userHistoricalExpenses);
            totalScore += amountScore * 0.3;
            scoreCount++;

            // Time anomaly score (15% weight)
            double timeScore = calculateTimeAnomalyScore(expense, userHistoricalExpenses);
            totalScore += timeScore * 0.15;
            scoreCount++;

            // Frequency anomaly score (20% weight)
            double frequencyScore = calculateFrequencyAnomalyScore(expense, userHistoricalExpenses);
            totalScore += frequencyScore * 0.2;
            scoreCount++;

            // Category anomaly score (15% weight)
            double categoryScore = calculateCategoryAnomalyScore(expense, userHistoricalExpenses);
            totalScore += categoryScore * 0.15;
            scoreCount++;

            // Merchant anomaly score (20% weight)
            double merchantScore = calculateMerchantAnomalyScore(expense, userHistoricalExpenses);
            totalScore += merchantScore * 0.2;
            scoreCount++;

            return Math.min(1.0, totalScore);

        } catch (Exception e) {
            logger.error("Error calculating anomaly score for expense {}", expense.getId(), e);
            return 0.0;
        }
    }

    /**
     * Train the anomaly detection model with user data
     */
    public void trainModel(List<Expense> trainingData) {
        try {
            logger.info("Training anomaly detection model with {} expenses", trainingData.size());

            // Group expenses by user
            Map<Long, List<Expense>> userExpenses = trainingData.stream()
                    .collect(Collectors.groupingBy(Expense::getUserId));

            for (Map.Entry<Long, List<Expense>> entry : userExpenses.entrySet()) {
                Long userId = entry.getKey();
                List<Expense> expenses = entry.getValue();
                
                if (expenses.size() >= MIN_HISTORICAL_DATA) {
                    trainUserModel(userId, expenses);
                }
            }

            updatePerformanceMetrics(trainingData);

        } catch (Exception e) {
            logger.error("Error training anomaly detection model", e);
        }
    }

    /**
     * Update user-specific baseline model
     */
    public void updateUserModel(Long userId, List<Expense> userExpenses) {
        try {
            if (userExpenses.size() >= MIN_HISTORICAL_DATA) {
                trainUserModel(userId, userExpenses);
                logger.debug("Updated user baseline model for user {}", userId);
            }

        } catch (Exception e) {
            logger.error("Error updating user model for user {}", userId, e);
        }
    }

    /**
     * Get model performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        return new HashMap<>(performanceMetrics);
    }

    /**
     * Get anomaly statistics for a user
     */
    public Map<String, Object> getUserAnomalyStatistics(Long userId, List<Expense> expenses) {
        Map<String, Object> stats = new HashMap<>();

        try {
            int totalExpenses = expenses.size();
            if (totalExpenses == 0) {
                return stats;
            }

            int anomalousExpenses = 0;
            Map<AnomalyType, Integer> anomalyTypeCounts = new HashMap<>();

            for (Expense expense : expenses) {
                List<Expense> historicalData = expenses.stream()
                        .filter(e -> e.getExpenseDate().isBefore(expense.getExpenseDate()))
                        .collect(Collectors.toList());

                if (historicalData.size() >= MIN_HISTORICAL_DATA) {
                    List<AnomalyType> anomalies = detectAnomalies(expense, historicalData);
                    
                    if (!anomalies.isEmpty()) {
                        anomalousExpenses++;
                        
                        for (AnomalyType type : anomalies) {
                            anomalyTypeCounts.put(type, anomalyTypeCounts.getOrDefault(type, 0) + 1);
                        }
                    }
                }
            }

            stats.put("totalExpenses", totalExpenses);
            stats.put("anomalousExpenses", anomalousExpenses);
            stats.put("anomalyRate", totalExpenses > 0 ? (double) anomalousExpenses / totalExpenses : 0.0);
            stats.put("anomalyBreakdown", anomalyTypeCounts);

        } catch (Exception e) {
            logger.error("Error calculating anomaly statistics for user {}", userId, e);
        }

        return stats;
    }

    // Private helper methods

    private boolean isAmountAnomalous(Expense expense, List<Expense> historicalExpenses) {
        try {
            // Filter expenses in same category if possible
            List<Expense> categoryExpenses = historicalExpenses.stream()
                    .filter(e -> expense.getCategory() != null && 
                               e.getCategory() != null && 
                               expense.getCategory().getName().equals(e.getCategory().getName()))
                    .collect(Collectors.toList());

            List<Expense> relevantExpenses = categoryExpenses.size() >= 5 ? 
                    categoryExpenses : historicalExpenses;

            if (relevantExpenses.size() < 3) {
                return false;
            }

            List<BigDecimal> amounts = relevantExpenses.stream()
                    .map(Expense::getAmount)
                    .collect(Collectors.toList());

            BigDecimal mean = calculateMean(amounts);
            BigDecimal stdDev = calculateStandardDeviation(amounts, mean);

            if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
                return false; // No variation in historical data
            }

            BigDecimal deviation = expense.getAmount().subtract(mean).abs();
            BigDecimal threshold = stdDev.multiply(BigDecimal.valueOf(AMOUNT_ANOMALY_THRESHOLD));

            return deviation.compareTo(threshold) > 0;

        } catch (Exception e) {
            logger.error("Error checking amount anomaly", e);
            return false;
        }
    }

    private boolean isTimeAnomalous(Expense expense, List<Expense> historicalExpenses) {
        try {
            // Check if expense is submitted at unusual hour
            int expenseHour = expense.getCreatedAt().getHour();
            
            Map<Integer, Long> hourFrequency = historicalExpenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getCreatedAt().getHour(),
                            Collectors.counting()));

            // If this hour has never been used before, it's potentially anomalous
            if (!hourFrequency.containsKey(expenseHour)) {
                return expenseHour < 6 || expenseHour > 22; // Only flag if outside business hours
            }

            // Check if this is an unusually rare time
            long totalExpenses = historicalExpenses.size();
            long hourCount = hourFrequency.get(expenseHour);
            double hourFrequencyRate = (double) hourCount / totalExpenses;

            return hourFrequencyRate < 0.02; // Less than 2% of expenses at this hour

        } catch (Exception e) {
            logger.error("Error checking time anomaly", e);
            return false;
        }
    }

    private boolean isFrequencyAnomalous(Expense expense, List<Expense> historicalExpenses) {
        try {
            // Check for high frequency of similar expenses in recent time
            LocalDate recentCutoff = expense.getExpenseDate().minusDays(7);
            
            List<Expense> recentSimilarExpenses = historicalExpenses.stream()
                    .filter(e -> e.getExpenseDate().isAfter(recentCutoff))
                    .filter(e -> isSimilarExpense(expense, e))
                    .collect(Collectors.toList());

            // Calculate normal frequency for similar expenses
            long totalSimilarExpenses = historicalExpenses.stream()
                    .mapToLong(e -> isSimilarExpense(expense, e) ? 1 : 0)
                    .sum();

            if (totalSimilarExpenses == 0) {
                return false;
            }

            // Calculate expected frequency per week
            long historyDays = ChronoUnit.DAYS.between(
                    historicalExpenses.stream()
                            .map(Expense::getExpenseDate)
                            .min(LocalDate::compareTo)
                            .orElse(expense.getExpenseDate()),
                    expense.getExpenseDate());

            double expectedWeeklyFrequency = (double) totalSimilarExpenses * 7 / Math.max(historyDays, 7);
            
            return recentSimilarExpenses.size() > expectedWeeklyFrequency * FREQUENCY_ANOMALY_THRESHOLD;

        } catch (Exception e) {
            logger.error("Error checking frequency anomaly", e);
            return false;
        }
    }

    private boolean isCategoryAnomalous(Expense expense, List<Expense> historicalExpenses) {
        try {
            if (expense.getCategory() == null) {
                return false;
            }

            String expenseCategory = expense.getCategory().getName();
            
            // Check if this category has been used before
            boolean categoryExists = historicalExpenses.stream()
                    .anyMatch(e -> e.getCategory() != null && 
                               expenseCategory.equals(e.getCategory().getName()));

            if (!categoryExists) {
                return true; // New category is anomalous
            }

            // Check if category is rarely used
            long categoryCount = historicalExpenses.stream()
                    .filter(e -> e.getCategory() != null)
                    .mapToLong(e -> expenseCategory.equals(e.getCategory().getName()) ? 1 : 0)
                    .sum();

            double categoryFrequency = (double) categoryCount / historicalExpenses.size();
            
            return categoryFrequency < 0.05; // Less than 5% usage

        } catch (Exception e) {
            logger.error("Error checking category anomaly", e);
            return false;
        }
    }

    private boolean isMerchantAnomalous(Expense expense, List<Expense> historicalExpenses) {
        try {
            if (expense.getMerchantName() == null || expense.getMerchantName().trim().isEmpty()) {
                return false;
            }

            String merchantName = expense.getMerchantName().toLowerCase();
            
            // Check if this exact merchant has been used before
            boolean merchantExists = historicalExpenses.stream()
                    .anyMatch(e -> e.getMerchantName() != null && 
                               merchantName.equals(e.getMerchantName().toLowerCase()));

            if (!merchantExists) {
                // Check for similar merchant names (partial matches)
                boolean similarMerchantExists = historicalExpenses.stream()
                        .anyMatch(e -> e.getMerchantName() != null && 
                                     calculateTextSimilarity(merchantName, e.getMerchantName().toLowerCase()) > 0.7);
                
                return !similarMerchantExists; // New merchant with no similar matches
            }

            return false; // Known merchant

        } catch (Exception e) {
            logger.error("Error checking merchant anomaly", e);
            return false;
        }
    }

    private boolean isPatternAnomalous(Expense expense, List<Expense> historicalExpenses) {
        try {
            // Look for unusual patterns like round numbers, duplicate descriptions, etc.
            
            // Check for suspicious round amounts
            if (isRoundAmount(expense.getAmount())) {
                long roundAmountCount = historicalExpenses.stream()
                        .mapToLong(e -> isRoundAmount(e.getAmount()) ? 1 : 0)
                        .sum();
                
                double roundAmountFrequency = (double) roundAmountCount / historicalExpenses.size();
                
                // If round amounts are rare in history but this is round, it's potentially suspicious
                if (roundAmountFrequency < 0.1) {
                    return true;
                }
            }

            // Check for duplicate or very similar descriptions
            if (expense.getDescription() != null) {
                long similarDescriptionCount = historicalExpenses.stream()
                        .filter(e -> e.getDescription() != null)
                        .mapToLong(e -> calculateTextSimilarity(
                                expense.getDescription().toLowerCase(),
                                e.getDescription().toLowerCase()) > 0.9 ? 1 : 0)
                        .sum();
                
                // Multiple very similar descriptions might indicate copy-paste fraud
                if (similarDescriptionCount > 5) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            logger.error("Error checking pattern anomaly", e);
            return false;
        }
    }

    private double calculateAmountAnomalyScore(Expense expense, List<Expense> historicalExpenses) {
        try {
            List<BigDecimal> amounts = historicalExpenses.stream()
                    .map(Expense::getAmount)
                    .collect(Collectors.toList());

            BigDecimal mean = calculateMean(amounts);
            BigDecimal stdDev = calculateStandardDeviation(amounts, mean);

            if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
                return 0.0;
            }

            BigDecimal deviation = expense.getAmount().subtract(mean).abs();
            BigDecimal normalizedDeviation = deviation.divide(stdDev, 4, RoundingMode.HALF_UP);

            // Convert to 0-1 scale, where 2.5 std devs = 1.0
            return Math.min(1.0, normalizedDeviation.doubleValue() / AMOUNT_ANOMALY_THRESHOLD);

        } catch (Exception e) {
            logger.error("Error calculating amount anomaly score", e);
            return 0.0;
        }
    }

    private double calculateTimeAnomalyScore(Expense expense, List<Expense> historicalExpenses) {
        try {
            int expenseHour = expense.getCreatedAt().getHour();
            
            Map<Integer, Long> hourFrequency = historicalExpenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getCreatedAt().getHour(),
                            Collectors.counting()));

            long totalExpenses = historicalExpenses.size();
            long hourCount = hourFrequency.getOrDefault(expenseHour, 0L);
            
            if (hourCount == 0) {
                // Never used this hour before
                return (expenseHour < 6 || expenseHour > 22) ? 1.0 : 0.5;
            }

            double hourFrequencyRate = (double) hourCount / totalExpenses;
            
            // Lower frequency = higher anomaly score
            return Math.max(0.0, 1.0 - (hourFrequencyRate * 50)); // Scale so 2% frequency = 0 score

        } catch (Exception e) {
            logger.error("Error calculating time anomaly score", e);
            return 0.0;
        }
    }

    private double calculateFrequencyAnomalyScore(Expense expense, List<Expense> historicalExpenses) {
        try {
            LocalDate recentCutoff = expense.getExpenseDate().minusDays(7);
            
            long recentSimilarCount = historicalExpenses.stream()
                    .filter(e -> e.getExpenseDate().isAfter(recentCutoff))
                    .mapToLong(e -> isSimilarExpense(expense, e) ? 1 : 0)
                    .sum();

            // Calculate expected frequency
            long totalSimilarExpenses = historicalExpenses.stream()
                    .mapToLong(e -> isSimilarExpense(expense, e) ? 1 : 0)
                    .sum();

            if (totalSimilarExpenses == 0) {
                return 0.0;
            }

            long historyDays = ChronoUnit.DAYS.between(
                    historicalExpenses.stream()
                            .map(Expense::getExpenseDate)
                            .min(LocalDate::compareTo)
                            .orElse(expense.getExpenseDate()),
                    expense.getExpenseDate());

            double expectedWeeklyFrequency = (double) totalSimilarExpenses * 7 / Math.max(historyDays, 7);
            
            if (expectedWeeklyFrequency == 0) {
                return 0.0;
            }

            double frequencyRatio = recentSimilarCount / expectedWeeklyFrequency;
            
            // Score increases with frequency ratio above 1.0
            return Math.min(1.0, Math.max(0.0, (frequencyRatio - 1.0) / FREQUENCY_ANOMALY_THRESHOLD));

        } catch (Exception e) {
            logger.error("Error calculating frequency anomaly score", e);
            return 0.0;
        }
    }

    private double calculateCategoryAnomalyScore(Expense expense, List<Expense> historicalExpenses) {
        try {
            if (expense.getCategory() == null) {
                return 0.0;
            }

            String expenseCategory = expense.getCategory().getName();
            
            long categoryCount = historicalExpenses.stream()
                    .filter(e -> e.getCategory() != null)
                    .mapToLong(e -> expenseCategory.equals(e.getCategory().getName()) ? 1 : 0)
                    .sum();

            if (categoryCount == 0) {
                return 1.0; // Completely new category
            }

            double categoryFrequency = (double) categoryCount / historicalExpenses.size();
            
            // Lower frequency = higher anomaly score
            return Math.max(0.0, 1.0 - (categoryFrequency * 20)); // Scale so 5% frequency = 0 score

        } catch (Exception e) {
            logger.error("Error calculating category anomaly score", e);
            return 0.0;
        }
    }

    private double calculateMerchantAnomalyScore(Expense expense, List<Expense> historicalExpenses) {
        try {
            if (expense.getMerchantName() == null || expense.getMerchantName().trim().isEmpty()) {
                return 0.0;
            }

            String merchantName = expense.getMerchantName().toLowerCase();
            
            // Check exact match
            boolean exactMatch = historicalExpenses.stream()
                    .anyMatch(e -> e.getMerchantName() != null && 
                               merchantName.equals(e.getMerchantName().toLowerCase()));

            if (exactMatch) {
                return 0.0; // Known merchant
            }

            // Check for similar merchants
            double maxSimilarity = historicalExpenses.stream()
                    .filter(e -> e.getMerchantName() != null)
                    .mapToDouble(e -> calculateTextSimilarity(merchantName, e.getMerchantName().toLowerCase()))
                    .max()
                    .orElse(0.0);

            // High similarity = low anomaly score
            return Math.max(0.0, 1.0 - maxSimilarity);

        } catch (Exception e) {
            logger.error("Error calculating merchant anomaly score", e);
            return 0.0;
        }
    }

    private void trainUserModel(Long userId, List<Expense> expenses) {
        try {
            UserBaselineModel baseline = new UserBaselineModel();
            baseline.setUserId(userId);
            baseline.setExpenseCount(expenses.size());
            
            // Calculate amount statistics
            List<BigDecimal> amounts = expenses.stream()
                    .map(Expense::getAmount)
                    .collect(Collectors.toList());
            
            baseline.setAverageAmount(calculateMean(amounts));
            baseline.setAmountStdDev(calculateStandardDeviation(amounts, baseline.getAverageAmount()));
            
            // Calculate category patterns
            Map<String, Long> categoryFrequency = expenses.stream()
                    .filter(e -> e.getCategory() != null)
                    .collect(Collectors.groupingBy(
                            e -> e.getCategory().getName(),
                            Collectors.counting()));
            baseline.setCategoryFrequency(categoryFrequency);
            
            // Calculate merchant patterns
            Set<String> merchants = expenses.stream()
                    .filter(e -> e.getMerchantName() != null)
                    .map(e -> e.getMerchantName().toLowerCase())
                    .collect(Collectors.toSet());
            baseline.setKnownMerchants(merchants);
            
            // Calculate time patterns
            Map<Integer, Long> hourFrequency = expenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getCreatedAt().getHour(),
                            Collectors.counting()));
            baseline.setHourFrequency(hourFrequency);
            
            baseline.setLastUpdated(new Date());
            
            userBaselines.put(userId, baseline);

        } catch (Exception e) {
            logger.error("Error training user baseline model for user {}", userId, e);
        }
    }

    private void updatePerformanceMetrics(List<Expense> trainingData) {
        performanceMetrics.put("totalTrainingExpenses", trainingData.size());
        performanceMetrics.put("userBaselinesCount", userBaselines.size());
        performanceMetrics.put("lastTrainingTime", new Date());
        
        // Calculate some basic statistics
        Map<Long, List<Expense>> userGroups = trainingData.stream()
                .collect(Collectors.groupingBy(Expense::getUserId));
        
        performanceMetrics.put("averageExpensesPerUser", 
                userGroups.values().stream().mapToInt(List::size).average().orElse(0.0));
        performanceMetrics.put("uniqueUsers", userGroups.size());
    }

    private boolean isSimilarExpense(Expense expense1, Expense expense2) {
        try {
            // Check category similarity
            boolean categoryMatch = false;
            if (expense1.getCategory() != null && expense2.getCategory() != null) {
                categoryMatch = expense1.getCategory().getName().equals(expense2.getCategory().getName());
            }

            // Check merchant similarity
            boolean merchantMatch = false;
            if (expense1.getMerchantName() != null && expense2.getMerchantName() != null) {
                merchantMatch = calculateTextSimilarity(
                        expense1.getMerchantName().toLowerCase(),
                        expense2.getMerchantName().toLowerCase()) > 0.7;
            }

            // Check amount similarity (within 20%)
            boolean amountMatch = false;
            if (expense1.getAmount() != null && expense2.getAmount() != null) {
                BigDecimal diff = expense1.getAmount().subtract(expense2.getAmount()).abs();
                BigDecimal threshold = expense1.getAmount().multiply(BigDecimal.valueOf(0.2));
                amountMatch = diff.compareTo(threshold) <= 0;
            }

            // Consider similar if at least 2 out of 3 criteria match
            int matches = (categoryMatch ? 1 : 0) + (merchantMatch ? 1 : 0) + (amountMatch ? 1 : 0);
            return matches >= 2;

        } catch (Exception e) {
            logger.error("Error checking expense similarity", e);
            return false;
        }
    }

    private boolean isRoundAmount(BigDecimal amount) {
        if (amount == null) return false;
        
        // Check if amount is a round number (ends in .00, .50, or is divisible by 10/25/50/100)
        return amount.remainder(BigDecimal.valueOf(100)).equals(BigDecimal.ZERO) ||
               amount.remainder(BigDecimal.valueOf(50)).equals(BigDecimal.ZERO) ||
               amount.remainder(BigDecimal.valueOf(25)).equals(BigDecimal.ZERO) ||
               amount.remainder(BigDecimal.valueOf(10)).equals(BigDecimal.ZERO) ||
               amount.toString().endsWith(".50");
    }

    private BigDecimal calculateMean(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> values, BigDecimal mean) {
        if (values.size() < 2) return BigDecimal.ZERO;
        
        BigDecimal variance = values.stream()
                .map(value -> value.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size() - 1), 4, RoundingMode.HALF_UP);
        
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        if (text1.equals(text2)) return 1.0;
        
        // Simple Jaccard similarity
        Set<String> words1 = Set.of(text1.toLowerCase().split("\\s+"));
        Set<String> words2 = Set.of(text2.toLowerCase().split("\\s+"));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    // Enum for anomaly types
    public enum AnomalyType {
        UNUSUAL_AMOUNT("Unusual amount for this type of expense"),
        UNUSUAL_TIME("Expense submitted at unusual time"),
        HIGH_FREQUENCY("High frequency of similar expenses"),
        UNUSUAL_CATEGORY("Rarely used or new expense category"),
        NEW_MERCHANT("New or unknown merchant"),
        UNUSUAL_PATTERN("Unusual expense pattern detected");

        private final String description;

        AnomalyType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Inner class for user baseline models
    private static class UserBaselineModel {
        private Long userId;
        private int expenseCount;
        private BigDecimal averageAmount;
        private BigDecimal amountStdDev;
        private Map<String, Long> categoryFrequency;
        private Set<String> knownMerchants;
        private Map<Integer, Long> hourFrequency;
        private Date lastUpdated;

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public int getExpenseCount() { return expenseCount; }
        public void setExpenseCount(int expenseCount) { this.expenseCount = expenseCount; }

        public BigDecimal getAverageAmount() { return averageAmount; }
        public void setAverageAmount(BigDecimal averageAmount) { this.averageAmount = averageAmount; }

        public BigDecimal getAmountStdDev() { return amountStdDev; }
        public void setAmountStdDev(BigDecimal amountStdDev) { this.amountStdDev = amountStdDev; }

        public Map<String, Long> getCategoryFrequency() { return categoryFrequency; }
        public void setCategoryFrequency(Map<String, Long> categoryFrequency) { this.categoryFrequency = categoryFrequency; }

        public Set<String> getKnownMerchants() { return knownMerchants; }
        public void setKnownMerchants(Set<String> knownMerchants) { this.knownMerchants = knownMerchants; }

        public Map<Integer, Long> getHourFrequency() { return hourFrequency; }
        public void setHourFrequency(Map<Integer, Long> hourFrequency) { this.hourFrequency = hourFrequency; }

        public Date getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}