package ch.zhaw.trueyield.mcp;

import ch.zhaw.trueyield.service.AiAnalysisService;
import ch.zhaw.trueyield.service.provider.NewsArticle;
import ch.zhaw.trueyield.service.provider.NewsProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MCP tools exposing TrueYield's ESG analysis capabilities to AI clients
 * (e.g. Claude Desktop). Connect via the SSE endpoint at /sse.
 */
@Component
public class EsgMcpTools {

    private final AiAnalysisService aiAnalysisService;
    private final List<NewsProvider> newsProviders;

    public EsgMcpTools(@Lazy AiAnalysisService aiAnalysisService, List<NewsProvider> newsProviders) {
        this.aiAnalysisService = aiAnalysisService;
        this.newsProviders = newsProviders;
    }

    @Tool(description = "Generate a concise ESG risk summary for a portfolio of holdings. "
            + "Provide a comma-separated list of company names and receive a professional "
            + "2-3 sentence risk assessment focused on greenwashing risks and ESG compliance concerns.")
    public String generateEsgRiskSummary(
            @ToolParam(description = "Comma-separated list of company names, e.g. 'Apple Inc., Tesla, Nestlé AG'")
            String companies) {
        List<String> names = Arrays.stream(companies.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        return aiAnalysisService.generateRiskSummary(names);
    }

    @Tool(description = "Analyse the ESG sentiment of a text snippet. "
            + "Returns a score between -1.0 (very negative ESG news) and 1.0 (very positive ESG news). "
            + "Useful for rating ESG news articles or sustainability reports.")
    public double analyseEsgSentiment(
            @ToolParam(description = "Text snippet about ESG matters, e.g. a news excerpt or sustainability statement")
            String text) {
        return aiAnalysisService.analyzeSentiment(text);
    }

    @Tool(description = "Fetch recent ESG and sustainability news articles for a company from multiple financial providers. "
            + "Returns a formatted list of article titles, sources and publication dates. ")
    public String fetchEsgNews(
            @ToolParam(description = "Company name to search for, e.g. 'Shell', 'Volkswagen'")
            String company) {
        
        List<NewsArticle> allArticles = new ArrayList<>();
        for (NewsProvider provider : newsProviders) {
            if (provider.isConfigured()) {
                try {
                    allArticles.addAll(provider.fetchNewsForHolding(company));
                } catch (Exception ignored) {}
            }
        }
        
        if (allArticles.isEmpty()) {
            return "No recent ESG news found for \"" + company + "\".";
        }
        
        StringBuilder sb = new StringBuilder("ESG news for \"").append(company).append("\":\n");
        allArticles.forEach(a -> sb.append("- [").append(a.sourceName()).append("] ").append(a.title())
                .append(" (").append(a.publishedAt()).append(")\n")
                .append("  ").append(a.url()).append("\n"));
        return sb.toString();
    }
}
