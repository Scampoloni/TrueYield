package ch.zhaw.trueyield.service.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlphaVantageNewsProviderTest {

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private AlphaVantageNewsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AlphaVantageNewsProvider("test-key", restClient);
    }

    @SuppressWarnings("unchecked")
    private void stubRestClient(Object body) {
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(uriSpec).retrieve();
        doReturn(body).when(responseSpec).body(Map.class);
    }

    private void stubRestClientThrows(RuntimeException ex) {
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(uriSpec).retrieve();
        doThrow(ex).when(responseSpec).body(Map.class);
    }

    private Map<String, Object> sampleArticle() {
        return Map.of(
                "title", "Ørsted expands offshore wind capacity",
                "url", "https://example.com/orsted-wind",
                "summary", "Ørsted announces major offshore wind expansion in the North Sea.",
                "source", "Reuters",
                "time_published", "20260509T120000"
        );
    }

    @Test
    void fetchNewsForSymbol_returnsArticles_whenApiResponds() {
        stubRestClient(Map.of("feed", List.of(sampleArticle())));

        List<NewsArticle> result = provider.fetchNewsForSymbol("ORSTED", "Ørsted A/S");

        assertEquals(1, result.size());
        assertEquals("Ørsted expands offshore wind capacity", result.get(0).title());
        assertEquals("https://example.com/orsted-wind", result.get(0).url());
        assertEquals("Reuters", result.get(0).sourceName());
        assertEquals("Ørsted announces major offshore wind expansion in the North Sea.", result.get(0).content());
    }

    @Test
    void fetchNewsForSymbol_parsesPublishedDate_correctly() {
        stubRestClient(Map.of("feed", List.of(sampleArticle())));

        List<NewsArticle> result = provider.fetchNewsForSymbol("ORSTED", "Ørsted A/S");

        assertEquals(LocalDate.of(2026, 5, 9), result.get(0).publishedAt());
    }

    @Test
    void fetchNewsForSymbol_usesTodayAsDate_whenTimePublishedMissing() {
        Map<String, Object> article = Map.of(
                "title", "Wind power growth",
                "url", "https://example.com/wind",
                "summary", "Summary text",
                "source", "Bloomberg"
        );
        stubRestClient(Map.of("feed", List.of(article)));

        List<NewsArticle> result = provider.fetchNewsForSymbol("VESTAS", "Vestas Wind Systems");

        assertEquals(LocalDate.now(), result.get(0).publishedAt());
    }

    @Test
    void fetchNewsForSymbol_usesTodayAsDate_whenTimePublishedIsInvalid() {
        Map<String, Object> article = new HashMap<>(sampleArticle());
        article.put("time_published", "BADDATE!");
        stubRestClient(Map.of("feed", List.of(article)));

        List<NewsArticle> result = provider.fetchNewsForSymbol("ORSTED", "Ørsted A/S");

        assertEquals(LocalDate.now(), result.get(0).publishedAt());
    }

    @Test
    void fetchNewsForSymbol_returnsEmpty_whenFeedKeyMissing() {
        stubRestClient(Map.of("Information", "API limit reached"));

        assertTrue(provider.fetchNewsForSymbol("AAPL", "Apple Inc.").isEmpty());
    }

    @Test
    void fetchNewsForSymbol_returnsEmpty_whenResponseIsNull() {
        stubRestClient(null);

        assertTrue(provider.fetchNewsForSymbol("AAPL", "Apple Inc.").isEmpty());
    }

    @Test
    void fetchNewsForSymbol_returnsEmpty_whenApiThrows() {
        stubRestClientThrows(new RuntimeException("timeout"));

        assertTrue(provider.fetchNewsForSymbol("AAPL", "Apple Inc.").isEmpty());
    }

    @Test
    void fetchNewsForSymbol_returnsEmpty_whenSymbolIsNull() {
        List<NewsArticle> result = provider.fetchNewsForSymbol(null, "Apple Inc.");

        assertTrue(result.isEmpty());
        verify(restClient, never()).get();
    }

    @Test
    void fetchNewsForSymbol_returnsEmpty_whenSymbolIsBlank() {
        List<NewsArticle> result = provider.fetchNewsForSymbol("   ", "Apple Inc.");

        assertTrue(result.isEmpty());
        verify(restClient, never()).get();
    }

    @Test
    void fetchNewsForHolding_returnsEmpty_becauseSymbolSearchIsPreferred() {
        List<NewsArticle> result = provider.fetchNewsForHolding("Apple Inc.");

        assertTrue(result.isEmpty());
        verify(restClient, never()).get();
    }

    @Test
    void isConfigured_returnsFalse_whenKeyIsBlank() {
        AlphaVantageNewsProvider unconfigured = new AlphaVantageNewsProvider("", restClient);
        assertFalse(unconfigured.isConfigured());
    }

    @Test
    void isConfigured_returnsTrue_whenKeyIsPresent() {
        assertTrue(provider.isConfigured());
    }

    @Test
    void getProviderName_returnsAlphaVantage() {
        assertEquals("Alpha Vantage", provider.getProviderName());
    }
}
