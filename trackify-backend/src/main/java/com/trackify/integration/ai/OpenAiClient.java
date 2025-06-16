package com.trackify.integration.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class OpenAiClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiClient.class);
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1";
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    @Value("${openai.model:gpt-3.5-turbo}")
    private String defaultModel;
    
    @Value("${openai.max.tokens:1000}")
    private Integer maxTokens;
    
    @Value("${openai.temperature:0.7}")
    private Double temperature;
    
    private final RestTemplate restTemplate;
    
    public OpenAiClient() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Generate text using OpenAI's chat completion API
     */
    public String generateText(String prompt, Map<String, Object> context) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                logger.warn("OpenAI API key not configured");
                throw new IllegalStateException("OpenAI API key not configured");
            }
            
            Map<String, Object> requestBody = buildChatCompletionRequest(prompt, context);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    OPENAI_API_URL + CHAT_COMPLETIONS_ENDPOINT,
                    request,
                    Map.class
            );
            
            return extractTextFromResponse(response.getBody());
            
        } catch (Exception e) {
            logger.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to generate AI response", e);
        }
    }
    
    /**
     * Query OpenAI with custom parameters
     */
    public Map<String, Object> query(String prompt, Map<String, Object> context) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                logger.warn("OpenAI API key not configured");
                throw new IllegalStateException("OpenAI API key not configured");
            }
            
            Map<String, Object> requestBody = buildChatCompletionRequest(prompt, context);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    OPENAI_API_URL + CHAT_COMPLETIONS_ENDPOINT,
                    request,
                    Map.class
            );
            
            return processQueryResponse(response.getBody(), context);
            
        } catch (Exception e) {
            logger.error("Error querying OpenAI API", e);
            throw new RuntimeException("Failed to query AI service", e);
        }
    }
    
    /**
     * Generate category suggestions for expenses
     */
    public String suggestExpenseCategory(String description, String merchantName, Double amount) {
        try {
            String prompt = buildCategorySuggestionPrompt(description, merchantName, amount);
            return generateText(prompt, Map.of("type", "category_suggestion"));
            
        } catch (Exception e) {
            logger.error("Error suggesting expense category", e);
            throw new RuntimeException("Failed to suggest expense category", e);
        }
    }
    
    /**
     * Analyze spending patterns and provide insights
     */
    public String generateSpendingInsights(Map<String, Object> spendingData) {
        try {
            String prompt = buildSpendingInsightsPrompt(spendingData);
            return generateText(prompt, Map.of("type", "spending_insights"));
            
        } catch (Exception e) {
            logger.error("Error generating spending insights", e);
            throw new RuntimeException("Failed to generate spending insights", e);
        }
    }
    
    /**
     * Generate budget recommendations
     */
    public List<String> generateBudgetRecommendations(Map<String, Object> budgetData) {
        try {
            String prompt = buildBudgetRecommendationPrompt(budgetData);
            String response = generateText(prompt, Map.of("type", "budget_recommendations"));
            
            return Arrays.asList(response.split("\n"))
                    .stream()
                    .filter(line -> !line.trim().isEmpty())
                    .map(line -> line.replaceAll("^[0-9]+\\.\\s*", ""))
                    .toList();
            
        } catch (Exception e) {
            logger.error("Error generating budget recommendations", e);
            throw new RuntimeException("Failed to generate budget recommendations", e);
        }
    }
    
    /**
     * Analyze receipt text and extract information
     */
    public Map<String, String> analyzeReceiptText(String receiptText) {
        try {
            String prompt = buildReceiptAnalysisPrompt(receiptText);
            String response = generateText(prompt, Map.of("type", "receipt_analysis"));
            
            return parseReceiptAnalysisResponse(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing receipt text", e);
            throw new RuntimeException("Failed to analyze receipt", e);
        }
    }
    
    /**
     * Generate expense policy compliance report
     */
    public String generateComplianceReport(Map<String, Object> complianceData) {
        try {
            String prompt = buildComplianceReportPrompt(complianceData);
            return generateText(prompt, Map.of("type", "compliance_report"));
            
        } catch (Exception e) {
            logger.error("Error generating compliance report", e);
            throw new RuntimeException("Failed to generate compliance report", e);
        }
    }
    
    /**
     * Generate smart notification message
     */
    public String generateNotificationMessage(String notificationType, Map<String, Object> context) {
        try {
            String prompt = buildNotificationPrompt(notificationType, context);
            return generateText(prompt, Map.of("type", "notification", "notificationType", notificationType));
            
        } catch (Exception e) {
            logger.error("Error generating notification message", e);
            throw new RuntimeException("Failed to generate notification message", e);
        }
    }
    
    // Private helper methods
    
    private Map<String, Object> buildChatCompletionRequest(String prompt, Map<String, Object> context) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", defaultModel);
        request.put("max_tokens", maxTokens);
        request.put("temperature", temperature);
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        // System message for context
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", buildSystemPrompt(context));
        messages.add(systemMessage);
        
        // User message
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        
        request.put("messages", messages);
        
        return request;
    }
    
    private String buildSystemPrompt(Map<String, Object> context) {
        String type = (String) context.getOrDefault("type", "general");
        
        switch (type) {
            case "category_suggestion":
                return "You are an AI assistant specialized in categorizing business expenses. " +
                       "Provide accurate and consistent category suggestions based on expense descriptions and merchant names. " +
                       "Common categories include: Travel, Meals, Office Supplies, Software, Marketing, " +
                       "Professional Services, Utilities, Equipment, Training, Entertainment, Transportation, Healthcare.";
                       
            case "spending_insights":
                return "You are a financial advisor AI that analyzes spending patterns and provides " +
                       "actionable insights to help users manage their finances better. Focus on practical " +
                       "recommendations and identify spending trends, potential savings opportunities, and budget optimization suggestions.";
                       
            case "budget_recommendations":
                return "You are a budget optimization expert. Provide practical and achievable " +
                       "budget recommendations based on spending history and financial goals. " +
                       "Focus on actionable advice that users can implement immediately.";
                       
            case "receipt_analysis":
                return "You are an AI specialized in analyzing receipt text and extracting structured " +
                       "information like merchant name, amount, date, and items. Be precise and accurate " +
                       "in your extractions.";
                       
            case "compliance_report":
                return "You are a compliance specialist that analyzes expense data and generates " +
                       "reports on policy adherence and potential violations. Identify specific issues " +
                       "and provide clear recommendations for compliance improvement.";
                       
            case "notification":
                return "You are an AI that generates personalized, concise, and actionable notification " +
                       "messages for expense management scenarios. Keep messages under 100 characters " +
                       "and make them actionable.";
                       
            default:
                return "You are a helpful AI assistant for expense management and financial analysis. " +
                       "Provide accurate, practical, and actionable advice.";
        }
    }
    
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting text from OpenAI response", e);
        }
        
        throw new RuntimeException("Unable to process AI response");
    }
    
    private Map<String, Object> processQueryResponse(Map<String, Object> response, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<>();
        
        String content = extractTextFromResponse(response);
        result.put("content", content);
        result.put("success", true);
        
        // Add usage information if available
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage != null) {
            result.put("usage", usage);
        }
        
        return result;
    }
    
    private String buildCategorySuggestionPrompt(String description, String merchantName, Double amount) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Suggest the most appropriate expense category for the following expense:\n\n");
        prompt.append("Description: ").append(description != null ? description : "N/A").append("\n");
        prompt.append("Merchant: ").append(merchantName != null ? merchantName : "N/A").append("\n");
        prompt.append("Amount: $").append(amount != null ? amount : "N/A").append("\n\n");
        prompt.append("Respond with only the category name (no explanations):");
        
        return prompt.toString();
    }
    
    private String buildSpendingInsightsPrompt(Map<String, Object> spendingData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following spending data and provide actionable insights:\n\n");
        
        for (Map.Entry<String, Object> entry : spendingData.entrySet()) {
            prompt.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        prompt.append("\nProvide 3-5 key insights and specific recommendations for better financial management:");
        
        return prompt.toString();
    }
    
    private String buildBudgetRecommendationPrompt(Map<String, Object> budgetData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following budget and spending data, provide specific budget recommendations:\n\n");
        
        for (Map.Entry<String, Object> entry : budgetData.entrySet()) {
            prompt.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        prompt.append("\nProvide 3-5 specific, actionable budget recommendations:");
        
        return prompt.toString();
    }
    
    private String buildReceiptAnalysisPrompt(String receiptText) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following receipt text and extract key information:\n\n");
        prompt.append(receiptText).append("\n\n");
        prompt.append("Extract and return the following information in this exact format:\n");
        prompt.append("Merchant: [merchant name]\n");
        prompt.append("Amount: [total amount]\n");
        prompt.append("Date: [date]\n");
        prompt.append("Items: [list of items if available]");
        
        return prompt.toString();
    }
    
    private String buildComplianceReportPrompt(Map<String, Object> complianceData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a compliance report based on the following data:\n\n");
        
        for (Map.Entry<String, Object> entry : complianceData.entrySet()) {
            prompt.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        prompt.append("\nGenerate a comprehensive compliance report highlighting any issues and specific recommendations:");
        
        return prompt.toString();
    }
    
    private String buildNotificationPrompt(String notificationType, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a concise, personalized notification message for: ").append(notificationType).append("\n\n");
        prompt.append("Context:\n");
        
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!"type".equals(entry.getKey()) && !"notificationType".equals(entry.getKey())) {
                prompt.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        prompt.append("\nGenerate a clear, actionable notification message (max 100 characters):");
        
        return prompt.toString();
    }
    
    private Map<String, String> parseReceiptAnalysisResponse(String response) {
        Map<String, String> result = new HashMap<>();
        
        String[] lines = response.split("\n");
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.startsWith("merchant:")) {
                result.put("merchant", line.substring(9).trim());
            } else if (lowerLine.startsWith("amount:")) {
                result.put("amount", line.substring(7).trim());
            } else if (lowerLine.startsWith("date:")) {
                result.put("date", line.substring(5).trim());
            } else if (lowerLine.startsWith("items:")) {
                result.put("items", line.substring(6).trim());
            }
        }
        
        return result;
    }
    
    /**
     * Test the OpenAI API connection
     */
    public boolean testConnection() {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                logger.warn("OpenAI API key not configured");
                return false;
            }
            
            String testPrompt = "Hello, this is a connection test. Please respond with 'OK'.";
            String response = generateText(testPrompt, Map.of("type", "test"));
            
            return response != null && !response.trim().isEmpty();
            
        } catch (Exception e) {
            logger.error("OpenAI connection test failed", e);
            return false;
        }
    }
    
    /**
     * Get API usage statistics
     */
    public Map<String, Object> getUsageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("apiKeyConfigured", apiKey != null && !apiKey.isEmpty());
        stats.put("defaultModel", defaultModel);
        stats.put("maxTokens", maxTokens);
        stats.put("temperature", temperature);
        stats.put("apiUrl", OPENAI_API_URL);
        return stats;
    }
    
    /**
     * Update configuration
     */
    public void updateConfiguration(String model, Integer tokens, Double temp) {
        if (model != null && !model.trim().isEmpty()) {
            this.defaultModel = model;
        }
        if (tokens != null && tokens > 0) {
            this.maxTokens = tokens;
        }
        if (temp != null && temp >= 0.0 && temp <= 2.0) {
            this.temperature = temp;
        }
        
        logger.info("Updated OpenAI configuration: model={}, maxTokens={}, temperature={}", 
                defaultModel, maxTokens, temperature);
    }
    
    /**
     * Validate API key format
     */
    public boolean isApiKeyValid() {
        return apiKey != null && 
               !apiKey.trim().isEmpty() && 
               apiKey.startsWith("sk-") && 
               apiKey.length() > 20;
    }
    
    /**
     * Get available models
     */
    public List<String> getAvailableModels() {
        return Arrays.asList(
                "gpt-4",
                "gpt-4-turbo-preview",
                "gpt-3.5-turbo",
                "gpt-3.5-turbo-16k"
        );
    }
    
    /**
     * Estimate token count for a prompt
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Rough estimation: 1 token ≈ 4 characters for English text
        return (int) Math.ceil(text.length() / 4.0);
    }
    
    /**
     * Build a detailed prompt with context
     */
    public String buildDetailedPrompt(String basePrompt, Map<String, Object> context, List<String> examples) {
        StringBuilder prompt = new StringBuilder();
        
        // Add context
        if (context != null && !context.isEmpty()) {
            prompt.append("Context:\n");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            prompt.append("\n");
        }
        
        // Add examples if provided
        if (examples != null && !examples.isEmpty()) {
            prompt.append("Examples:\n");
            for (String example : examples) {
                prompt.append("- ").append(example).append("\n");
            }
            prompt.append("\n");
        }
        
        // Add the main prompt
        prompt.append("Task: ").append(basePrompt);
        
        return prompt.toString();
    }
    
    /**
     * Generate multiple responses and return the best one
     */
    public String generateBestResponse(String prompt, Map<String, Object> context, int numResponses) {
        if (numResponses <= 1) {
            return generateText(prompt, context);
        }
        
        List<String> responses = new ArrayList<>();
        
        for (int i = 0; i < numResponses; i++) {
            try {
                String response = generateText(prompt, context);
                if (response != null && !response.trim().isEmpty()) {
                    responses.add(response);
                }
            } catch (Exception e) {
                logger.warn("Failed to generate response {}/{}", i + 1, numResponses, e);
            }
        }
        
        if (responses.isEmpty()) {
            throw new RuntimeException("Failed to generate any valid responses");
        }
        
        // Return the longest response as it's likely to be more comprehensive
        return responses.stream()
                .max(Comparator.comparing(String::length))
                .orElse(responses.get(0));
    }
    
    /**
     * Generate response with retry logic
     */
    public String generateTextWithRetry(String prompt, Map<String, Object> context, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return generateText(prompt, context);
            } catch (Exception e) {
                lastException = e;
                logger.warn("Attempt {}/{} failed for AI text generation", attempt, maxRetries, e);
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("Failed to generate text after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Check if the service is healthy
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean connectionTest = testConnection();
            health.put("status", connectionTest ? "healthy" : "unhealthy");
            health.put("apiKeyConfigured", isApiKeyValid());
            health.put("model", defaultModel);
            health.put("lastChecked", new Date());
            
            if (connectionTest) {
                health.put("message", "OpenAI service is operational");
            } else {
                health.put("message", "OpenAI service is not responding");
            }
            
        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
            health.put("lastChecked", new Date());
        }
        
        return health;
    }
    
    /**
     * Generate structured response for complex queries
     */
    public Map<String, Object> generateStructuredResponse(String prompt, Map<String, Object> context, String responseFormat) {
        try {
            String enhancedPrompt = prompt + "\n\nPlease format your response as " + responseFormat + ".";
            String response = generateText(enhancedPrompt, context);
            
            Map<String, Object> result = new HashMap<>();
            result.put("rawResponse", response);
            result.put("format", responseFormat);
            result.put("timestamp", new Date());
            
            // Try to parse structured response based on format
            if ("json".equalsIgnoreCase(responseFormat)) {
                try {
                    // In a real implementation, you would parse JSON here
                    result.put("parsed", "JSON parsing would be implemented here");
                } catch (Exception e) {
                    result.put("parseError", "Failed to parse JSON response");
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error generating structured response", e);
            throw new RuntimeException("Failed to generate structured response", e);
        }
    }
    
    /**
     * Calculate estimated cost for a request
     */
    public Map<String, Object> estimateRequestCost(String prompt, Map<String, Object> context) {
        Map<String, Object> estimate = new HashMap<>();
        
        try {
            int promptTokens = estimateTokenCount(prompt);
            int contextTokens = estimateTokenCount(context.toString());
            int totalInputTokens = promptTokens + contextTokens;
            int estimatedOutputTokens = Math.min(maxTokens, totalInputTokens / 2); // Rough estimate
            
            // Pricing estimates (as of knowledge cutoff - actual prices may vary)
            double inputCostPer1kTokens = getInputCostPer1kTokens(defaultModel);
            double outputCostPer1kTokens = getOutputCostPer1kTokens(defaultModel);
            
            double inputCost = (totalInputTokens / 1000.0) * inputCostPer1kTokens;
            double outputCost = (estimatedOutputTokens / 1000.0) * outputCostPer1kTokens;
            double totalCost = inputCost + outputCost;
            
            estimate.put("inputTokens", totalInputTokens);
            estimate.put("estimatedOutputTokens", estimatedOutputTokens);
            estimate.put("estimatedTotalCost", totalCost);
            estimate.put("model", defaultModel);
            estimate.put("currency", "USD");
            estimate.put("note", "Estimates are approximate and may not reflect actual costs");
            
        } catch (Exception e) {
            logger.error("Error estimating request cost", e);
            estimate.put("error", "Failed to estimate cost");
        }
        
        return estimate;
    }
    
    private double getInputCostPer1kTokens(String model) {
        switch (model.toLowerCase()) {
            case "gpt-4":
                return 0.03;
            case "gpt-4-turbo-preview":
                return 0.01;
            case "gpt-3.5-turbo":
                return 0.0015;
            case "gpt-3.5-turbo-16k":
                return 0.003;
            default:
                return 0.002; // Default estimate
        }
    }
    
    private double getOutputCostPer1kTokens(String model) {
        switch (model.toLowerCase()) {
            case "gpt-4":
                return 0.06;
            case "gpt-4-turbo-preview":
                return 0.03;
            case "gpt-3.5-turbo":
                return 0.002;
            case "gpt-3.5-turbo-16k":
                return 0.004;
            default:
                return 0.003; // Default estimate
        }
    }
 }