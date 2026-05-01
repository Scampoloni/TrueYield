package ch.zhaw.trueyield.service.provider;

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
public class NewsApiOrgProvider implements NewsProvider {

    private static final Logger log = LoggerFactory.getLogger(NewsApiOrgProvider.class);
    private static final int MAX_ARTICLES = 5;

    private final RestClient restClient;
    private final String apiKey;

    public NewsApiOrgProvider(
            @Value("${news.api.newsapiorg.base-url:https://newsapi.org/v2}") String baseUrl,
            @Value("${news.api.newsapiorg.key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "TrueYield/1.0")
                .build();
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "NewsAPI.org";
    }

    @Override
    public List<NewsArticle> fetchNewsForHolding(String companyName) {
        if (!isConfigured()) return Collections.emptyList();

        String cleanName = companyName
                .replaceAll("(?i)\\s+(Inc\\.?|PLC\\.?|Ltd\\.?|Corp\\.?|AG|SE|NV|SA|GmbH)\\s*$", "")
                .trim();

        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/everything")
                            .queryParam("q", "\"" + cleanName + "\" AND (ESG OR sustainability)")
                            .queryParam("language", "en")
                            .queryParam("sortBy", "relevancy")
                            .queryParam("pageSize", MAX_ARTICLES)
                            .queryParam("apiKey", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("articles")) return Collections.emptyList();
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
            if (articles == null) return Collections.emptyList();
            
            return articles.stream().map(this::mapArticle).toList();
        } catch (Exception e) {
            log.warn("NewsApiOrgProvider: failed to fetch news for '{}': {}", companyName, e.getMessage());
            return Collections.emptyList();
        }
    }

    private NewsArticle mapArticle(Map<String, Object> raw) {
        String title = (String) raw.getOrDefault("title", "");
        String url = (String) raw.getOrDefault("url", "");
        String content = (String) raw.getOrDefault("description", title);
        if (content == null || content.isBlank()) {
            content = title;
        }
        
        String sourceName = getProviderName();
        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) raw.get("source");
        if (source != null && source.containsKey("name")) {
             sourceName = (String) source.get("name");
        }
        
        return new NewsArticle(title, content, url, LocalDate.now(), sourceName != null ? sourceName : getProviderName());
    }
}
