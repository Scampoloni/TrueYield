package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.service.provider.GuardianNewsProvider;
import ch.zhaw.trueyield.service.provider.NewsApiOrgProvider;
import ch.zhaw.trueyield.service.provider.NewsArticle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewsServiceTest {

    // ── GuardianNewsProvider ─────────────────────────────────────────────────

    @Test
    void guardianProvider_isNotConfigured_whenApiKeyIsBlank() {
        GuardianNewsProvider provider = new GuardianNewsProvider(
                "https://content.guardianapis.com", "");
        assertFalse(provider.isConfigured());
    }

    @Test
    void guardianProvider_isConfigured_whenApiKeyIsPresent() {
        GuardianNewsProvider provider = new GuardianNewsProvider(
                "https://content.guardianapis.com", "test-key");
        assertTrue(provider.isConfigured());
    }

    @Test
    void guardianProvider_returnsEmpty_whenNotConfigured() {
        GuardianNewsProvider provider = new GuardianNewsProvider(
                "https://content.guardianapis.com", "");
        List<NewsArticle> result = provider.fetchNewsForHolding("Apple Inc.");
        assertTrue(result.isEmpty());
    }

    @Test
    void guardianProvider_returnsProviderName() {
        GuardianNewsProvider provider = new GuardianNewsProvider(
                "https://content.guardianapis.com", "key");
        assertEquals("The Guardian", provider.getProviderName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Apple Inc.", "Shell PLC", "BASF SE", "Nestlé AG", "Unilever Ltd."})
    void guardianProvider_returnsEmpty_forUnconfiguredProvider_withAnySuffix(String companyName) {
        GuardianNewsProvider provider = new GuardianNewsProvider(
                "https://content.guardianapis.com", "");
        assertTrue(provider.fetchNewsForHolding(companyName).isEmpty());
    }

    // ── NewsApiOrgProvider ───────────────────────────────────────────────────

    @Test
    void newsApiOrgProvider_isNotConfigured_whenApiKeyIsBlank() {
        NewsApiOrgProvider provider = new NewsApiOrgProvider("https://newsapi.org/v2", "");
        assertFalse(provider.isConfigured());
    }

    @Test
    void newsApiOrgProvider_isConfigured_whenApiKeyIsPresent() {
        NewsApiOrgProvider provider = new NewsApiOrgProvider("https://newsapi.org/v2", "test-key");
        assertTrue(provider.isConfigured());
    }

    @Test
    void newsApiOrgProvider_returnsEmpty_whenNotConfigured() {
        NewsApiOrgProvider provider = new NewsApiOrgProvider("https://newsapi.org/v2", "");
        assertTrue(provider.fetchNewsForHolding("Tesla").isEmpty());
    }

    @Test
    void newsApiOrgProvider_returnsProviderName() {
        NewsApiOrgProvider provider = new NewsApiOrgProvider("https://newsapi.org/v2", "key");
        assertEquals("NewsAPI.org", provider.getProviderName());
    }
}
