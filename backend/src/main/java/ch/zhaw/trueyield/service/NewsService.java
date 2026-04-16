package ch.zhaw.trueyield.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private static final int MAX_ARTICLES = 5;

    private final RestClient restClient;
    private final String apiKey;

    public NewsService(
            @Value("${news.api.base-url:https://content.guardianapis.com}") String baseUrl,
            @Value("${news.api.key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Fetches up to MAX_ARTICLES ESG-relevant news articles for a given company name or symbol.
     * Returns an empty list if the API key is not configured or the request fails.
     */
    public List<NewsArticle> fetchNewsForHolding(String companyName) {
        if (!isConfigured()) {
            log.debug("NewsService: NEWS_API_KEY not configured, skipping news fetch for '{}'", companyName);
            return Collections.emptyList();
        }

        String cleanName = companyName
                .replaceAll("(?i)\\s+(Inc\\.?|PLC\\.?|Ltd\\.?|Corp\\.?|AG|SE|NV|SA|GmbH)\\s*$", "")
                .trim();
        String query = "\"" + cleanName + "\" ESG";

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("page-size", MAX_ARTICLES)
                            .queryParam("show-fields", "trailText")
                            .queryParam("api-key", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("response")) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> inner = (Map<String, Object>) response.get("response");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) inner.get("results");
            if (results == null) {
                return Collections.emptyList();
            }

            return results.stream()
                    .map(this::mapArticle)
                    .toList();

        } catch (Exception e) {
            log.warn("NewsService: failed to fetch news for '{}': {}", companyName, e.getMessage());
            return Collections.emptyList();
        }
    }

    private NewsArticle mapArticle(Map<String, Object> raw) {
        String title = (String) raw.getOrDefault("webTitle", "");
        String url = (String) raw.getOrDefault("webUrl", "");
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) raw.get("fields");
        String content = fields != null ? (String) fields.getOrDefault("trailText", title) : title;
        return new NewsArticle(title, content, url, LocalDate.now());
    }

    public record NewsArticle(String title, String content, String url, LocalDate publishedAt) {}
}
