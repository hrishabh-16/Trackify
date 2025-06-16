package com.trackify.integration.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SmsParser {

    private static final Logger logger = LoggerFactory.getLogger(SmsParser.class);

    // Bank sender patterns
    private static final Map<String, String> BANK_SENDERS = new HashMap<>();
    static {
        BANK_SENDERS.put("SBIINB", "State Bank of India");
        BANK_SENDERS.put("HDFCBK", "HDFC Bank");
        BANK_SENDERS.put("ICICIB", "ICICI Bank");
        BANK_SENDERS.put("AXISBK", "Axis Bank");
        BANK_SENDERS.put("KOTAKB", "Kotak Mahindra Bank");
        BANK_SENDERS.put("PNBSMS", "Punjab National Bank");
        BANK_SENDERS.put("BOBSMS", "Bank of Baroda");
        BANK_SENDERS.put("CBSSMS", "Canara Bank");
        BANK_SENDERS.put("UNINSM", "Union Bank");
        BANK_SENDERS.put("IOBSMS", "Indian Overseas Bank");
        BANK_SENDERS.put("SBICAR", "SBI Card");
        BANK_SENDERS.put("HDFCCC", "HDFC Credit Card");
        BANK_SENDERS.put("ICICIC", "ICICI Credit Card");
        BANK_SENDERS.put("AXISBK", "Axis Credit Card");
    }

    // UPI App senders
    private static final Map<String, String> UPI_SENDERS = new HashMap<>();
    static {
        UPI_SENDERS.put("PAYTM", "Paytm");
        UPI_SENDERS.put("PHONEPE", "PhonePe");
        UPI_SENDERS.put("GPAY", "Google Pay");
        UPI_SENDERS.put("AMAZONP", "Amazon Pay");
        UPI_SENDERS.put("BHIMUPI", "BHIM UPI");
        UPI_SENDERS.put("MOBIKW", "MobiKwik");
        UPI_SENDERS.put("AIRTEL", "Airtel Money");
        UPI_SENDERS.put("JIOMON", "JioMoney");
    }

    // Transaction type patterns
    private static final Pattern DEBIT_PATTERN = Pattern.compile(
        "(?:debited|deducted|spent|paid|withdrawn|purchase|buy|bought|transaction|txn)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CREDIT_PATTERN = Pattern.compile(
        "(?:credited|received|deposit|refund|cashback|reward|salary|transfer in)",
        Pattern.CASE_INSENSITIVE
    );

    // Amount patterns - multiple variations
    private static final Pattern[] AMOUNT_PATTERNS = {
        Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|₹)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("amount\\s*:?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:has been|is|was)\\s*(?:debited|credited)", Pattern.CASE_INSENSITIVE)
    };

    // Account number patterns
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
        "(?:a/c|account|ac)\\s*(?:no\\.?|number)?\\s*(?:ending|ending with|xxxx)?\\s*([0-9x*]{4,})",
        Pattern.CASE_INSENSITIVE
    );

    // Card number patterns
    private static final Pattern CARD_PATTERN = Pattern.compile(
        "(?:card|cc)\\s*(?:no\\.?|number)?\\s*(?:ending|ending with|xxxx)?\\s*([0-9x*]{4,})",
        Pattern.CASE_INSENSITIVE
    );

    // UPI ID patterns
    private static final Pattern UPI_ID_PATTERN = Pattern.compile(
        "([\\w.-]+@[\\w-]+)",
        Pattern.CASE_INSENSITIVE
    );

    // Merchant patterns
    private static final Pattern MERCHANT_PATTERNS[] = {
        Pattern.compile("(?:at|to|from|merchant)\\s+([a-z0-9\\s&.-]{3,50})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:paid to|received from)\\s+([a-z0-9\\s&.-]{3,50})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:transaction at|txn at)\\s+([a-z0-9\\s&.-]{3,50})", Pattern.CASE_INSENSITIVE)
    };

    // Reference number patterns
    private static final Pattern REF_PATTERNS[] = {
        Pattern.compile("(?:ref|reference|txn|transaction|utr)\\s*(?:no\\.?|number|id)?\\s*:?\\s*([a-z0-9]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:rrn|retrieval reference number)\\s*:?\\s*([a-z0-9]+)", Pattern.CASE_INSENSITIVE)
    };

    // Date and time patterns
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile(
        "(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})\\s*(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*(?:am|pm))?)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})"
    );

    // Balance patterns
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
        "(?:balance|bal|available balance)\\s*:?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Parse SMS message and extract transaction information
     */
    public SmsTransactionInfo parseSms(String smsContent, String sender, LocalDateTime receivedTime) {
        logger.info("Parsing SMS from sender: {}", sender);
        
        SmsTransactionInfo transactionInfo = new SmsTransactionInfo();
        
        if (smsContent == null || smsContent.trim().isEmpty()) {
            logger.warn("Empty SMS content received");
            return transactionInfo;
        }

        String normalizedContent = smsContent.trim();
        
        // Set basic info
        transactionInfo.setSender(sender);
        transactionInfo.setReceivedTime(receivedTime);
        transactionInfo.setOriginalMessage(normalizedContent);
        
        // Identify bank/service provider
        transactionInfo.setBankName(identifyBank(sender));
        transactionInfo.setServiceProvider(identifyServiceProvider(sender));
        
        // Check if this is a transaction SMS
        if (!isTransactionSms(normalizedContent)) {
            logger.debug("SMS does not appear to be a transaction message");
            transactionInfo.setIsTransaction(false);
            return transactionInfo;
        }
        
        transactionInfo.setIsTransaction(true);
        
        // Extract transaction details
        transactionInfo.setAmount(extractAmount(normalizedContent));
        transactionInfo.setTransactionType(extractTransactionType(normalizedContent));
        transactionInfo.setAccountNumber(extractAccountNumber(normalizedContent));
        transactionInfo.setCardNumber(extractCardNumber(normalizedContent));
        transactionInfo.setMerchantName(extractMerchantName(normalizedContent));
        transactionInfo.setReferenceNumber(extractReferenceNumber(normalizedContent));
        transactionInfo.setTransactionDate(extractTransactionDate(normalizedContent, receivedTime));
        transactionInfo.setBalance(extractBalance(normalizedContent));
        transactionInfo.setUpiId(extractUpiId(normalizedContent));
        transactionInfo.setPaymentMethod(determinePaymentMethod(normalizedContent, sender));
        
        // Extract additional information
        transactionInfo.setLocation(extractLocation(normalizedContent));
        transactionInfo.setDescription(extractDescription(normalizedContent));
        
        logger.info("Successfully parsed SMS transaction: Amount={}, Type={}, Merchant={}", 
                transactionInfo.getAmount(), transactionInfo.getTransactionType(), transactionInfo.getMerchantName());
        
        return transactionInfo;
    }

    /**
     * Check if SMS is a transaction message
     */
    private boolean isTransactionSms(String content) {
        String lowerContent = content.toLowerCase();
        
        // Transaction keywords
        String[] transactionKeywords = {
            "debited", "credited", "paid", "received", "withdrawn", "deposit",
            "transaction", "txn", "purchase", "refund", "cashback", "transfer",
            "amount", "balance", "upi", "card", "account"
        };
        
        for (String keyword : transactionKeywords) {
            if (lowerContent.contains(keyword)) {
                return true;
            }
        }
        
        // Check for amount patterns
        for (Pattern pattern : AMOUNT_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Identify bank from sender ID
     */
    private String identifyBank(String sender) {
        if (sender == null) return null;
        
        String upperSender = sender.toUpperCase();
        
        // Check exact matches first
        if (BANK_SENDERS.containsKey(upperSender)) {
            return BANK_SENDERS.get(upperSender);
        }
        
        // Check partial matches
        for (Map.Entry<String, String> entry : BANK_SENDERS.entrySet()) {
            if (upperSender.contains(entry.getKey()) || entry.getKey().contains(upperSender)) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    /**
     * Identify service provider from sender ID
     */
    private String identifyServiceProvider(String sender) {
        if (sender == null) return null;
        
        String upperSender = sender.toUpperCase();
        
        // Check UPI apps
        for (Map.Entry<String, String> entry : UPI_SENDERS.entrySet()) {
            if (upperSender.contains(entry.getKey()) || entry.getKey().contains(upperSender)) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    /**
     * Extract amount from SMS content
     */
    private BigDecimal extractAmount(String content) {
        for (Pattern pattern : AMOUNT_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                try {
                    String amountStr = matcher.group(1).replaceAll(",", "");
                    return new BigDecimal(amountStr);
                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse amount: {}", matcher.group(1));
                }
            }
        }
        return null;
    }

    /**
     * Extract transaction type (DEBIT/CREDIT)
     */
    private String extractTransactionType(String content) {
        if (DEBIT_PATTERN.matcher(content).find()) {
            return "DEBIT";
        } else if (CREDIT_PATTERN.matcher(content).find()) {
            return "CREDIT";
        }
        return "DEBIT"; // Default assumption for expense tracking
    }

    /**
     * Extract account number from SMS
     */
    private String extractAccountNumber(String content) {
        Matcher matcher = ACCOUNT_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract card number from SMS
     */
    private String extractCardNumber(String content) {
        Matcher matcher = CARD_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract UPI ID from SMS
     */
    private String extractUpiId(String content) {
        Matcher matcher = UPI_ID_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract merchant name from SMS
     */
    private String extractMerchantName(String content) {
        for (Pattern pattern : MERCHANT_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String merchant = matcher.group(1).trim();
                // Clean up merchant name
                merchant = cleanMerchantName(merchant);
                if (merchant.length() > 3) { // Minimum length check
                    return merchant;
                }
            }
        }
        return null;
    }

    /**
     * Clean up merchant name
     */
    private String cleanMerchantName(String merchantName) {
        if (merchantName == null) return null;
        
        // Remove common unwanted patterns
        merchantName = merchantName.replaceAll("(?i)\\b(pvt ltd|private limited|ltd|inc|corp|company)\\b", "");
        merchantName = merchantName.replaceAll("\\s+", " ").trim();
        
        // Remove trailing punctuation
        merchantName = merchantName.replaceAll("[.,;:!]+$", "");
        
        return merchantName.length() > 100 ? merchantName.substring(0, 100) : merchantName;
    }

    /**
     * Extract reference number from SMS
     */
    private String extractReferenceNumber(String content) {
        for (Pattern pattern : REF_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * Extract transaction date from SMS
     */
    private LocalDateTime extractTransactionDate(String content, LocalDateTime receivedTime) {
        // Try to extract date and time together
        Matcher dateTimeMatcher = DATE_TIME_PATTERN.matcher(content);
        if (dateTimeMatcher.find()) {
            try {
                String dateStr = dateTimeMatcher.group(1);
                String timeStr = dateTimeMatcher.group(2);
                return parseDateTimeString(dateStr, timeStr);
            } catch (Exception e) {
                logger.warn("Failed to parse date-time from SMS: {}", e.getMessage());
            }
        }
        
        // Try to extract date only
        Matcher dateMatcher = DATE_PATTERN.matcher(content);
        if (dateMatcher.find()) {
            try {
                String dateStr = dateMatcher.group(1);
                return parseDateString(dateStr, receivedTime);
            } catch (Exception e) {
                logger.warn("Failed to parse date from SMS: {}", e.getMessage());
            }
        }
        
        // Default to received time
        return receivedTime;
    }

    /**
     * Parse date and time string
     */
    private LocalDateTime parseDateTimeString(String dateStr, String timeStr) {
        // Normalize date format
        dateStr = dateStr.replaceAll("[-/]", "/");
        timeStr = timeStr.trim().toLowerCase();
        
        // Handle AM/PM
        boolean isPM = timeStr.contains("pm");
        timeStr = timeStr.replaceAll("\\s*(am|pm)\\s*", "");
        
        String[] dateParts = dateStr.split("/");
        String[] timeParts = timeStr.split(":");
        
        int day, month, year;
        if (dateParts[2].length() == 4) { // yyyy/mm/dd format
            year = Integer.parseInt(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(dateParts[2]);
        } else { // dd/mm/yyyy or dd/mm/yy format
            day = Integer.parseInt(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            year = Integer.parseInt(dateParts[2]);
            if (year < 100) year += 2000; // Convert 2-digit year
        }
        
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        // Convert to 24-hour format
        if (isPM && hour != 12) {
            hour += 12;
        } else if (!isPM && hour == 12) {
            hour = 0;
        }
        
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    /**
     * Parse date string with default time
     */
    private LocalDateTime parseDateString(String dateStr, LocalDateTime defaultTime) {
        dateStr = dateStr.replaceAll("[-/]", "/");
        String[] dateParts = dateStr.split("/");
        
        int day, month, year;
        if (dateParts[2].length() == 4) { // yyyy/mm/dd format
            year = Integer.parseInt(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            day = Integer.parseInt(dateParts[2]);
        } else { // dd/mm/yyyy or dd/mm/yy format
            day = Integer.parseInt(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            year = Integer.parseInt(dateParts[2]);
            if (year < 100) year += 2000; // Convert 2-digit year
        }
        
        return LocalDateTime.of(year, month, day, defaultTime.getHour(), defaultTime.getMinute());
    }

    /**
     * Extract balance from SMS
     */
    private BigDecimal extractBalance(String content) {
        Matcher matcher = BALANCE_PATTERN.matcher(content);
        if (matcher.find()) {
            try {
                String balanceStr = matcher.group(1).replaceAll(",", "");
                return new BigDecimal(balanceStr);
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse balance: {}", matcher.group(1));
            }
        }
        return null;
    }

    /**
     * Determine payment method based on SMS content and sender
     */
    private String determinePaymentMethod(String content, String sender) {
        String lowerContent = content.toLowerCase();
        
        // Check for UPI indicators
        if (lowerContent.contains("upi") || extractUpiId(content) != null || 
            UPI_SENDERS.values().stream().anyMatch(app -> sender.toUpperCase().contains(app.toUpperCase()))) {
            return "UPI";
        }
        
        // Check for card indicators
        if (lowerContent.contains("card") || lowerContent.contains("pos") || 
            lowerContent.contains("atm") || extractCardNumber(content) != null) {
            if (lowerContent.contains("credit card") || lowerContent.contains("cc")) {
                return "CREDIT_CARD";
            } else {
                return "DEBIT_CARD";
            }
        }
        
        // Check for net banking
        if (lowerContent.contains("netbanking") || lowerContent.contains("net banking") ||
            lowerContent.contains("online transfer") || lowerContent.contains("imps") ||
            lowerContent.contains("neft") || lowerContent.contains("rtgs")) {
            return "NET_BANKING";
        }
        
        // Check for wallet
        if (lowerContent.contains("wallet") || lowerContent.contains("prepaid")) {
            return "WALLET";
        }
        
        return "OTHER";
    }

    /**
     * Extract location from SMS content
     */
    private String extractLocation(String content) {
        // Look for location patterns
        Pattern locationPattern = Pattern.compile(
            "(?:at|location|branch)\\s+([a-z0-9\\s,.-]{5,50})",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = locationPattern.matcher(content);
        if (matcher.find()) {
            String location = matcher.group(1).trim();
            return location.length() > 100 ? location.substring(0, 100) : location;
        }
        
        return null;
    }

    /**
     * Extract description from SMS content
     */
    private String extractDescription(String content) {
        // Remove sensitive information and return cleaned content
        String description = content;
        
        // Remove account numbers
        description = description.replaceAll("(?i)(?:a/c|account)\\s*(?:no\\.?)?\\s*[0-9x*]+", "[ACCOUNT]");
        
        // Remove card numbers
        description = description.replaceAll("(?i)(?:card)\\s*(?:no\\.?)?\\s*[0-9x*]+", "[CARD]");
        
        // Remove phone numbers
        description = description.replaceAll("\\b[0-9]{10}\\b", "[PHONE]");
        
        // Limit length
        return description.length() > 500 ? description.substring(0, 500) + "..." : description;
    }

    /**
     * Data class to hold SMS transaction information
     */
    public static class SmsTransactionInfo {
        private String sender;
        private LocalDateTime receivedTime;
        private String originalMessage;
        private boolean isTransaction;
        private String bankName;
        private String serviceProvider;
        private BigDecimal amount;
        private String transactionType;
        private String accountNumber;
        private String cardNumber;
        private String upiId;
        private String merchantName;
        private String referenceNumber;
        private LocalDateTime transactionDate;
        private BigDecimal balance;
        private String paymentMethod;
        private String location;
        private String description;

        // Constructors
        public SmsTransactionInfo() {}

        // Getters and Setters
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }

        public LocalDateTime getReceivedTime() { return receivedTime; }
        public void setReceivedTime(LocalDateTime receivedTime) { this.receivedTime = receivedTime; }

        public String getOriginalMessage() { return originalMessage; }
        public void setOriginalMessage(String originalMessage) { this.originalMessage = originalMessage; }

        public boolean isTransaction() { return isTransaction; }
        public void setIsTransaction(boolean isTransaction) { this.isTransaction = isTransaction; }

        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }

        public String getServiceProvider() { return serviceProvider; }
        public void setServiceProvider(String serviceProvider) { this.serviceProvider = serviceProvider; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }

        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

        public LocalDateTime getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        @Override
        public String toString() {
            return "SmsTransactionInfo{" +
                    "sender='" + sender + '\'' +
                    ", amount=" + amount +
                    ", transactionType='" + transactionType + '\'' +
                    ", merchantName='" + merchantName + '\'' +
                    ", paymentMethod='" + paymentMethod + '\'' +
                    ", transactionDate=" + transactionDate +
                    ", isTransaction=" + isTransaction +
                    '}';
        }
    }
}