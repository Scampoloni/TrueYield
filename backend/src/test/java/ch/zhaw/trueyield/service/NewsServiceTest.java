package ch.zhaw.trueyield.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewsServiceTest {

    private static final String GUARDIAN_BASE = "https://content.guardianapis.com";

    // ── isConfigured ─────────────────────────────────────────────────────────

    @Test
    void isConfigured_returnsTrue_whenApiKeyIsPresent() {
        NewsService service = new NewsService(GUARDIAN_BASE, "valid-api-key");
        assertTrue(service.isConfigured());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void isConfigured_returnsFalse_whenApiKeyIsNullOrBlank(String key) {
        NewsService service = new NewsService(GUARDIAN_BASE, key);
        assertFalse(service.isConfigured());
    }

    // ── fetchNewsForHolding ───────────────────────────────────────────────────

    @Test
    void fetchNewsForHolding_returnsEmptyList_whenNotConfigured() {
        NewsService service = new NewsService(GUARDIAN_BASE, "");

        List<NewsService.NewsArticle> result = service.fetchNewsForHolding("Apple Inc.");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Apple Inc.", "Tesla", "Microsoft Corp.", "AAPL", "Nestlé AG"})
    void fetchNewsForHolding_returnsEmptyList_forAnyCompany_whenNotConfigured(String company) {
        NewsService service = new NewsService(GUARDIAN_BASE, "");

        List<NewsService.NewsArticle> result = service.fetchNewsForHolding(company);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchNewsForHolding_returnsEmptyList_whenApiKeyIsWhitespaceOnly() {
        NewsService service = new NewsService(GUARDIAN_BASE, "   ");

        List<NewsService.NewsArticle> result = service.fetchNewsForHolding("Tesla");

        assertTrue(result.isEmpty());
    }

    // ── NewsArticle record ────────────────────────────────────────────────────

    @Test
    void newsArticle_storesAllFieldsCorrectly() {
        LocalDate date = LocalDate.of(2025, 4, 18);
        NewsService.NewsArticle article = new NewsService.NewsArticle(
                "ESG Scandal at Major Bank",
                "The bank was found to have misrepresented its ESG credentials.",
                "https://theguardian.com/article/123",
                date);

        assertEquals("ESG Scandal at Major Bank", article.title());
        assertEquals("The bank was found to have misrepresented its ESG credentials.", article.content());
        assertEquals("https://theguardian.com/article/123", article.url());
        assertEquals(date, article.publishedAt());
    }

    @Test
    void newsArticle_titleAndContentCanBeIdentical() {
        LocalDate date = LocalDate.now();
        NewsService.NewsArticle article = new NewsService.NewsArticle("Headline", "Headline", "https://url.com", date);

        assertEquals(article.title(), article.content());
    }

    @Test
    void newsArticle_urlCanBeBlank() {
        NewsService.NewsArticle article = new NewsService.NewsArticle("Title", "Content", "", LocalDate.now());

        assertEquals("", article.url());
    }
}
