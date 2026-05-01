package ch.zhaw.trueyield.service.provider;

import java.util.List;

public interface NewsProvider {
    /**
     * Fetches up to MAX_ARTICLES ESG-relevant news articles for a given company name.
     */
    List<NewsArticle> fetchNewsForHolding(String companyName);

    /**
     * @return true if the provider is properly configured with an API key.
     */
    boolean isConfigured();
    
    /**
     * @return Name of the provider for logging/identification.
     */
    String getProviderName();
}
