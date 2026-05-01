package ch.zhaw.trueyield.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    @Autowired(required = false)
    private ChatModel chatModel;

    @Value("${spring.ai.anthropic.api-key:}")
    private String apiKey;

    public boolean isAvailable() {
        return chatModel != null && apiKey != null && !apiKey.isBlank();
    }

    public String generateRiskSummary(List<String> holdingNames) {
        if (!isAvailable()) {
            return "AI analysis unavailable.";
        }
        String holdings = holdingNames.isEmpty() ? "no holdings listed" : String.join(", ", holdingNames);
        log.info("AiAnalysisService: generating risk summary for holdings: [{}]", holdings);
        String prompt = """
                You are an ESG risk analyst. Provide a concise 2-3 sentence risk summary
                for an ESG investment portfolio containing the following holdings: %s.
                Focus on potential greenwashing risks and ESG compliance concerns for these
                specific companies. Be specific and professional.
                """.formatted(holdings);
        try {
            return chatModel.call(new Prompt(prompt))
                    .getResult().getOutput().getText();
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
            String text = chatModel.call(new Prompt(prompt))
                    .getResult().getOutput().getText();
            double score = Double.parseDouble(text.trim().replace(',', '.'));
            return Math.max(-1.0, Math.min(1.0, score));
        } catch (Exception e) {
            log.warn("AiAnalysisService: analyzeSentiment failed: {}", e.getMessage());
            return 0.0;
        }
    }

    public double analyzeRelevance(String companyName, String articleText) {
        if (!isAvailable()) {
            return 1.0; // Assume relevant if AI is down to not block ingestion
        }
        String prompt = """
                Read the following news article snippet. Does it seem related to the company "%s" or its industry?
                Even if the company is only mentioned briefly or implied by context (like its CEO, products, or sector news where it operates), consider it relevant.
                Only reject it if it is completely unrelated spam or clearly about a completely different topic.
                
                Respond ONLY with a decimal number between 0.0 (completely irrelevant) and 1.0 (relevant). No explanation.
                
                Text: "%s"
                """.formatted(companyName, articleText);
        try {
            String text = chatModel.call(new Prompt(prompt))
                    .getResult().getOutput().getText();
            double score = Double.parseDouble(text.trim().replace(',', '.'));
            return Math.max(0.0, Math.min(1.0, score));
        } catch (Exception e) {
            log.warn("AiAnalysisService: analyzeRelevance failed for '{}': {}", companyName, e.getMessage());
            return 1.0; 
        }
    }
}
