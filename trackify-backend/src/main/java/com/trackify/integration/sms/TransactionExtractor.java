package com.trackify.integration.sms;

import com.trackify.entity.Category;
import com.trackify.entity.Expense;
import com.trackify.entity.User;
import com.trackify.enums.ExpenseStatus;
import com.trackify.enums.PaymentMethod;
import com.trackify.integration.ai.CategorySuggestionEngine;
import com.trackify.integration.sms.SmsParser.SmsTransactionInfo;
import com.trackify.repository.CategoryRepository;
import com.trackify.repository.ExpenseRepository;
import com.trackify.repository.UserRepository;
import com.trackify.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TransactionExtractor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionExtractor.class);

    @Autowired
    private SmsParser smsParser;

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

    // Merchant to category mapping for smart categorization
    private static final Map<String, String> MERCHANT_CATEGORY_MAPPING = new HashMap<>();
    static {
        // Food & Dining
        MERCHANT_CATEGORY_MAPPING.put("swiggy", "Food & Dining");
        MERCHANT_CATEGORY_MAPPING.put("zomato", "Food & Dining");
        MERCHANT_CATEGORY_MAPPING.put("dominos", "Food & Dining");
        MERCHANT_CATEGORY_MAPPING.put("kfc", "Food & Dining");
        MERCHANT_CATEGORY_MAPPING.put("mcdonald", "Food & Dining");
        MERCHANT_CATEGORY_MAPPING.put("pizza", "Food & Dining");
        MERCHANT_CATEGORY_MAPPING.put("restaurant", "Food & Dining");
        MERCHANT_CATEGORY_MAPPING.put("cafe", "Food & Dining");
        MERCHANT_CATEGORY_MAPPING.put("starbucks", "Food & Dining");

        // Transportation
        MERCHANT_CATEGORY_MAPPING.put("uber", "Transportation");
        MERCHANT_CATEGORY_MAPPING.put("ola", "Transportation");
        MERCHANT_CATEGORY_MAPPING.put("rapido", "Transportation");
        MERCHANT_CATEGORY_MAPPING.put("metro", "Transportation");
        MERCHANT_CATEGORY_MAPPING.put("irctc", "Transportation");
        MERCHANT_CATEGORY_MAPPING.put("spicejet", "Transportation");
        MERCHANT_CATEGORY_MAPPING.put("indigo", "Transportation");
        MERCHANT_CATEGORY_MAPPING.put("petrol", "Transportation");
        MERCHANT_CATEGORY_MAPPING.put("fuel", "Transportation");
        MERCHANT_CATEGORY_MAPPING.put("gas station", "Transportation");

        // Shopping
        MERCHANT_CATEGORY_MAPPING.put("amazon", "Shopping");
        MERCHANT_CATEGORY_MAPPING.put("flipkart", "Shopping");
        MERCHANT_CATEGORY_MAPPING.put("myntra", "Shopping");
        MERCHANT_CATEGORY_MAPPING.put("ajio", "Shopping");
        MERCHANT_CATEGORY_MAPPING.put("bigbasket", "Shopping");
        MERCHANT_CATEGORY_MAPPING.put("grofers", "Shopping");
        MERCHANT_CATEGORY_MAPPING.put("blinkit", "Shopping");
        MERCHANT_CATEGORY_MAPPING.put("mall", "Shopping");
        MERCHANT_CATEGORY_MAPPING.put("store", "Shopping");

        // Entertainment
        MERCHANT_CATEGORY_MAPPING.put("netflix", "Entertainment");
        MERCHANT_CATEGORY_MAPPING.put("prime", "Entertainment");
        MERCHANT_CATEGORY_MAPPING.put("hotstar", "Entertainment");
        MERCHANT_CATEGORY_MAPPING.put("spotify", "Entertainment");
        MERCHANT_CATEGORY_MAPPING.put("youtube", "Entertainment");
        MERCHANT_CATEGORY_MAPPING.put("cinema", "Entertainment");
        MERCHANT_CATEGORY_MAPPING.put("multiplex", "Entertainment");
        MERCHANT_CATEGORY_MAPPING.put("pvr", "Entertainment");
        MERCHANT_CATEGORY_MAPPING.put("inox", "Entertainment");

        // Healthcare
        MERCHANT_CATEGORY_MAPPING.put("hospital", "Healthcare");
        MERCHANT_CATEGORY_MAPPING.put("clinic", "Healthcare");
        MERCHANT_CATEGORY_MAPPING.put("pharmacy", "Healthcare");
        MERCHANT_CATEGORY_MAPPING.put("apollo", "Healthcare");
        MERCHANT_CATEGORY_MAPPING.put("medplus", "Healthcare");
        MERCHANT_CATEGORY_MAPPING.put("doctor", "Healthcare");

        // Utilities
        MERCHANT_CATEGORY_MAPPING.put("electricity", "Utilities");
        MERCHANT_CATEGORY_MAPPING.put("water", "Utilities");
        MERCHANT_CATEGORY_MAPPING.put("gas", "Utilities");
        MERCHANT_CATEGORY_MAPPING.put("internet", "Utilities");
        MERCHANT_CATEGORY_MAPPING.put("broadband", "Utilities");
        MERCHANT_CATEGORY_MAPPING.put("mobile", "Utilities");
        MERCHANT_CATEGORY_MAPPING.put("recharge", "Utilities");
        MERCHANT_CATEGORY_MAPPING.put("airtel", "Utilities");
        MERCHANT_CATEGORY_MAPPING.put("jio", "Utilities");
        MERCHANT_CATEGORY_MAPPING.put("vodafone", "Utilities");

        // Financial Services
        MERCHANT_CATEGORY_MAPPING.put("bank", "Financial Services");
        MERCHANT_CATEGORY_MAPPING.put("loan", "Financial Services");
        MERCHANT_CATEGORY_MAPPING.put("insurance", "Financial Services");
        MERCHANT_CATEGORY_MAPPING.put("mutual fund", "Financial Services");
        MERCHANT_CATEGORY_MAPPING.put("sip", "Financial Services");
    }

    /**
     * Extract transaction from SMS and create expense
     */
    public Expense extractAndCreateExpense(String smsContent, String sender, Long userId) {
        try {
            logger.info("Extracting transaction from SMS for user: {}", userId);

            // Parse SMS to extract transaction info
            SmsTransactionInfo transactionInfo = smsParser.parseSms(smsContent, sender, LocalDateTime.now());

            if (!transactionInfo.isTransaction()) {
                logger.debug("SMS is not a transaction message");
                return null;
            }

            if (transactionInfo.getAmount() == null || transactionInfo.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Invalid or missing amount in SMS transaction");
                return null;
            }

            // Only process debit transactions for expense tracking
            if (!"DEBIT".equals(transactionInfo.getTransactionType())) {
                logger.debug("Skipping credit transaction");
                return null;
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // Check for duplicate transactions
            if (isDuplicateTransaction(transactionInfo, userId)) {
                logger.warn("Duplicate transaction detected, skipping");
                return null;
            }

            // Create expense from transaction info
            Expense expense = createExpenseFromSmsTransaction(transactionInfo, user);

            // Save expense
            expense = expenseRepository.save(expense);

            logger.info("Successfully created expense from SMS transaction: {}", expense.getId());

            // Send notification
            notificationService.notifyExpenseSubmitted(expense.getId(), userId, userId);

            return expense;

        } catch (Exception e) {
            logger.error("Error extracting transaction from SMS for user: {}", userId, e);
            throw new RuntimeException("Failed to extract transaction from SMS", e);
        }
    }

    /**
     * Process multiple SMS messages in batch
     */
    public List<Expense> processSmsMessagesBatch(List<SmsMessage> smsMessages, Long userId) {
        logger.info("Processing {} SMS messages in batch for user: {}", smsMessages.size(), userId);
        
        List<Expense> createdExpenses = new java.util.ArrayList<>();
        
        for (SmsMessage smsMessage : smsMessages) {
            try {
                Expense expense = extractAndCreateExpense(
                    smsMessage.getContent(), 
                    smsMessage.getSender(), 
                    userId
                );
                
                if (expense != null) {
                    createdExpenses.add(expense);
                }
            } catch (Exception e) {
                logger.warn("Failed to process SMS message from {}: {}", smsMessage.getSender(), e.getMessage());
                // Continue processing other messages
            }
        }
        
        logger.info("Successfully processed {} expenses from {} SMS messages", 
                   createdExpenses.size(), smsMessages.size());
        
        return createdExpenses;
    }

    /**
     * Check if transaction is duplicate
     */
    private boolean isDuplicateTransaction(SmsTransactionInfo transactionInfo, Long userId) {
        // Check for duplicate based on amount, date, and reference number
        LocalDateTime startTime = transactionInfo.getTransactionDate().minusMinutes(5);
        LocalDateTime endTime = transactionInfo.getTransactionDate().plusMinutes(5);

        List<Expense> recentExpenses = expenseRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);


        for (Expense expense : recentExpenses) {
            // Check amount match
            if (expense.getAmount().compareTo(transactionInfo.getAmount()) == 0) {
                // Check reference number if available
                if (transactionInfo.getReferenceNumber() != null && 
                    transactionInfo.getReferenceNumber().equals(expense.getReferenceNumber())) {
                    return true;
                }
                
                // Check merchant name similarity
                if (transactionInfo.getMerchantName() != null && 
                    expense.getMerchantName() != null &&
                    isSimilarMerchantName(transactionInfo.getMerchantName(), expense.getMerchantName())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if merchant names are similar
     */
    private boolean isSimilarMerchantName(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        
        String clean1 = name1.toLowerCase().trim();
        String clean2 = name2.toLowerCase().trim();
        
        // Exact match
        if (clean1.equals(clean2)) return true;
        
        // Contains match
        if (clean1.contains(clean2) || clean2.contains(clean1)) return true;
        
        // Levenshtein distance for similarity
        return calculateLevenshteinDistance(clean1, clean2) <= 3;
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private int calculateLevenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1),
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }

        return dp[str1.length()][str2.length()];
    }

    /**
     * Create expense from SMS transaction info
     */
    private Expense createExpenseFromSmsTransaction(SmsTransactionInfo transactionInfo, User user) {
        Expense expense = new Expense();

        // Basic expense details
        expense.setUserId(user.getId());
        expense.setAmount(transactionInfo.getAmount());
        expense.setExpenseDate(transactionInfo.getTransactionDate().toLocalDate());
        expense.setTitle(generateExpenseTitle(transactionInfo));
        expense.setDescription(generateExpenseDescription(transactionInfo));
        expense.setStatus(ExpenseStatus.PENDING);
        expense.setMerchantName(transactionInfo.getMerchantName());
        expense.setLocation(transactionInfo.getLocation());
        expense.setReferenceNumber(transactionInfo.getReferenceNumber());
        expense.setNotes(generateExpenseNotes(transactionInfo));
        expense.setCurrencyCode("INR");
        expense.setIsBusinessExpense(false);
        expense.setIsReimbursable(false);
        expense.setReimbursed(false);

        // Set payment method
        expense.setPaymentMethod(mapPaymentMethod(transactionInfo.getPaymentMethod()));

        // Determine category
        Category category = suggestCategoryForTransaction(transactionInfo, user);
        expense.setCategoryId(category.getId());

        return expense;
    }

    /**
     * Generate expense title from transaction info
     */
    private String generateExpenseTitle(SmsTransactionInfo transactionInfo) {
        StringBuilder title = new StringBuilder();

        if (transactionInfo.getMerchantName() != null && !transactionInfo.getMerchantName().trim().isEmpty()) {
            title.append(transactionInfo.getMerchantName());
        } else if (transactionInfo.getServiceProvider() != null) {
            title.append(transactionInfo.getServiceProvider()).append(" Transaction");
        } else if (transactionInfo.getBankName() != null) {
            title.append(transactionInfo.getBankName()).append(" Transaction");
        } else {
            title.append("SMS Transaction");
        }

        // Add payment method info
        if (transactionInfo.getPaymentMethod() != null) {
            title.append(" (").append(transactionInfo.getPaymentMethod()).append(")");
        }

        return title.toString();
    }

    /**
     * Generate expense description from transaction info
     */
    private String generateExpenseDescription(SmsTransactionInfo transactionInfo) {
        StringBuilder description = new StringBuilder();

        description.append("Transaction extracted from SMS");

        if (transactionInfo.getBankName() != null) {
            description.append(" from ").append(transactionInfo.getBankName());
        }

        if (transactionInfo.getAccountNumber() != null) {
            description.append(" (Account: ").append(maskAccountNumber(transactionInfo.getAccountNumber())).append(")");
        }

        if (transactionInfo.getReferenceNumber() != null) {
            description.append(" [Ref: ").append(transactionInfo.getReferenceNumber()).append("]");
        }

        return description.toString();
    }

    /**
     * Generate expense notes from transaction info
     */
    private String generateExpenseNotes(SmsTransactionInfo transactionInfo) {
        StringBuilder notes = new StringBuilder();

        notes.append("SMS Details:\n");
        notes.append("Sender: ").append(transactionInfo.getSender()).append("\n");
        notes.append("Received: ").append(transactionInfo.getReceivedTime()).append("\n");

        if (transactionInfo.getBalance() != null) {
            notes.append("Balance: ₹").append(transactionInfo.getBalance()).append("\n");
        }

        if (transactionInfo.getUpiId() != null) {
            notes.append("UPI ID: ").append(transactionInfo.getUpiId()).append("\n");
        }

        return notes.toString();
    }

    /**
     * Mask account number for privacy
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "****" + lastFour;
    }

    /**
     * Map SMS payment method to enum
     */
    private PaymentMethod mapPaymentMethod(String paymentMethod) {
        if (paymentMethod == null) return PaymentMethod.OTHER;

        switch (paymentMethod.toUpperCase()) {
            case "CASH":
                return PaymentMethod.CASH;
            case "CREDIT_CARD":
                return PaymentMethod.CREDIT_CARD;
            case "DEBIT_CARD":
                return PaymentMethod.DEBIT_CARD;
            case "BANK_TRANSFER":
                return PaymentMethod.BANK_TRANSFER;
            case "DIGITAL_WALLET":
                return PaymentMethod.DIGITAL_WALLET;
            case "CHECK":
                return PaymentMethod.CHECK;
            case "UPI":
                return PaymentMethod.UPI;
            case "NET_BANKING":
                return PaymentMethod.NET_BANKING;
            default:
                return PaymentMethod.OTHER;
        }
    }


    /**
     * Suggest category for transaction based on merchant and context
     */
    private Category suggestCategoryForTransaction(SmsTransactionInfo transactionInfo, User user) {
        // Try merchant-based categorization first
        if (transactionInfo.getMerchantName() != null) {
            String categoryName = getCategoryFromMerchant(transactionInfo.getMerchantName());
            if (categoryName != null) {
                // FIXED: Changed from findByNameAndUserId to findByNameAndCreatedBy
                Optional<Category> category = categoryRepository.findByNameAndCreatedBy(categoryName, user.getId());
                if (category.isPresent()) {
                    return category.get();
                }

                // FIXED: Changed from findByNameAndUserIdIsNull to findByNameAndCreatedByIsNull
                Optional<Category> systemCategory = categoryRepository.findByNameAndCreatedByIsNull(categoryName);
                if (systemCategory.isPresent()) {
                    return systemCategory.get();
                }
            }
        }

        // Try AI-based category suggestion
        try {
            String context = buildContextForAI(transactionInfo);
            Long suggestedCategoryId = categorySuggestionEngine.suggestCategory(context, user.getId());

            if (suggestedCategoryId != null) {
                Optional<Category> category = categoryRepository.findById(suggestedCategoryId);
                if (category.isPresent()) {
                    return category.get();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get AI category suggestion: {}", e.getMessage());
        }

        // Fallback to default category
        return getDefaultCategory(user);
    }
    /**
     * Get category from merchant name using predefined mapping
     */
    private String getCategoryFromMerchant(String merchantName) {
        if (merchantName == null) return null;
        
        String lowerMerchant = merchantName.toLowerCase();
        
        for (Map.Entry<String, String> entry : MERCHANT_CATEGORY_MAPPING.entrySet()) {
            if (lowerMerchant.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    /**
     * Build context string for AI categorization
     */
    private String buildContextForAI(SmsTransactionInfo transactionInfo) {
        StringBuilder context = new StringBuilder();
        
        if (transactionInfo.getMerchantName() != null) {
            context.append("Merchant: ").append(transactionInfo.getMerchantName()).append(" ");
        }
        
        if (transactionInfo.getLocation() != null) {
            context.append("Location: ").append(transactionInfo.getLocation()).append(" ");
        }
        
        if (transactionInfo.getPaymentMethod() != null) {
            context.append("Payment: ").append(transactionInfo.getPaymentMethod()).append(" ");
        }
        
        if (transactionInfo.getServiceProvider() != null) {
            context.append("Service: ").append(transactionInfo.getServiceProvider()).append(" ");
        }
        
        return context.toString().trim();
    }

    /**
     * Get default category for user
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
     * Extract transactions from bank statement content
     */
    public List<Expense> extractFromBankStatement(String statementContent, Long userId) {
        logger.info("Extracting transactions from bank statement for user: {}", userId);
        
        List<Expense> expenses = new java.util.ArrayList<>();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Split statement into lines
        String[] lines = statementContent.split("\\r?\\n");
        
        for (String line : lines) {
            try {
                // Try to parse each line as a transaction
                SmsTransactionInfo transactionInfo = parseStatementLine(line);
                
                if (transactionInfo != null && transactionInfo.isTransaction() && 
                    "DEBIT".equals(transactionInfo.getTransactionType())) {
                    
                    Expense expense = createExpenseFromSmsTransaction(transactionInfo, user);
                    expenses.add(expense);
                }
            } catch (Exception e) {
                logger.debug("Failed to parse statement line: {}", line);
                // Continue with next line
            }
        }
        
        // Save all expenses
        if (!expenses.isEmpty()) {
            expenses = expenseRepository.saveAll(expenses);
            logger.info("Created {} expenses from bank statement", expenses.size());
        }
        
        return expenses;
    }

    /**
     * Parse a single line from bank statement
     */
    private SmsTransactionInfo parseStatementLine(String line) {
        // This is a simplified parser for common bank statement formats
        // You would need to customize this based on your bank's statement format
        
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        // Example format: "01/12/2023  DEBIT  500.00  UPI-SWIGGY-REF123456789  Balance: 10000.00"
        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            return null;
        }
        
        try {
            SmsTransactionInfo info = new SmsTransactionInfo();
            
            // Parse date (assuming first part is date)
            info.setTransactionDate(parseStatementDate(parts[0]));
            
            // Parse transaction type
            String type = parts[1].toUpperCase();
            if (type.contains("DEBIT") || type.contains("DR")) {
                info.setTransactionType("DEBIT");
                info.setIsTransaction(true);
            } else if (type.contains("CREDIT") || type.contains("CR")) {
                info.setTransactionType("CREDIT");
                info.setIsTransaction(true);
            } else {
                return null;
            }
            
            // Parse amount
            for (int i = 2; i < parts.length; i++) {
                try {
                    BigDecimal amount = new BigDecimal(parts[i].replaceAll(",", ""));
                    info.setAmount(amount);
                    break;
                } catch (NumberFormatException ignored) {
                    // Continue looking for amount
                }
            }
            
            // Extract merchant/description from remaining parts
            StringBuilder description = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (!parts[i].contains("Balance:")) {
                    description.append(parts[i]).append(" ");
                }
            }
            
            String desc = description.toString().trim();
            if (!desc.isEmpty()) {
                info.setMerchantName(extractMerchantFromDescription(desc));
                info.setDescription(desc);
            }
            
            return info;
            
        } catch (Exception e) {
            logger.debug("Failed to parse statement line: {}", line);
            return null;
        }
    }

    /**
     * Parse date from statement line
     */
    private LocalDateTime parseStatementDate(String dateStr) {
        try {
            // Handle various date formats
            dateStr = dateStr.replaceAll("[-/]", "/");
            String[] parts = dateStr.split("/");
            
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                
                if (year < 100) {
                    year += 2000;
                }
                
                return LocalDateTime.of(year, month, day, 0, 0);
            }
        } catch (Exception e) {
            logger.debug("Failed to parse statement date: {}", dateStr);
        }
        
        return LocalDateTime.now();
    }

    /**
     * Extract merchant name from transaction description
     */
    private String extractMerchantFromDescription(String description) {
        if (description == null) return null;
        
        // Remove common prefixes
        description = description.replaceAll("(?i)^(UPI-|POS-|ATM-|NEFT-|IMPS-)", "");
        
        // Extract meaningful part before reference numbers
        String[] parts = description.split("-");
        if (parts.length > 1) {
            String merchant = parts[1].trim();
            // Remove reference numbers and keep only merchant name
            merchant = merchant.replaceAll("\\b[A-Z0-9]{10,}\\b", "").trim();
            if (!merchant.isEmpty()) {
                return merchant.length() > 100 ? merchant.substring(0, 100) : merchant;
            }
        }
        
        // Fallback to first meaningful part
        String cleaned = description.replaceAll("\\b[A-Z0-9]{10,}\\b", "").trim();
        return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
    }

    /**
     * Data class for SMS message
     */
    public static class SmsMessage {
        private String content;
        private String sender;
        private LocalDateTime receivedTime;

        public SmsMessage() {}

        public SmsMessage(String content, String sender, LocalDateTime receivedTime) {
            this.content = content;
            this.sender = sender;
            this.receivedTime = receivedTime;
        }

        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }

        public LocalDateTime getReceivedTime() { return receivedTime; }
        public void setReceivedTime(LocalDateTime receivedTime) { this.receivedTime = receivedTime; }
    }
}