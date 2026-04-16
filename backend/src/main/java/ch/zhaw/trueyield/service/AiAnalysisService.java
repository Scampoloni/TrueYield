package ch.zhaw.trueyield.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";

    @Value("${spring.ai.anthropic.api-key:}")
    private String apiKey;

    @Value("${spring.ai.anthropic.chat.options.model:claude-haiku-4-5-20251001}")
    private String model;

    @Value("${spring.ai.anthropic.chat.options.max-tokens:512}")
    private int maxTokens;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.create();
        if (isAvailable()) {
            log.info("AiAnalysisService: Anthropic API configured (direct RestClient).");
        } else {
            log.warn("AiAnalysisService: ANTHROPIC_API_KEY not set — AI features disabled.");
        }
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generateRiskSummary(String portfolioId) {
        if (!isAvailable()) {
            return "AI analysis unavailable.";
        }
        String prompt = """
                You are an ESG risk analyst. Provide a concise 2-3 sentence risk summary
                for the investment portfolio with ID "%s".
                Focus on potential greenwashing risks and ESG compliance concerns.
                Be specific and professional.
                """.formatted(portfolioId);
        return callAnthropic(prompt, "AI analysis unavailable.");
    }

    public double analyzeSentiment(String contentSnippet) {
        if (!isAvailable()) {
            return 0.0;
        }
        String prompt = """
                Analyze the ESG sentiment of the following text and respond ONLY with a
                decimal number between -1.0 (very negative ESG news) and 1.0 (very positive ESG news).
                No explanation, just the number.

                Text: "%s"
                """.formatted(contentSnippet);
        try {
            String text = callAnthropic(prompt, "0.0");
            double score = Double.parseDouble(text.trim().replace(',', '.'));
            return Math.max(-1.0, Math.min(1.0, score));
        } catch (Exception e) {
            log.warn("AiAnalysisService: analyzeSentiment parse failed: {}", e.getMessage());
            return 0.0;
        }
    }

    @SuppressWarnings("unchecked")
    private String callAnthropic(String userMessage, String fallback) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "messages", List.of(Map.of("role", "user", "content", userMessage))
            );

            Map<?, ?> response = restClient.post()
                    .uri(ANTHROPIC_API_URL)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                List<Map<String, Object>> content =
                        (List<Map<String, Object>>) response.get("content");
                if (content != null && !content.isEmpty()) {
                    return String.valueOf(content.get(0).get("text"));
                }
            }
            return fallback;
        } catch (Exception e) {
            log.warn("AiAnalysisService: Anthropic call failed: {}", e.getMessage());
            return fallback;
        }
    }
}
