package ch.zhaw.trueyield.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    @Value("${spring.ai.anthropic.api-key:}")
    private String apiKey;

    private ChatClient chatClient;

    @PostConstruct
    public void init() {
        if (chatClientBuilder != null && apiKey != null && !apiKey.isBlank() && !apiKey.equals("dummy-key")) {
            this.chatClient = chatClientBuilder.build();
            log.info("AiAnalysisService: ChatClient initialised (Anthropic).");
        } else {
            log.warn("AiAnalysisService: ANTHROPIC_API_KEY not set or ChatClient.Builder unavailable — AI features disabled.");
        }
    }

    public boolean isAvailable() {
        return chatClient != null;
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
        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.warn("AiAnalysisService: generateRiskSummary failed: {}", e.getMessage());
            return "AI analysis unavailable.";
        }
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
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim()
                    .replace(',', '.');
            double score = Double.parseDouble(response);
            return Math.max(-1.0, Math.min(1.0, score));
        } catch (Exception e) {
            log.warn("AiAnalysisService: analyzeSentiment failed: {}", e.getMessage());
            return 0.0;
        }
    }
}
