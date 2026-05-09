package ch.zhaw.trueyield.service.provider;

import java.util.List;

public interface NewsProvider {
    /**
     * Fetches ESG-relevant news articles by company name (text search).
     */
    List<NewsArticle> fetchNewsForHolding(String companyName);

    /**
     * Fetches ESG-relevant news articles by ticker symbol.
     * Providers that support symbol-based search override this method.
     * Default falls back to name-based search.
     */
    default List<NewsArticle> fetchNewsForSymbol(String symbol, String companyName) {
        return fetchNewsForHolding(companyName);
    }

    /**
     * @return true if the provider is properly configured with an API key.
     */
    boolean isConfigured();

    /**
     * @return Name of the provider for logging/identification.
     */
    String getProviderName();
}
