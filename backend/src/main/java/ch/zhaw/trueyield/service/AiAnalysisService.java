package ch.zhaw.trueyield.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiAnalysisService {

    public record PortfolioRiskResult(int score, String rationale) {}

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

    public boolean isPremiumSource(String sourceName) {
        if (sourceName == null || sourceName.isBlank()) return false;
        if (!isAvailable()) return false;
        String prompt = """
                Is "%s" a premium, high-quality financial or general news source with strong editorial
                standards (e.g. Reuters, Bloomberg, Financial Times, Wall Street Journal, The Guardian,
                AP, AFP, BBC, Le Monde, Der Spiegel, NZZ)?
                Respond ONLY with "yes" or "no".
                """.formatted(sourceName);
        try {
            String answer = chatModel.call(new Prompt(prompt))
                    .getResult().getOutput().getText();
            return answer != null && answer.trim().toLowerCase().startsWith("yes");
        } catch (Exception e) {
            log.warn("AiAnalysisService: isPremiumSource check failed for '{}': {}", sourceName, e.getMessage());
            return false;
        }
    }

    public PortfolioRiskResult generatePortfolioRiskScore(List<String> holdingNames, List<String> evidenceSnippets) {
        if (!isAvailable()) {
            return new PortfolioRiskResult(5, "AI analysis unavailable.");
        }
        String holdings = holdingNames.isEmpty() ? "no holdings listed" : String.join(", ", holdingNames);
        String evidence = evidenceSnippets.isEmpty()
                ? "No news evidence available."
                : evidenceSnippets.stream().map(s -> "- " + s).collect(Collectors.joining("\n"));
        log.info("AiAnalysisService: generating portfolio risk score for holdings: [{}], {} evidence snippets",
                holdings, evidenceSnippets.size());
        String prompt = """
                You are an ESG risk analyst. Assess the overall ESG risk of an investment portfolio.

                Holdings in this portfolio: %s

                Recent ESG news evidence collected for these holdings:
                %s

                Using BOTH your training knowledge about these companies AND the news evidence above, \
                assign an overall portfolio ESG risk score.

                Scoring guide:
                - Score 8–10: Severe ESG violations, clear greenwashing, fossil fuel industries without \
                credible transition plan
                - Score 5–7: Mixed ESG profile, moderate risks, some controversies
                - Score 1–4: Sustainable companies, strong ESG compliance, renewable energy, low controversy
                - Score 0: Only if all companies are explicitly ESG-certified with no negative signals

                Respond in EXACTLY this format:
                Line 1: a single integer from 0 to 10 (nothing else on this line)
                Line 2: one or two sentences explaining the score in English
                """.formatted(holdings, evidence);
        try {
            String response = chatModel.call(new Prompt(prompt))
                    .getResult().getOutput().getText().trim();
            String[] lines = response.split("\n", 2);
            int score = Math.max(0, Math.min(10, Integer.parseInt(lines[0].trim())));
            String rationale = lines.length > 1 && !lines[1].isBlank() ? lines[1].trim() : "No rationale provided.";
            log.info("AiAnalysisService: portfolio risk score={}, rationale='{}'", score, rationale);
            return new PortfolioRiskResult(score, rationale);
        } catch (Exception e) {
            log.warn("AiAnalysisService: generatePortfolioRiskScore failed: {}", e.getMessage());
            return new PortfolioRiskResult(5, "AI analysis unavailable.");
        }
    }

    public double generatePortfolioSentiment(List<String> holdingNames) {
        if (!isAvailable()) {
            return 0.0;
        }
        String holdings = holdingNames.isEmpty() ? "no holdings listed" : String.join(", ", holdingNames);
        log.info("AiAnalysisService: generating training-based ESG sentiment for holdings: [{}]", holdings);
        String prompt = """
                You are an ESG analyst. Based solely on your training knowledge about these companies, \
                rate the overall ESG sentiment of this portfolio as a number between -1.0 and 1.0.

                Companies: %s

                Guidelines:
                -  1.0: All companies are established ESG leaders, renewable energy, no controversies
                -  0.3: Mostly positive ESG profile with minor issues
                -  0.0: Neutral, mixed ESG signals or insufficient knowledge
                - -0.3: Notable ESG controversies, fossil fuels, governance issues
                - -1.0: Severe ESG violations, greenwashing, significant environmental damage

                Respond ONLY with a single decimal number between -1.0 and 1.0. No explanation.
                """.formatted(holdings);
        try {
            String text = chatModel.call(new Prompt(prompt))
                    .getResult().getOutput().getText();
            double score = Double.parseDouble(text.trim().replace(',', '.'));
            log.info("AiAnalysisService: training-based sentiment score={}", score);
            return Math.max(-1.0, Math.min(1.0, score));
        } catch (Exception e) {
            log.warn("AiAnalysisService: generatePortfolioSentiment failed: {}", e.getMessage());
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
