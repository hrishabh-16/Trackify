package com.trackify.integration.bank;

import com.trackify.dto.request.ExpenseRequest;
import com.trackify.entity.Category;
import com.trackify.entity.Expense;
import com.trackify.entity.User;
import com.trackify.enums.ExpenseStatus;
import com.trackify.enums.PaymentMethod;
import com.trackify.integration.ai.CategorySuggestionEngine;
import com.trackify.repository.CategoryRepository;
import com.trackify.repository.ExpenseRepository;
import com.trackify.repository.UserRepository;
import com.trackify.service.NotificationService;
import com.trackify.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UpiTransactionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(UpiTransactionProcessor.class);

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategorySuggestionEngine categorySuggestionEngine;

    @Autowired
    private NotificationService notificationService;

    // UPI ID patterns
    private static final Pattern UPI_ID_PATTERN = Pattern.compile("([\\w.-]+@[\\w-]+)", Pattern.CASE_INSENSITIVE);
    
    // Amount patterns
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_PATTERN_2 = Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|₹)", Pattern.CASE_INSENSITIVE);
    
    // Transaction ID patterns
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile("(?:txn|transaction|ref|reference)\\s*(?:id|no|number)?\\s*:?\\s*([a-z0-9]+)", Pattern.CASE_INSENSITIVE);
    
    // Merchant/Receiver patterns
    private static final Pattern MERCHANT_PATTERN = Pattern.compile("(?:to|paid to|received from|from)\\s+([a-z0-9\\s@._-]+)", Pattern.CASE_INSENSITIVE);
    
    // Date patterns
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:am|pm)?)", Pattern.CASE_INSENSITIVE);

    // UPI App keywords
    private static final Map<String, String> UPI_APP_KEYWORDS = new HashMap<>();
    static {
        UPI_APP_KEYWORDS.put("paytm", "Paytm");
        UPI_APP_KEYWORDS.put("phonepe", "PhonePe");
        UPI_APP_KEYWORDS.put("gpay", "Google Pay");
        UPI_APP_KEYWORDS.put("googlepay", "Google Pay");
        UPI_APP_KEYWORDS.put("amazonpay", "Amazon Pay");
        UPI_APP_KEYWORDS.put("bhim", "BHIM");
        UPI_APP_KEYWORDS.put("yono", "SBI Yono");
        UPI_APP_KEYWORDS.put("mobikwik", "MobiKwik");
        UPI_APP_KEYWORDS.put("freecharge", "FreeCharge");
        UPI_APP_KEYWORDS.put("airtel", "Airtel Money");
        UPI_APP_KEYWORDS.put("jiomoney", "JioMoney");
    }

    /**
     * Process UPI transaction data and create expense
     */
    public Expense processUpiTransaction(String transactionData, Long userId) {
        try {
            logger.info("Processing UPI transaction for user: {}", userId);
            
            UpiTransactionInfo transactionInfo = extractTransactionInfo(transactionData);
            
            if (transactionInfo.getAmount() == null || transactionInfo.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Invalid or missing amount in transaction data");
                return null;
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // Create expense from transaction info
            Expense expense = createExpenseFromTransaction(transactionInfo, user);
            
            // Save expense
            expense = expenseRepository.save(expense);
            
            logger.info("Successfully created expense from UPI transaction: {}", expense.getId());
            
            // Send notification
            notificationService.notifyExpenseSubmitted(expense.getId(), userId, userId);
            
            return expense;
            
        } catch (Exception e) {
            logger.error("Error processing UPI transaction for user: {}", userId, e);
            throw new RuntimeException("Failed to process UPI transaction", e);
        }
    }

    /**
     * Extract transaction information from text data
     */
    public UpiTransactionInfo extractTransactionInfo(String text) {
        UpiTransactionInfo info = new UpiTransactionInfo();
        
        if (text == null || text.trim().isEmpty()) {
            return info;
        }
        
        String normalizedText = text.toLowerCase().trim();
        
        // Extract amount
        info.setAmount(extractAmount(text));
        
        // Extract UPI ID
        info.setUpiId(extractUpiId(text));
        
        // Extract transaction ID
        info.setTransactionId(extractTransactionId(text));
        
        // Extract merchant/receiver name
        info.setMerchantName(extractMerchantName(text));
        
        // Extract date and time
        info.setTransactionDate(extractTransactionDate(text));
        
        // Extract UPI app name
        info.setUpiApp(extractUpiApp(normalizedText));
        
        // Determine transaction type
        info.setTransactionType(determineTransactionType(normalizedText));
        
        // Extract additional notes
        info.setNotes(extractNotes(text));
        
        return info;
    }

    // All other extraction methods remain the same as in your original code...
    // (extractAmount, parseAmount, extractUpiId, etc.)

    /**
     * Extract amount from transaction text
     */
    private BigDecimal extractAmount(String text) {
        // Try first pattern: Rs. 100, ₹ 100
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            return parseAmount(matcher.group(1));
        }
        
        // Try second pattern: 100 Rs, 100 ₹
        matcher = AMOUNT_PATTERN_2.matcher(text);
        if (matcher.find()) {
            return parseAmount(matcher.group(1));
        }
        
        return null;
    }

    /**
     * Parse amount string to BigDecimal
     */
    private BigDecimal parseAmount(String amountStr) {
        try {
            // Remove commas and parse
            String cleanAmount = amountStr.replaceAll(",", "");
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse amount: {}", amountStr);
            return null;
        }
    }

    /**
     * Extract UPI ID from text
     */
    private String extractUpiId(String text) {
        Matcher matcher = UPI_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract transaction ID from text
     */
    private String extractTransactionId(String text) {
        Matcher matcher = TRANSACTION_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract merchant/receiver name from text
     */
    private String extractMerchantName(String text) {
        Matcher matcher = MERCHANT_PATTERN.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            // Clean up the name (remove UPI ID if present)
            if (name.contains("@")) {
                String[] parts = name.split("@");
                name = parts[0].trim();
            }
            return name.length() > 100 ? name.substring(0, 100) : name;
        }
        return null;
    }

    /**
     * Extract transaction date from text
     */
    private LocalDateTime extractTransactionDate(String text) {
        LocalDate date = LocalDate.now(); // Default to today
        
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            date = parseDate(dateMatcher.group(1));
        }
        
        // Extract time if available
        Matcher timeMatcher = TIME_PATTERN.matcher(text);
        if (timeMatcher.find()) {
            return parseDateTime(date, timeMatcher.group(1));
        }
        
        return date.atStartOfDay();
    }

    /**
     * Parse date string to LocalDate
     */
    private LocalDate parseDate(String dateStr) {
        try {
            // Try different date formats
            String[] formats = {
                "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "yyyy-MM-dd",
                "dd/MM/yy", "dd-MM-yy", "yy/MM/dd", "yy-MM-dd"
            };
            
            for (String format : formats) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                    return LocalDate.parse(dateStr, formatter);
                } catch (DateTimeParseException ignored) {
                    // Try next format
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse date: {}", dateStr);
        }
        
        return LocalDate.now(); // Default to today if parsing fails
    }

    /**
     * Parse date and time to LocalDateTime
     */
    private LocalDateTime parseDateTime(LocalDate date, String timeStr) {
        try {
            // Clean time string
            timeStr = timeStr.trim().toLowerCase();
            
            // Handle AM/PM
            boolean isPM = timeStr.contains("pm");
            timeStr = timeStr.replaceAll("\\s*(am|pm)\\s*", "");
            
            String[] timeParts = timeStr.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            // Convert to 24-hour format
            if (isPM && hour != 12) {
                hour += 12;
            } else if (!isPM && hour == 12) {
                hour = 0;
            }
            
            return date.atTime(hour, minute);
            
        } catch (Exception e) {
            logger.warn("Failed to parse time: {}", timeStr);
            return date.atStartOfDay();
        }
    }

    /**
     * Extract UPI app name from text
     */
    private String extractUpiApp(String text) {
        for (Map.Entry<String, String> entry : UPI_APP_KEYWORDS.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "UPI";
    }

    /**
     * Determine transaction type (DEBIT/CREDIT)
     */
    private String determineTransactionType(String text) {
        if (text.contains("paid") || text.contains("debited") || text.contains("sent")) {
            return "DEBIT";
        } else if (text.contains("received") || text.contains("credited")) {
            return "CREDIT";
        }
        return "DEBIT"; // Default assumption for expense tracking
    }

    /**
     * Extract additional notes from transaction
     */
    private String extractNotes(String text) {
        // Look for common note patterns
        Pattern notePattern = Pattern.compile("(?:for|note|remark|description)\\s*:?\\s*([^\\n\\r]{1,200})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = notePattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // If no specific note pattern, return cleaned up text (first 200 chars)
        String cleanText = text.replaceAll("[\\r\\n]+", " ").trim();
        return cleanText.length() > 200 ? cleanText.substring(0, 200) + "..." : cleanText;
    }

    /**
     * Create expense from transaction info
     */
    private Expense createExpenseFromTransaction(UpiTransactionInfo transactionInfo, User user) {
        Expense expense = new Expense();
        
        // Basic expense details
        expense.setUserId(user.getId());
        expense.setAmount(transactionInfo.getAmount());
        expense.setExpenseDate(transactionInfo.getTransactionDate().toLocalDate());
        expense.setTitle(generateExpenseTitle(transactionInfo));
        expense.setDescription(generateExpenseDescription(transactionInfo));
        expense.setStatus(ExpenseStatus.PENDING);
        expense.setPaymentMethod(PaymentMethod.UPI);
        expense.setMerchantName(transactionInfo.getMerchantName());
        expense.setReferenceNumber(transactionInfo.getTransactionId());
        expense.setNotes(transactionInfo.getNotes());
        expense.setCurrencyCode("INR");
        expense.setIsBusinessExpense(false);
        expense.setIsReimbursable(false);
        expense.setReimbursed(false);
        
        // Try to determine category
        Category category = suggestCategory(transactionInfo, user);
        expense.setCategoryId(category.getId());
        
        return expense;
    }

    /**
     * Generate expense title from transaction info
     */
    private String generateExpenseTitle(UpiTransactionInfo transactionInfo) {
        StringBuilder title = new StringBuilder();
        
        if (transactionInfo.getUpiApp() != null) {
            title.append(transactionInfo.getUpiApp()).append(" - ");
        }
        
        if (transactionInfo.getMerchantName() != null && !transactionInfo.getMerchantName().isEmpty()) {
            title.append(transactionInfo.getMerchantName());
        } else {
            title.append("UPI Transaction");
        }
        
        return title.toString();
    }

    /**
     * Generate expense description from transaction info
     */
    private String generateExpenseDescription(UpiTransactionInfo transactionInfo) {
        StringBuilder description = new StringBuilder();
        
        description.append("UPI Transaction");
        
        if (transactionInfo.getUpiApp() != null) {
            description.append(" via ").append(transactionInfo.getUpiApp());
        }
        
        if (transactionInfo.getUpiId() != null) {
            description.append(" to ").append(transactionInfo.getUpiId());
        }
        
        if (transactionInfo.getTransactionId() != null) {
            description.append(" (Ref: ").append(transactionInfo.getTransactionId()).append(")");
        }
        
        return description.toString();
    }

    /**
     * Suggest category for the transaction - FIXED METHOD CALLS
     */
    private Category suggestCategory(UpiTransactionInfo transactionInfo, User user) {
        // Try AI-based category suggestion
        try {
            String context = (transactionInfo.getMerchantName() != null ? transactionInfo.getMerchantName() : "") + 
                           " " + (transactionInfo.getNotes() != null ? transactionInfo.getNotes() : "");
            
            // Use the correct method signature
            String suggestedCategoryName = categorySuggestionEngine.suggestCategory(
                context, 
                transactionInfo.getMerchantName(), 
                transactionInfo.getAmount()
            );
            
            if (suggestedCategoryName != null) {
                // FIXED: Use createdBy instead of userId
                Optional<Category> category = categoryRepository.findByNameAndCreatedBy(suggestedCategoryName, user.getId());
                if (category.isPresent()) {
                    return category.get();
                }
                
                // Try system category
                Optional<Category> systemCategory = categoryRepository.findByNameAndCreatedByIsNull(suggestedCategoryName);
                if (systemCategory.isPresent()) {
                    return systemCategory.get();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get AI category suggestion: {}", e.getMessage());
        }
        
        // Fallback to default category
        return getDefaultCategory(user);
    }

    /**
     * Get default category for user - FIXED METHOD CALLS
     */
    private Category getDefaultCategory(User user) {
        // Try user's "Others" category
        Optional<Category> userOthers = categoryRepository.findByNameAndCreatedBy("Others", user.getId());
        if (userOthers.isPresent()) {
            return userOthers.get();
        }
        
        // Try system "Others" category
        Optional<Category> systemOthers = categoryRepository.findByNameAndCreatedByIsNull("Others");
        if (systemOthers.isPresent()) {
            return systemOthers.get();
        }
        
        // FIXED: Use correct method name
        List<Category> categories = categoryRepository.findByCreatedByOrCreatedByIsNull(user.getId());
        if (!categories.isEmpty()) {
            return categories.get(0);
        }
        
        throw new RuntimeException("No categories available for user");
    }

    /**
     * Data class to hold UPI transaction information
     */
    public static class UpiTransactionInfo {
        private BigDecimal amount;
        private String upiId;
        private String transactionId;
        private String merchantName;
        private LocalDateTime transactionDate;
        private String upiApp;
        private String transactionType;
        private String notes;

        // Constructors
        public UpiTransactionInfo() {}

        // Getters and Setters
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

        public LocalDateTime getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

        public String getUpiApp() { return upiApp; }
        public void setUpiApp(String upiApp) { this.upiApp = upiApp; }

        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        @Override
        public String toString() {
            return "UpiTransactionInfo{" +
                    "amount=" + amount +
                    ", upiId='" + upiId + '\'' +
                    ", transactionId='" + transactionId + '\'' +
                    ", merchantName='" + merchantName + '\'' +
                    ", transactionDate=" + transactionDate +
                    ", upiApp='" + upiApp + '\'' +
                    ", transactionType='" + transactionType + '\'' +
                    ", notes='" + notes + '\'' +
                    '}';
        }
    }
}