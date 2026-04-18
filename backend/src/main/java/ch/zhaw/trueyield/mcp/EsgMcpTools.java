package ch.zhaw.trueyield.mcp;

import ch.zhaw.trueyield.service.AiAnalysisService;
import ch.zhaw.trueyield.service.NewsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * MCP tools exposing TrueYield's ESG analysis capabilities to AI clients
 * (e.g. Claude Desktop). Connect via the SSE endpoint at /sse.
 */
@Component
public class EsgMcpTools {

    private final AiAnalysisService aiAnalysisService;
    private final NewsService newsService;

    public EsgMcpTools(AiAnalysisService aiAnalysisService, NewsService newsService) {
        this.aiAnalysisService = aiAnalysisService;
        this.newsService = newsService;
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

    @Tool(description = "Fetch recent ESG and sustainability news articles for a company from The Guardian. "
            + "Returns a formatted list of article titles and publication dates. "
            + "Returns an empty result if the news API is not configured.")
    public String fetchEsgNews(
            @ToolParam(description = "Company name to search for, e.g. 'Shell', 'Volkswagen'")
            String company) {
        List<NewsService.NewsArticle> articles = newsService.fetchNewsForHolding(company);
        if (articles.isEmpty()) {
            return "No recent ESG news found for \"" + company + "\".";
        }
        StringBuilder sb = new StringBuilder("ESG news for \"").append(company).append("\":\n");
        articles.forEach(a -> sb.append("- ").append(a.title())
                .append(" [").append(a.publishedAt()).append("]\n")
                .append("  ").append(a.url()).append("\n"));
        return sb.toString();
    }
}
