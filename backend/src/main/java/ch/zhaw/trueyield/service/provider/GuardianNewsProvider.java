package ch.zhaw.trueyield.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class GuardianNewsProvider implements NewsProvider {

    private static final Logger log = LoggerFactory.getLogger(GuardianNewsProvider.class);
    private static final int MAX_ARTICLES = 5;
    private static final Set<String> ALLOWED_HOSTS = Set.of("content.guardianapis.com");
    // Possessive quantifier \s*+ prevents backtracking between the suffix and end-of-string anchor
    private static final Pattern COMPANY_SUFFIX =
            Pattern.compile("\\s+(?:Inc\\.?|PLC\\.?|Ltd\\.?|Corp\\.?|AG|SE|NV|SA|GmbH)\\s*+$",
                    Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final String apiKey;

    @org.springframework.beans.factory.annotation.Autowired
    public GuardianNewsProvider(
            @Value("${news.api.guardian.base-url:https://content.guardianapis.com}") String baseUrl,
            @Value("${news.api.guardian.key:}") String apiKey) {
        this.apiKey = apiKey;
        validateBaseUrl(baseUrl, ALLOWED_HOSTS);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    static void validateBaseUrl(String url, Set<String> allowedHosts) {
        try {
            String host = URI.create(url).getHost();
            if (host == null || !allowedHosts.contains(host)) {
                throw new IllegalArgumentException("Disallowed news API host: " + host);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid news API base URL: " + url, e);
        }
    }

    GuardianNewsProvider(String apiKey, RestClient restClient) {
        this.apiKey = apiKey;
        this.restClient = restClient;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "The Guardian";
    }

    @Override
    public List<NewsArticle> fetchNewsForHolding(String companyName) {
        if (!isConfigured()) return Collections.emptyList();

        String cleanName = COMPANY_SUFFIX.matcher(companyName).replaceAll("")
                .trim();
        String firstWord = cleanName.split("\\s+")[0];

        try {
            // Primary: exact company name + ESG (phrase match)
            List<Map<String, Object>> results = searchGuardian("\"" + cleanName + "\" ESG");
            // Fallback: only if first word is meaningfully different and still company-specific
            if (results.isEmpty() && !firstWord.equals(cleanName) && firstWord.length() > 3) {
                results = searchGuardian("\"" + firstWord + "\" AND (ESG OR sustainability OR greenwashing)");
            }
            return results.stream().map(this::mapArticle).toList();
        } catch (Exception e) {
            log.warn("GuardianNewsProvider: failed to fetch news for '{}': {}", companyName, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchGuardian(String query) {
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
        if (response == null || !response.containsKey("response")) return Collections.emptyList();
        Map<String, Object> inner = (Map<String, Object>) response.get("response");
        List<Map<String, Object>> results = (List<Map<String, Object>>) inner.get("results");
        return results != null ? results : Collections.emptyList();
    }

    private NewsArticle mapArticle(Map<String, Object> raw) {
        String title = (String) raw.getOrDefault("webTitle", "");
        String url = (String) raw.getOrDefault("webUrl", "");
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) raw.get("fields");
        String content = fields != null ? (String) fields.getOrDefault("trailText", title) : title;
        return new NewsArticle(title, content, url, LocalDate.now(), getProviderName());
    }
}
