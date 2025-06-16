package com.trackify.integration.ai;

import com.trackify.entity.Expense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class CategorySuggestionEngine {

    private static final Logger logger = LoggerFactory.getLogger(CategorySuggestionEngine.class);

    // Pre-defined category mappings based on keywords
    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = new HashMap<>();
    
    // Merchant to category mappings
    private static final Map<String, String> MERCHANT_CATEGORIES = new HashMap<>();
    
    // User-specific models (in production, store in database)
    private final Map<Long, Map<String, CategoryModel>> userModels = new ConcurrentHashMap<>();
    
    // Global model performance metrics
    private final Map<String, Object> performanceMetrics = new ConcurrentHashMap<>();

    static {
        initializeCategoryKeywords();
        initializeMerchantCategories();
    }

    /**
     * Suggest the most likely category for an expense
     */
    public String suggestCategory(String description, String merchantName, BigDecimal amount) {
        try {
            logger.debug("Suggesting category for description: '{}', merchant: '{}', amount: {}", 
                    description, merchantName, amount);

            Map<String, Double> categoryScores = calculateCategoryScores(description, merchantName, amount);
            
            return categoryScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("General");

        } catch (Exception e) {
            logger.error("Error suggesting category", e);
            return "General";
        }
    }

    /**
     * Suggest multiple categories with confidence scores
     */
    public List<String> suggestMultipleCategories(String description, String merchantName, BigDecimal amount) {
        try {
            Map<String, Double> categoryScores = calculateCategoryScores(description, merchantName, amount);
            
            return categoryScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error suggesting multiple categories", e);
            return Arrays.asList("General", "Business", "Office");
        }
    }

    /**
     * Get confidence scores for category suggestions
     */
    public Map<String, Double> getCategorySuggestionConfidence(String description, String merchantName, BigDecimal amount) {
        try {
            return calculateCategoryScores(description, merchantName, amount);

        } catch (Exception e) {
            logger.error("Error getting category suggestion confidence", e);
            return Map.of("General", 0.5);
        }
    }

    /**
     * Train the model with user expense data
     */
    public void trainModel(List<Expense> trainingData) {
        try {
            logger.info("Training category model with {} expenses", trainingData.size());
            
            // Group expenses by user
            Map<Long, List<Expense>> userExpenses = trainingData.stream()
                    .collect(Collectors.groupingBy(Expense::getUserId));

            for (Map.Entry<Long, List<Expense>> entry : userExpenses.entrySet()) {
                Long userId = entry.getKey();
                List<Expense> expenses = entry.getValue();
                
                trainUserModel(userId, expenses);
            }

            updatePerformanceMetrics(trainingData);

        } catch (Exception e) {
            logger.error("Error training category model", e);
        }
    }

    /**
     * Update user-specific model
     */
    public void updateUserModel(Long userId, List<Expense> userExpenses) {
        try {
            trainUserModel(userId, userExpenses);
            logger.debug("Updated user model for user {}", userId);

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
     * Suggest category for a specific user (using personalized model)
     */
    public String suggestCategoryForUser(Long userId, String description, String merchantName, BigDecimal amount) {
        try {
            Map<String, CategoryModel> userModel = userModels.get(userId);
            
            if (userModel != null && !userModel.isEmpty()) {
                // Use personalized model
                Map<String, Double> personalizedScores = calculatePersonalizedScores(
                        userId, description, merchantName, amount);
                
                return personalizedScores.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(suggestCategory(description, merchantName, amount));
            } else {
                // Fall back to general model
                return suggestCategory(description, merchantName, amount);
            }

        } catch (Exception e) {
            logger.error("Error suggesting category for user {}", userId, e);
            return suggestCategory(description, merchantName, amount);
        }
    }

    // Private helper methods

    private static void initializeCategoryKeywords() {
        CATEGORY_KEYWORDS.put("Travel", Set.of(
                "hotel", "flight", "airline", "airport", "taxi", "uber", "lyft", "rental", "car", 
                "train", "bus", "accommodation", "lodging", "airbnb", "booking"
        ));
        
        CATEGORY_KEYWORDS.put("Meals", Set.of(
                "restaurant", "food", "dining", "lunch", "dinner", "breakfast", "cafe", "coffee", 
                "starbucks", "mcdonalds", "pizza", "delivery", "takeout", "catering"
        ));
        
        CATEGORY_KEYWORDS.put("Office Supplies", Set.of(
                "office", "supplies", "stationery", "paper", "pen", "pencil", "stapler", "printer", 
                "ink", "toner", "folder", "notebook", "whiteboard", "desk"
        ));
        
        CATEGORY_KEYWORDS.put("Software", Set.of(
                "software", "app", "subscription", "license", "saas", "cloud", "microsoft", 
                "google", "adobe", "slack", "zoom", "github", "aws", "digital"
        ));
        
        CATEGORY_KEYWORDS.put("Marketing", Set.of(
                "marketing", "advertising", "promotion", "campaign", "social", "media", "facebook", 
                "google ads", "linkedin", "twitter", "instagram", "youtube", "seo"
        ));
        
        CATEGORY_KEYWORDS.put("Professional Services", Set.of(
                "consulting", "legal", "accounting", "lawyer", "accountant", "advisor", "consultant", 
                "audit", "tax", "professional", "service", "contractor"
        ));
        
        CATEGORY_KEYWORDS.put("Utilities", Set.of(
                "electricity", "gas", "water", "internet", "phone", "mobile", "utility", "bill", 
                "service", "telecom", "broadband", "wifi"
        ));
        
        CATEGORY_KEYWORDS.put("Equipment", Set.of(
                "equipment", "computer", "laptop", "monitor", "keyboard", "mouse", "camera", 
                "hardware", "machinery", "tool", "device", "electronics"
        ));
        
        CATEGORY_KEYWORDS.put("Training", Set.of(
                "training", "course", "education", "learning", "workshop", "seminar", "conference", 
                "certification", "class", "tutorial", "udemy", "coursera"
        ));
        
        CATEGORY_KEYWORDS.put("Entertainment", Set.of(
                "entertainment", "movie", "theater", "concert", "event", "show", "party", "celebration", 
                "team building", "recreation", "sports", "game"
        ));
        
        CATEGORY_KEYWORDS.put("Transportation", Set.of(
                "transportation", "gas", "fuel", "parking", "toll", "metro", "subway", "bus fare", 
                "commute", "mileage", "vehicle", "maintenance"
        ));
        
        CATEGORY_KEYWORDS.put("Healthcare", Set.of(
                "medical", "health", "doctor", "hospital", "pharmacy", "medicine", "insurance", 
                "dental", "vision", "clinic", "treatment", "prescription"
        ));
    }

    private static void initializeMerchantCategories() {
        // Common merchants and their categories
        MERCHANT_CATEGORIES.put("starbucks", "Meals");
        MERCHANT_CATEGORIES.put("mcdonalds", "Meals");
        MERCHANT_CATEGORIES.put("subway", "Meals");
        MERCHANT_CATEGORIES.put("pizza hut", "Meals");
        MERCHANT_CATEGORIES.put("dominos", "Meals");
        
        MERCHANT_CATEGORIES.put("uber", "Transportation");
        MERCHANT_CATEGORIES.put("lyft", "Transportation");
        MERCHANT_CATEGORIES.put("shell", "Transportation");
        MERCHANT_CATEGORIES.put("exxon", "Transportation");
        MERCHANT_CATEGORIES.put("chevron", "Transportation");
        
        MERCHANT_CATEGORIES.put("marriott", "Travel");
        MERCHANT_CATEGORIES.put("hilton", "Travel");
        MERCHANT_CATEGORIES.put("delta", "Travel");
        MERCHANT_CATEGORIES.put("american airlines", "Travel");
        MERCHANT_CATEGORIES.put("united", "Travel");
        
        MERCHANT_CATEGORIES.put("microsoft", "Software");
        MERCHANT_CATEGORIES.put("adobe", "Software");
        MERCHANT_CATEGORIES.put("slack", "Software");
        MERCHANT_CATEGORIES.put("zoom", "Software");
        MERCHANT_CATEGORIES.put("github", "Software");
        
        MERCHANT_CATEGORIES.put("staples", "Office Supplies");
        MERCHANT_CATEGORIES.put("office depot", "Office Supplies");
        MERCHANT_CATEGORIES.put("best buy", "Equipment");
        MERCHANT_CATEGORIES.put("amazon", "General");
        MERCHANT_CATEGORIES.put("walmart", "General");
    }

    private Map<String, Double> calculateCategoryScores(String description, String merchantName, BigDecimal amount) {
        Map<String, Double> scores = new HashMap<>();

        // Initialize all categories with base score
        for (String category : CATEGORY_KEYWORDS.keySet()) {
            scores.put(category, 0.0);
        }

        // Score based on description keywords
        if (description != null && !description.trim().isEmpty()) {
            String[] words = description.toLowerCase().split("\\s+");
            Set<String> descriptionWords = Set.of(words);

            for (Map.Entry<String, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
                String category = entry.getKey();
                Set<String> keywords = entry.getValue();

                long matchCount = keywords.stream()
                        .mapToLong(keyword -> descriptionWords.contains(keyword) ? 1 : 0)
                        .sum();

                if (matchCount > 0) {
                    double score = (double) matchCount / keywords.size();
                    scores.put(category, scores.get(category) + score * 0.6); // 60% weight
                }
            }
        }

        // Score based on merchant name
        if (merchantName != null && !merchantName.trim().isEmpty()) {
            String lowerMerchant = merchantName.toLowerCase();

            // Exact merchant match
            String exactCategory = MERCHANT_CATEGORIES.get(lowerMerchant);
            if (exactCategory != null) {
                scores.put(exactCategory, scores.get(exactCategory) + 0.8); // 80% weight
            } else {
                // Partial merchant match
                for (Map.Entry<String, String> entry : MERCHANT_CATEGORIES.entrySet()) {
                    if (lowerMerchant.contains(entry.getKey()) || entry.getKey().contains(lowerMerchant)) {
                        scores.put(entry.getValue(), scores.get(entry.getValue()) + 0.4); // 40% weight
                    }
                }
            }
        }

        // Score based on amount patterns (optional enhancement)
        if (amount != null) {
            addAmountBasedScoring(scores, amount);
        }

        // Normalize scores
        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxScore > 0) {
            scores.replaceAll((k, v) -> v / maxScore);
        }

        // Add small base probability for all categories
        scores.replaceAll((k, v) -> v + 0.01);

        return scores;
    }

    private void addAmountBasedScoring(Map<String, Double> scores, BigDecimal amount) {
        double amountValue = amount.doubleValue();

        // Typical amount ranges for different categories
        if (amountValue > 500) {
            scores.put("Travel", scores.get("Travel") + 0.2);
            scores.put("Equipment", scores.get("Equipment") + 0.2);
        } else if (amountValue > 100) {
            scores.put("Software", scores.get("Software") + 0.1);
            scores.put("Professional Services", scores.get("Professional Services") + 0.1);
        } else if (amountValue < 50) {
            scores.put("Meals", scores.get("Meals") + 0.1);
            scores.put("Office Supplies", scores.get("Office Supplies") + 0.1);
            scores.put("Transportation", scores.get("Transportation") + 0.1);
        }
    }

    private void trainUserModel(Long userId, List<Expense> expenses) {
        Map<String, CategoryModel> userModel = new HashMap<>();

        // Group expenses by category
        Map<String, List<Expense>> categoryGroups = expenses.stream()
                .filter(expense -> expense.getCategory() != null)
                .collect(Collectors.groupingBy(expense -> expense.getCategory().getName()));

        for (Map.Entry<String, List<Expense>> entry : categoryGroups.entrySet()) {
            String category = entry.getKey();
            List<Expense> categoryExpenses = entry.getValue();

            CategoryModel model = new CategoryModel();
            model.setCategory(category);
            model.setExpenseCount(categoryExpenses.size());
            
            // Extract common keywords for this category
            Set<String> commonKeywords = extractCommonKeywords(categoryExpenses);
            model.setKeywords(commonKeywords);
            
            // Extract common merchants
            Set<String> commonMerchants = extractCommonMerchants(categoryExpenses);
            model.setMerchants(commonMerchants);
            
            // Calculate amount statistics
            calculateAmountStatistics(model, categoryExpenses);

            userModel.put(category, model);
        }

        userModels.put(userId, userModel);
    }

    private Set<String> extractCommonKeywords(List<Expense> expenses) {
        Map<String, Integer> keywordCounts = new HashMap<>();

        for (Expense expense : expenses) {
            if (expense.getDescription() != null) {
                String[] words = expense.getDescription().toLowerCase().split("\\s+");
                for (String word : words) {
                    if (word.length() > 3 && !isStopWord(word)) {
                        keywordCounts.put(word, keywordCounts.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }

        // Return keywords that appear in at least 10% of expenses
        int threshold = Math.max(1, expenses.size() / 10);
        return keywordCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Set<String> extractCommonMerchants(List<Expense> expenses) {
        Map<String, Integer> merchantCounts = new HashMap<>();

        for (Expense expense : expenses) {
            if (expense.getMerchantName() != null) {
                String merchant = expense.getMerchantName().toLowerCase();
                merchantCounts.put(merchant, merchantCounts.getOrDefault(merchant, 0) + 1);
            }
        }

        // Return merchants that appear at least twice
        return merchantCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private void calculateAmountStatistics(CategoryModel model, List<Expense> expenses) {
        List<BigDecimal> amounts = expenses.stream()
                .map(Expense::getAmount)
                .sorted()
                .collect(Collectors.toList());

        if (!amounts.isEmpty()) {
            model.setMinAmount(amounts.get(0));
            model.setMaxAmount(amounts.get(amounts.size() - 1));
            
            BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            model.setAverageAmount(sum.divide(BigDecimal.valueOf(amounts.size()), 2, java.math.RoundingMode.HALF_UP));
            
            // Calculate median
            int medianIndex = amounts.size() / 2;
            if (amounts.size() % 2 == 0) {
                model.setMedianAmount(amounts.get(medianIndex - 1).add(amounts.get(medianIndex))
                        .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
            } else {
                model.setMedianAmount(amounts.get(medianIndex));
            }
        }
    }

    private Map<String, Double> calculatePersonalizedScores(Long userId, String description, 
                                                           String merchantName, BigDecimal amount) {
        Map<String, Double> baseScores = calculateCategoryScores(description, merchantName, amount);
        Map<String, CategoryModel> userModel = userModels.get(userId);

        if (userModel == null) {
            return baseScores;
        }

        // Enhance scores based on user's historical patterns
        for (Map.Entry<String, CategoryModel> entry : userModel.entrySet()) {
            String category = entry.getKey();
            CategoryModel model = entry.getValue();

            double personalizedScore = baseScores.getOrDefault(category, 0.0);

            // Boost score if description matches user's keywords for this category
            if (description != null && model.getKeywords() != null) {
                String[] words = description.toLowerCase().split("\\s+");
                long matchCount = Arrays.stream(words)
                        .filter(model.getKeywords()::contains)
                        .count();
                
                if (matchCount > 0) {
                    personalizedScore += 0.3 * (matchCount / (double) words.length);
                }
            }

            // Boost score if merchant matches user's merchants for this category
            if (merchantName != null && model.getMerchants() != null) {
                String lowerMerchant = merchantName.toLowerCase();
                if (model.getMerchants().contains(lowerMerchant)) {
                    personalizedScore += 0.4;
                }
            }

            // Boost score if amount is typical for this category for the user
            if (amount != null && model.getMinAmount() != null && model.getMaxAmount() != null) {
                if (amount.compareTo(model.getMinAmount()) >= 0 && 
                    amount.compareTo(model.getMaxAmount()) <= 0) {
                    personalizedScore += 0.2;
                }
            }

            baseScores.put(category, personalizedScore);
        }

        return baseScores;
    }

    private void updatePerformanceMetrics(List<Expense> trainingData) {
        performanceMetrics.put("totalTrainingExpenses", trainingData.size());
        performanceMetrics.put("uniqueCategories", trainingData.stream()
                .filter(e -> e.getCategory() != null)
                .map(e -> e.getCategory().getName())
                .collect(Collectors.toSet()).size());
        performanceMetrics.put("lastTrainingTime", new Date());
        performanceMetrics.put("userModelsCount", userModels.size());
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
                "the", "and", "for", "with", "from", "this", "that", "have", "has", "was", "were",
                "been", "being", "will", "would", "could", "should", "may", "might", "must"
        );
        return stopWords.contains(word.toLowerCase());
    }

    // Inner class to represent category models
    private static class CategoryModel {
        private String category;
        private int expenseCount;
        private Set<String> keywords;
        private Set<String> merchants;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private BigDecimal averageAmount;
        private BigDecimal medianAmount;

        // Getters and setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getExpenseCount() { return expenseCount; }
        public void setExpenseCount(int expenseCount) { this.expenseCount = expenseCount; }

        public Set<String> getKeywords() { return keywords; }
        public void setKeywords(Set<String> keywords) { this.keywords = keywords; }

        public Set<String> getMerchants() { return merchants; }
        public void setMerchants(Set<String> merchants) { this.merchants = merchants; }

        public BigDecimal getMinAmount() { return minAmount; }
        public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

        public BigDecimal getMaxAmount() { return maxAmount; }
        public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

        public BigDecimal getAverageAmount() { return averageAmount; }
        public void setAverageAmount(BigDecimal averageAmount) { this.averageAmount = averageAmount; }

        public BigDecimal getMedianAmount() { return medianAmount; }
        public void setMedianAmount(BigDecimal medianAmount) { this.medianAmount = medianAmount; }
    }
}