package ch.zhaw.trueyield.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ChatModel.class)
public class AiAnalysisService {

    private final ChatClient chatClient;

    @Autowired
    public AiAnalysisService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String generateRiskSummary(String portfolioId) {
        String prompt = """
                You are an ESG risk analyst. Provide a concise 2-3 sentence risk summary
                for the investment portfolio with ID \"%s\".
                Focus on potential greenwashing risks and ESG compliance concerns.
                Be specific and professional.
                """.formatted(portfolioId);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    public double analyzeSentiment(String contentSnippet) {
        String prompt = """
                Analyze the ESG sentiment of the following text and respond ONLY with a
                decimal number between -1.0 (very negative ESG news) and 1.0 (very positive ESG news).
                No explanation, just the number.

                Text: \"%s\"
                """.formatted(contentSnippet);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content()
                .trim()
                .replace(',', '.');

        try {
            double score = Double.parseDouble(response);
            return Math.max(-1.0, Math.min(1.0, score));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
