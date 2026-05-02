package ch.zhaw.trueyield.service.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsApiOrgProviderTest {

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private NewsApiOrgProvider provider;

    @BeforeEach
    void setUp() {
        provider = new NewsApiOrgProvider("test-key", restClient);
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(uriSpec).retrieve();
    }

    @Test
    void fetchNews_returnsArticles_whenApiResponds() {
        Map<String, Object> article = Map.of(
                "title", "Tesla ESG Risk",
                "url", "https://newsapi.org/tesla-esg",
                "description", "Tesla faces ESG scrutiny.",
                "source", Map.of("name", "Reuters"));
        doReturn(Map.of("articles", List.of(article))).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("Tesla Inc.");

        assertEquals(1, result.size());
        assertEquals("Tesla ESG Risk", result.get(0).title());
        assertEquals("https://newsapi.org/tesla-esg", result.get(0).url());
        assertEquals("Tesla faces ESG scrutiny.", result.get(0).content());
        assertEquals("Reuters", result.get(0).sourceName());
    }

    @Test
    void fetchNews_usesTitle_whenDescriptionIsBlank() {
        Map<String, Object> article = Map.of(
                "title", "BASF Sustainability",
                "url", "https://newsapi.org/basf",
                "description", "");
        doReturn(Map.of("articles", List.of(article))).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("BASF SE");

        assertEquals(1, result.size());
        assertEquals("BASF Sustainability", result.get(0).content());
    }

    @Test
    void fetchNews_usesProviderName_whenSourceIsNull() {
        Map<String, Object> article = new HashMap<>();
        article.put("title", "Unilever ESG");
        article.put("url", "https://newsapi.org/unilever");
        article.put("description", "Unilever news.");
        article.put("source", null);
        doReturn(Map.of("articles", List.of(article))).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("Unilever Ltd.");

        assertEquals(1, result.size());
        assertEquals("NewsAPI.org", result.get(0).sourceName());
    }

    @Test
    void fetchNews_returnsEmpty_whenResponseHasNoArticlesKey() {
        doReturn(Map.of("status", "ok")).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_returnsEmpty_whenResponseIsNull() {
        doReturn(null).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_returnsEmpty_whenArticlesIsNull() {
        Map<String, Object> response = new HashMap<>();
        response.put("articles", null);
        doReturn(response).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_returnsEmpty_whenApiThrows() {
        doThrow(new RuntimeException("timeout")).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_stripsAgSuffix_beforeQuerying() {
        doReturn(Map.of("articles", List.of())).when(responseSpec).body(Map.class);

        provider.fetchNewsForHolding("Volkswagen AG");

        verify(responseSpec, atLeastOnce()).body(Map.class);
    }
}
