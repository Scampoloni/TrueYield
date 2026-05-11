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
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class NewsdataIoProvider implements NewsProvider {

    private static final Logger log = LoggerFactory.getLogger(NewsdataIoProvider.class);
    private static final int MAX_ARTICLES = 10;
    private static final Set<String> ALLOWED_HOSTS = Set.of("newsdata.io");
    // Possessive quantifier \s*+ prevents backtracking between the suffix and end-of-string anchor
    private static final Pattern COMPANY_SUFFIX =
            Pattern.compile("\\s+(?:Inc\\.?|PLC\\.?|Ltd\\.?|Corp\\.?|AG|SE|NV|SA|GmbH)\\s*+$",
                    Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final String apiKey;

    @org.springframework.beans.factory.annotation.Autowired
    public NewsdataIoProvider(
            @Value("${news.api.newsdata.base-url:https://newsdata.io/api/1}") String baseUrl,
            @Value("${news.api.newsdata.key:}") String apiKey) {
        this.apiKey = apiKey;
        GuardianNewsProvider.validateBaseUrl(baseUrl, ALLOWED_HOSTS);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    NewsdataIoProvider(String apiKey, RestClient restClient) {
        this.apiKey = apiKey;
        this.restClient = restClient;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "Newsdata.io";
    }

    @Override
    public List<NewsArticle> fetchNewsForHolding(String companyName) {
        if (!isConfigured()) return Collections.emptyList();
        String cleanName = COMPANY_SUFFIX.matcher(companyName).replaceAll("").trim();
        String query = cleanName + " AND (ESG OR sustainability OR greenwashing OR climate OR emissions)";
        return fetchNews(query, companyName);
    }

    @Override
    public List<NewsArticle> fetchNewsForSymbol(String symbol, String companyName) {
        if (!isConfigured()) return Collections.emptyList();
        String cleanName = COMPANY_SUFFIX.matcher(companyName).replaceAll("").trim();
        String query = "(" + symbol + " OR " + cleanName + ") AND (ESG OR sustainability OR greenwashing OR climate)";
        return fetchNews(query, companyName);
    }

    private List<NewsArticle> fetchNews(String query, String companyName) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/news")
                            .queryParam("q", query)
                            .queryParam("category", "business")
                            .queryParam("language", "en")
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("results")) return Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null) return Collections.emptyList();

            return results.stream().limit(MAX_ARTICLES).map(this::mapArticle).toList();
        } catch (Exception e) {
            log.warn("NewsdataIoProvider: failed to fetch news for '{}': {}", companyName, e.getMessage());
            return Collections.emptyList();
        }
    }

    private NewsArticle mapArticle(Map<String, Object> raw) {
        String title = (String) raw.getOrDefault("title", "");
        String url = (String) raw.getOrDefault("link", "");
        String content = (String) raw.getOrDefault("description", title);
        if (content == null || content.isBlank()) {
            content = title;
        }
        
        String sourceName = getProviderName();
        if (raw.containsKey("source_id")) {
             sourceName = (String) raw.get("source_id");
             // Capitalize first letter
             if (sourceName != null && !sourceName.isEmpty()) {
                 sourceName = sourceName.substring(0, 1).toUpperCase() + sourceName.substring(1);
             }
        }
        
        return new NewsArticle(title, content, url, LocalDate.now(), sourceName != null ? sourceName : getProviderName());
    }
}
