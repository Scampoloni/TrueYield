package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import ch.zhaw.trueyield.service.provider.NewsProvider;
import ch.zhaw.trueyield.service.provider.NewsArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NewsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(NewsIngestionService.class);

    @Autowired
    private List<NewsProvider> newsProviders;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired(required = false)
    private AiAnalysisService aiAnalysisService;

    @Async
    public void ingestNewsForHolding(String holdingId, String companyName) {
        log.info("NewsIngestion: starting multi-provider news fetch for '{}'", companyName);
        
        List<NewsArticle> allArticles = new ArrayList<>();
        
        // 1. Fetch from all configured providers
        for (NewsProvider provider : newsProviders) {
            if (provider.isConfigured()) {
                try {
                    List<NewsArticle> articles = provider.fetchNewsForHolding(companyName);
                    allArticles.addAll(articles);
                    log.debug("NewsIngestion: {} returned {} articles for '{}'", provider.getProviderName(), articles.size(), companyName);
                } catch (Exception e) {
                    log.error("NewsIngestion: provider {} failed for '{}': {}", provider.getProviderName(), companyName, e.getMessage());
                }
            } else {
                log.debug("NewsIngestion: provider {} is not configured", provider.getClass().getSimpleName());
            }
        }
        
        // 2. Process and deduplicate
        int saved = 0;
        Set<String> seenUrls = new HashSet<>();
        
        for (NewsArticle article : allArticles) {
            String url = article.url();
            if (url == null || url.isBlank() || !seenUrls.add(url)) {
                continue; // Deduplicate within this run
            }
            if (evidenceRepository.existsByHoldingIdAndSourceUrl(holdingId, url)) {
                continue; // Deduplicate against DB
            }
            
            String snippet = article.content() == null || article.content().isBlank() ? article.title() : article.content();
            
            // 3. Relevance Filter
            if (aiAnalysisService != null && aiAnalysisService.isAvailable()) {
                double relevance = aiAnalysisService.analyzeRelevance(companyName, snippet);
                if (relevance < 0.5) {
                    log.info("NewsIngestion: skipped article '{}' due to low relevance score ({})", article.title(), relevance);
                    continue;
                }
            }
            
            // 4. Sentiment and Source Weighting
            double sentiment = 0.0;
            try {
                if (aiAnalysisService != null && aiAnalysisService.isAvailable()) {
                    sentiment = aiAnalysisService.analyzeSentiment(snippet);
                    
                    // Apply source weighting
                    String source = article.sourceName() != null ? article.sourceName().toLowerCase() : "";
                    if (!isPremiumSource(source)) {
                        sentiment = sentiment * 0.5; // Dampen sentiment for generic sources
                    }
                }
            } catch (Exception e) {
                log.warn("NewsIngestion: sentiment analysis failed for '{}': {}", article.title(), e.getMessage());
            }
            
            Evidence evidence = new Evidence(holdingId);
            evidence.setSourceUrl(url);
            evidence.setContentSnippet(snippet);
            evidence.setAiSentimentScore(sentiment);
            evidence.setSourceName(article.sourceName());
            evidence.setPublishedAt(article.publishedAt() != null ? article.publishedAt() : LocalDate.now());
            evidenceRepository.save(evidence);
            saved++;
        }
        
        log.info("NewsIngestion: saved {} new evidence entries for holding '{}' ({})", saved, sanitize(holdingId), sanitize(companyName));
    }
    
    private boolean isPremiumSource(String sourceName) {
        if (sourceName == null) return false;
        return sourceName.contains("reuters") || 
               sourceName.contains("bloomberg") || 
               sourceName.contains("financial times") || 
               sourceName.contains("wall street journal") ||
               sourceName.contains("the guardian");
    }

    private String sanitize(String value) {
        if (value == null) return "null";
        return value.replaceAll("[\r\n\t]", "_");
    }
}
