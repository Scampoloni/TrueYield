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

    private void stubRestClient(Object body) {
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(uriSpec).retrieve();
        doReturn(body).when(responseSpec).body(Map.class);
    }

    private void stubRestClientSequence(Object firstBody, Object secondBody) {
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(uriSpec).retrieve();
        doReturn(firstBody).doReturn(secondBody).when(responseSpec).body(Map.class);
    }

    private void stubRestClientSequence(Object firstBody, Object secondBody, Object thirdBody, Object fourthBody) {
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(uriSpec).retrieve();
        doReturn(firstBody).doReturn(secondBody).doReturn(thirdBody).doReturn(fourthBody).when(responseSpec).body(Map.class);
    }

    private void stubRestClientThrows(RuntimeException ex) {
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(uriSpec).retrieve();
        doThrow(ex).when(responseSpec).body(Map.class);
    }

    private Map<String, Object> sampleArticle() {
        return Map.of(
                "title", "Orsted expands offshore wind capacity",
                "url", "https://example.com/orsted-wind",
                "summary", "Orsted announces major offshore wind expansion in the North Sea.",
                "source", "Reuters",
                "time_published", "20260509T120000"
        );
    }

    @Test
    void fetchNewsForSymbol_returnsArticles_whenApiResponds() {
        stubRestClient(Map.of("feed", List.of(sampleArticle())));

        List<NewsArticle> result = provider.fetchNewsForSymbol("ORSTED", "Orsted A/S");

        assertEquals(1, result.size());
        assertEquals("Orsted expands offshore wind capacity", result.get(0).title());
        assertEquals("https://example.com/orsted-wind", result.get(0).url());
        assertEquals("Reuters", result.get(0).sourceName());
        assertEquals("Orsted announces major offshore wind expansion in the North Sea.", result.get(0).content());
        verify(restClient, times(1)).get();
    }

    @Test
    void fetchNewsForSymbol_fallsBackToTickerOnly_whenTopicFilteredFeedIsEmpty() {
        stubRestClientSequence(
                Map.of("feed", List.of()),
                Map.of("feed", List.of(sampleArticle()))
        );

        List<NewsArticle> result = provider.fetchNewsForSymbol("ORSTED", "Orsted A/S");

        assertEquals(1, result.size());
        assertEquals("https://example.com/orsted-wind", result.get(0).url());
        verify(restClient, times(2)).get();
    }

    @Test
    void fetchNewsForSymbol_triesAlias_whenPrimarySymbolHasNoFeed() {
        stubRestClientSequence(
                Map.of("feed", List.of()), // BEP + topics
                Map.of("feed", List.of()), // BEP ticker-only
                Map.of("feed", List.of()), // BEPC + topics
                Map.of("feed", List.of(sampleArticle())) // BEPC ticker-only
        );

        List<NewsArticle> result = provider.fetchNewsForSymbol("BEP", "Brookfield Renewable");

        assertEquals(1, result.size());
        assertEquals("https://example.com/orsted-wind", result.get(0).url());
        verify(restClient, times(4)).get();
    }

    @Test
    void fetchNewsForSymbol_parsesPublishedDate_correctly() {
        stubRestClient(Map.of("feed", List.of(sampleArticle())));

        List<NewsArticle> result = provider.fetchNewsForSymbol("ORSTED", "Orsted A/S");

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

        List<NewsArticle> result = provider.fetchNewsForSymbol("ORSTED", "Orsted A/S");

        assertEquals(LocalDate.now(), result.get(0).publishedAt());
    }

    @Test
    void fetchNewsForSymbol_returnsEmpty_whenFeedKeyMissing() {
        stubRestClient(Map.of("Information", "API limit reached"));

        assertTrue(provider.fetchNewsForSymbol("AAPL", "Apple Inc.").isEmpty());
        verify(restClient, times(2)).get();
    }

    @Test
    void fetchNewsForSymbol_returnsEmpty_whenResponseIsNull() {
        stubRestClient(null);

        assertTrue(provider.fetchNewsForSymbol("AAPL", "Apple Inc.").isEmpty());
        verify(restClient, times(2)).get();
    }

    @Test
    void fetchNewsForSymbol_returnsEmpty_whenApiThrows() {
        stubRestClientThrows(new RuntimeException("timeout"));

        assertTrue(provider.fetchNewsForSymbol("AAPL", "Apple Inc.").isEmpty());
        verify(restClient, times(2)).get();
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
