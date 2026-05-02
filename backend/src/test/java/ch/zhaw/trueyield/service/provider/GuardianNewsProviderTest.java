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
class GuardianNewsProviderTest {

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private GuardianNewsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new GuardianNewsProvider("test-key", restClient);
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(uriSpec).retrieve();
    }

    @Test
    void fetchNews_returnsArticles_whenApiResponds() {
        Map<String, Object> article = Map.of(
                "webTitle", "Apple ESG Report",
                "webUrl", "https://theguardian.com/apple-esg",
                "fields", Map.of("trailText", "Apple publishes ESG report."));
        doReturn(Map.of("response", Map.of("results", List.of(article)))).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("Apple Inc.");

        assertEquals(1, result.size());
        assertEquals("Apple ESG Report", result.get(0).title());
        assertEquals("https://theguardian.com/apple-esg", result.get(0).url());
        assertEquals("Apple publishes ESG report.", result.get(0).content());
        assertEquals("The Guardian", result.get(0).sourceName());
    }

    @Test
    void fetchNews_fallsBackToFirstWord_whenExactQueryReturnsEmpty() {
        Map<String, Object> article = Map.of(
                "webTitle", "Deutsche sustainability",
                "webUrl", "https://theguardian.com/deutsche",
                "fields", Map.of("trailText", "Deutsche ESG progress."));

        // "Deutsche Bank AG" → cleanName="Deutsche Bank", firstWord="Deutsche" (different → fallback fires)
        doReturn(Map.of("response", Map.of("results", List.of())))
                .doReturn(Map.of("response", Map.of("results", List.of(article))))
                .when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("Deutsche Bank AG");

        assertEquals(1, result.size());
        assertEquals("Deutsche sustainability", result.get(0).title());
    }

    @Test
    void fetchNews_usesTitle_whenFieldsIsNull() {
        Map<String, Object> article = Map.of(
                "webTitle", "Shell Climate Plan",
                "webUrl", "https://theguardian.com/shell");
        doReturn(Map.of("response", Map.of("results", List.of(article)))).when(responseSpec).body(Map.class);

        List<NewsArticle> result = provider.fetchNewsForHolding("Shell PLC");

        assertEquals(1, result.size());
        assertEquals("Shell Climate Plan", result.get(0).content());
    }

    @Test
    void fetchNews_returnsEmpty_whenResponseHasNoResponseKey() {
        doReturn(Map.of("status", "ok")).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_returnsEmpty_whenResponseIsNull() {
        doReturn(null).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_returnsEmpty_whenResultsIsNull() {
        Map<String, Object> inner = new HashMap<>();
        inner.put("results", null);
        doReturn(Map.of("response", inner)).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_returnsEmpty_whenApiThrows() {
        doThrow(new RuntimeException("Connection refused")).when(responseSpec).body(Map.class);

        assertTrue(provider.fetchNewsForHolding("Apple Inc.").isEmpty());
    }

    @Test
    void fetchNews_stripsCorpSuffix_beforeQuerying() {
        doReturn(Map.of("response", Map.of("results", List.of()))).when(responseSpec).body(Map.class);

        provider.fetchNewsForHolding("Siemens Corp.");

        verify(responseSpec, atLeastOnce()).body(Map.class);
    }
}
