package ch.zhaw.trueyield.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * News provider backed by Alpha Vantage NEWS_SENTIMENT API.
 * Searches by ticker symbol with an ESG topic filter, guaranteeing
 * company-specific articles. Sentiment scoring is intentionally left
 * to the AI layer (Claude ESG sentiment) rather than using Alpha
 * Vantage's finance-oriented sentiment score.
 */
@Service
public class AlphaVantageNewsProvider implements NewsProvider {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageNewsProvider.class);
    private static final int MAX_ARTICLES = 5;
    private static final Set<String> ALLOWED_HOSTS = Set.of("www.alphavantage.co");
    private static final DateTimeFormatter AV_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public AlphaVantageNewsProvider(
            @Value("${news.api.alphavantage.base-url:https://www.alphavantage.co}") String baseUrl,
            @Value("${news.api.alphavantage.key:}") String apiKey) {
        this.apiKey = apiKey;
        validateBaseUrl(baseUrl);
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    AlphaVantageNewsProvider(String apiKey, RestClient restClient) {
        this.apiKey = apiKey;
        this.restClient = restClient;
    }

    private static void validateBaseUrl(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null || !ALLOWED_HOSTS.contains(host)) {
                throw new IllegalArgumentException("Disallowed Alpha Vantage host: " + host);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Alpha Vantage base URL: " + url, e);
        }
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "Alpha Vantage";
    }

    /**
     * Name-based fallback — Alpha Vantage works best with symbols,
     * so this returns empty and lets other providers handle name searches.
     */
    @Override
    public List<NewsArticle> fetchNewsForHolding(String companyName) {
        return Collections.emptyList();
    }

    /**
     * Primary method: symbol-based ESG news fetch.
     * Uses tickers + topics=environment_social_governance to ensure
     * only company-specific ESG articles are returned.
     */
    @Override
    public List<NewsArticle> fetchNewsForSymbol(String symbol, String companyName) {
        if (!isConfigured()) return Collections.emptyList();
        if (symbol == null || symbol.isBlank()) return Collections.emptyList();
        try {
            Map<?, ?> response = restClient.get()
                    .uri(u -> u.path("/query")
                            .queryParam("function", "NEWS_SENTIMENT")
                            .queryParam("tickers", symbol.toUpperCase())
                            .queryParam("topics", "environment_social_governance")
                            .queryParam("limit", MAX_ARTICLES)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("feed")) {
                log.warn("AlphaVantageNewsProvider: empty or error response for '{}'", symbol);
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> feed = (List<Map<String, Object>>) response.get("feed");
            if (feed == null) return Collections.emptyList();

            return feed.stream()
                    .limit(MAX_ARTICLES)
                    .map(this::mapArticle)
                    .toList();
        } catch (Exception e) {
            log.warn("AlphaVantageNewsProvider: failed for '{}': {}", symbol, e.getMessage());
            return Collections.emptyList();
        }
    }

    private NewsArticle mapArticle(Map<String, Object> raw) {
        String title   = (String) raw.getOrDefault("title", "");
        String url     = (String) raw.getOrDefault("url", "");
        String summary = (String) raw.getOrDefault("summary", title);
        String source  = (String) raw.getOrDefault("source", getProviderName());

        LocalDate publishedAt = LocalDate.now();
        String timePublished = (String) raw.get("time_published");
        if (timePublished != null && timePublished.length() >= 8) {
            try {
                publishedAt = LocalDate.parse(timePublished, AV_DATE);
            } catch (Exception ignored) {
                try {
                    publishedAt = LocalDate.parse(timePublished.substring(0, 8),
                            DateTimeFormatter.BASIC_ISO_DATE);
                } catch (Exception alsoIgnored) {
                    publishedAt = LocalDate.now();
                }
            }
        }

        return new NewsArticle(title, summary, url, publishedAt, source);
    }
}
