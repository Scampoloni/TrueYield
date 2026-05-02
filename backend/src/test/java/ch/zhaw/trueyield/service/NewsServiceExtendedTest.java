package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.service.provider.NewsdataIoProvider;
import ch.zhaw.trueyield.service.provider.NewsArticle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewsServiceExtendedTest {

    // ── NewsdataIoProvider ───────────────────────────────────────────────────

    @Test
    void newsdataProvider_isNotConfigured_whenApiKeyIsBlank() {
        NewsdataIoProvider provider = new NewsdataIoProvider("https://newsdata.io/api/1", "");
        assertFalse(provider.isConfigured());
    }

    @Test
    void newsdataProvider_isConfigured_whenApiKeyIsPresent() {
        NewsdataIoProvider provider = new NewsdataIoProvider("https://newsdata.io/api/1", "test-key");
        assertTrue(provider.isConfigured());
    }

    @Test
    void newsdataProvider_returnsEmpty_whenNotConfigured() {
        NewsdataIoProvider provider = new NewsdataIoProvider("https://newsdata.io/api/1", "");
        List<NewsArticle> result = provider.fetchNewsForHolding("Volkswagen AG");
        assertTrue(result.isEmpty());
    }

    @Test
    void newsdataProvider_returnsProviderName() {
        NewsdataIoProvider provider = new NewsdataIoProvider("https://newsdata.io/api/1", "key");
        assertEquals("Newsdata.io", provider.getProviderName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Apple Inc.", "BASF SE", "Nestlé AG", "BP PLC", "Siemens Corp."})
    void newsdataProvider_returnsEmpty_forUnconfiguredProvider_withAnySuffix(String companyName) {
        NewsdataIoProvider provider = new NewsdataIoProvider("https://newsdata.io/api/1", "");
        assertTrue(provider.fetchNewsForHolding(companyName).isEmpty());
    }
}
