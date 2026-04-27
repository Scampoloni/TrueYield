package ch.zhaw.trueyield.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceExtendedTest {

    private static final String BASE_URL = "https://content.guardianapis.com";

    @SuppressWarnings({"unchecked", "rawtypes"})
    private RestClient buildMockRestClientReturning(Map<String, Object> responseBody) {
        RestClient mockClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(mockClient.get()
                .uri((Function) any())
                .retrieve()
                .body(Map.class))
                .thenReturn(responseBody);
        return mockClient;
    }

    private Map<String, Object> guardianResponse(List<Map<String, Object>> results) {
        Map<String, Object> inner = new HashMap<>();
        inner.put("results", results);
        Map<String, Object> outer = new HashMap<>();
        outer.put("response", inner);
        return outer;
    }

    private Map<String, Object> article(String title, String url, String trailText) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("trailText", trailText);
        Map<String, Object> raw = new HashMap<>();
        raw.put("webTitle", title);
        raw.put("webUrl", url);
        raw.put("fields", fields);
        return raw;
    }

    // ── fetchNewsForHolding — configured, API returns articles ────────────────

    @Test
    void fetchNewsForHolding_returnsArticles_whenApiRespondsWithData() {
        NewsService service = new NewsService(BASE_URL, "test-api-key");

        Map<String, Object> art = article("ESG Report", "https://guardian.com/1", "Trail text here");
        RestClient mockClient = buildMockRestClientReturning(guardianResponse(List.of(art)));
        ReflectionTestUtils.setField(service, "restClient", mockClient);

        List<NewsService.NewsArticle> results = service.fetchNewsForHolding("Apple");

        assertEquals(1, results.size());
        assertEquals("ESG Report", results.get(0).title());
        assertEquals("https://guardian.com/1", results.get(0).url());
        assertEquals("Trail text here", results.get(0).content());
        assertNotNull(results.get(0).publishedAt());
    }

    @Test
    void fetchNewsForHolding_articleWithNoFields_usesTitleAsContent() {
        NewsService service = new NewsService(BASE_URL, "test-api-key");

        Map<String, Object> raw = new HashMap<>();
        raw.put("webTitle", "Headline Only");
        raw.put("webUrl", "https://guardian.com/2");
        // no "fields" key

        RestClient mockClient = buildMockRestClientReturning(guardianResponse(List.of(raw)));
        ReflectionTestUtils.setField(service, "restClient", mockClient);

        List<NewsService.NewsArticle> results = service.fetchNewsForHolding("Tesla");
        assertEquals(1, results.size());
        assertEquals("Headline Only", results.get(0).content());
    }

    @Test
    void fetchNewsForHolding_returnsEmptyList_whenApiReturnsNull() {
        NewsService service = new NewsService(BASE_URL, "test-api-key");

        RestClient mockClient = buildMockRestClientReturning(null);
        ReflectionTestUtils.setField(service, "restClient", mockClient);

        List<NewsService.NewsArticle> results = service.fetchNewsForHolding("Apple");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void fetchNewsForHolding_returnsEmptyList_whenResponseMissingResponseKey() {
        NewsService service = new NewsService(BASE_URL, "test-api-key");

        RestClient mockClient = buildMockRestClientReturning(Map.of("status", "ok"));
        ReflectionTestUtils.setField(service, "restClient", mockClient);

        List<NewsService.NewsArticle> results = service.fetchNewsForHolding("Apple");
        assertTrue(results.isEmpty());
    }

    @Test
    void fetchNewsForHolding_returnsEmptyList_whenResultsIsNull() {
        NewsService service = new NewsService(BASE_URL, "test-api-key");

        Map<String, Object> inner = new HashMap<>();
        inner.put("results", null);
        Map<String, Object> outer = Map.of("response", inner);

        RestClient mockClient = buildMockRestClientReturning(outer);
        ReflectionTestUtils.setField(service, "restClient", mockClient);

        List<NewsService.NewsArticle> results = service.fetchNewsForHolding("Apple");
        assertTrue(results.isEmpty());
    }

    @Test
    void fetchNewsForHolding_returnsEmptyList_whenApiThrowsException() {
        NewsService service = new NewsService(BASE_URL, "test-api-key");

        @SuppressWarnings({"unchecked", "rawtypes"})
        RestClient mockClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(mockClient.get()
                .uri((Function) any())
                .retrieve()
                .body(Map.class))
                .thenThrow(new RuntimeException("Connection refused"));
        ReflectionTestUtils.setField(service, "restClient", mockClient);

        List<NewsService.NewsArticle> results = service.fetchNewsForHolding("Apple");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ── fallback search: company with multiple words ──────────────────────────

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fetchNewsForHolding_usesFirstWordFallback_whenExactSearchReturnsEmpty() {
        NewsService service = new NewsService(BASE_URL, "test-api-key");

        // First call (exact name) returns empty; second call (first word) returns an article
        Map<String, Object> fallbackArt = article("Green Fund ESG", "https://guardian.com/3", "Trail");

        RestClient mockClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(mockClient.get()
                .uri((Function) any())
                .retrieve()
                .body(Map.class))
                .thenReturn(guardianResponse(List.of()))   // first call: exact → empty
                .thenReturn(guardianResponse(List.of(fallbackArt)));  // second call: firstWord → result
        ReflectionTestUtils.setField(service, "restClient", mockClient);

        // "Green Energy" → cleanName = "Green Energy", firstWord = "Green" → fallback
        List<NewsService.NewsArticle> results = service.fetchNewsForHolding("Green Energy");
        assertEquals(1, results.size());
        assertEquals("Green Fund ESG", results.get(0).title());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fetchNewsForHolding_doesNotFallback_whenFirstWordEqualsCleanName() {
        NewsService service = new NewsService(BASE_URL, "test-api-key");

        RestClient mockClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(mockClient.get()
                .uri((Function) any())
                .retrieve()
                .body(Map.class))
                .thenReturn(guardianResponse(List.of()));  // only one call
        ReflectionTestUtils.setField(service, "restClient", mockClient);

        // "Apple Inc." → cleanName = "Apple" → firstWord = "Apple" → same → no fallback
        List<NewsService.NewsArticle> results = service.fetchNewsForHolding("Apple Inc.");
        assertTrue(results.isEmpty());
        // verify only one HTTP call was made (no fallback)
        verify(mockClient.get().uri((Function) any()).retrieve(), times(1)).body(Map.class);
    }

    // ── mapArticle: trailText fallback when fields key is absent ─────────────

    @Test
    void fetchNewsForHolding_articleFieldsKeyAbsent_usesTitleAsContent() {
        NewsService service = new NewsService(BASE_URL, "test-api-key");

        Map<String, Object> fields = new HashMap<>();
        // "trailText" key is absent → getOrDefault returns title
        Map<String, Object> raw = new HashMap<>();
        raw.put("webTitle", "Article Title");
        raw.put("webUrl", "https://url.com");
        raw.put("fields", fields);

        RestClient mockClient = buildMockRestClientReturning(guardianResponse(List.of(raw)));
        ReflectionTestUtils.setField(service, "restClient", mockClient);

        List<NewsService.NewsArticle> results = service.fetchNewsForHolding("Apple");
        assertEquals(1, results.size());
        // trailText key absent → getOrDefault(key, title) returns title
        assertEquals("Article Title", results.get(0).content());
    }

    // ── isConfigured already covered by NewsServiceTest; repeat key cases ─────

    @Test
    void isConfigured_withValidKey_returnsTrue() {
        NewsService service = new NewsService(BASE_URL, "my-key");
        assertTrue(service.isConfigured());
    }

    @Test
    void isConfigured_withBlankKey_returnsFalse() {
        NewsService service = new NewsService(BASE_URL, "  ");
        assertFalse(service.isConfigured());
    }
}
