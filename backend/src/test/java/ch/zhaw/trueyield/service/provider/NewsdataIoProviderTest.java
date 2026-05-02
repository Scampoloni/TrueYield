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
class NewsdataIoProviderTest {

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private NewsdataIoProvider provider;

    @BeforeEach
    void setUp() {
        provider = new NewsdataIoProvider("test-key", restClient);
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(uriSpec).retrieve();
    }

    @Test
    void fetchNews_returnsArticles_whenApiResponds() {
        Map<String, Object> article = Map.of(
                "title", "BP Greenwashing",
                "link", "https://newsdata.io/bp-esg",
                "description", "BP faces greenwashing claims.",
                "source_id", "reuters");
        doReturn(Map.of("results", List.of(article))).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("BP PLC");

        assertEquals(1, result.size());
        assertEquals("BP Greenwashing", result.get(0).title());
        assertEquals("https://newsdata.io/bp-esg", result.get(0).url());
        assertEquals("BP faces greenwashing claims.", result.get(0).content());
        assertEquals("Reuters", result.get(0).sourceName());
    }

    @Test
    void fetchNews_capitalizesSourceId() {
        Map<String, Object> article = Map.of(
                "title", "Nestlé Water",
                "link", "https://newsdata.io/nestle",
                "description", "Water usage.",
                "source_id", "bloomberg");
        doReturn(Map.of("results", List.of(article))).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("Nestlé AG");

        assertEquals("Bloomberg", result.get(0).sourceName());
    }

    @Test
    void fetchNews_usesTitle_whenDescriptionIsBlank() {
        Map<String, Object> article = Map.of(
                "title", "Shell Carbon Plan",
                "link", "https://newsdata.io/shell",
                "description", "");
        doReturn(Map.of("results", List.of(article))).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("Shell PLC");

        assertEquals("Shell Carbon Plan", result.get(0).content());
    }

    @Test
    void fetchNews_usesProviderName_whenSourceIdAbsent() {
        Map<String, Object> article = new HashMap<>();
        article.put("title", "BASF ESG");
        article.put("link", "https://newsdata.io/basf");
        article.put("description", "BASF news.");
        doReturn(Map.of("results", List.of(article))).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("BASF SE");

        assertEquals("Newsdata.io", result.get(0).sourceName());
    }

    @Test
    void fetchNews_limitsToFiveArticles() {
        List<Map<String, Object>> articles = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            articles.add(Map.of("title", "Article " + i, "link", "https://newsdata.io/" + i, "description", "desc"));
        }
        doReturn(Map.of("results", articles)).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("Siemens Corp.");

        assertEquals(5, result.size());
    }

    @Test
    void fetchNews_returnsEmpty_whenResponseHasNoResultsKey() {
        doReturn(Map.of("status", "success")).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_returnsEmpty_whenResponseIsNull() {
        doReturn(null).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_returnsEmpty_whenResultsIsNull() {
        Map<String, Object> response = new HashMap<>();
        response.put("results", null);
        doReturn(response).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_returnsEmpty_whenApiThrows() {
        doThrow(new RuntimeException("API error")).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }
}
