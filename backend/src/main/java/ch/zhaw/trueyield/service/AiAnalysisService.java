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
                Rate how relevant this news article is to the ESG (Environmental, Social, Governance) \
                risk profile of the company "%s".

                Score 0.7–1.0: the article specifically covers this company's environmental impact, \
                labour practices, governance issues, greenwashing, ESG ratings, sustainability \
                strategy, regulatory ESG compliance, or supply-chain ethics.
                Score 0.3–0.6: the article covers this company with a partial ESG angle, or covers \
                sector-wide ESG issues that directly affect this company.
                Score 0.0–0.2: only tangentially related, about a different company, generic business \
                news with no ESG angle, or an unrelated topic entirely.

                Respond ONLY with a single decimal number between 0.0 and 1.0. No explanation.

                Company: "%s"
                Article: "%s"
                """.formatted(companyName, companyName, articleText);
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
