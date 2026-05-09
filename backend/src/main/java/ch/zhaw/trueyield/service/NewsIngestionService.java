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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NewsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(NewsIngestionService.class);
    static final int MAX_EVIDENCE_PER_HOLDING = 10;
    private static final double RELEVANCE_THRESHOLD = 0.45;

    @Autowired
    private List<NewsProvider> newsProviders;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired(required = false)
    private AiAnalysisService aiAnalysisService;

    private record ScoredArticle(NewsArticle article, String snippet, double relevance) {}

    @Async
    public void ingestNewsForHolding(String holdingId, String companyName) {
        log.info("NewsIngestion: starting multi-provider news fetch for '{}'", sanitize(companyName));

        // 1. Fetch from all configured providers
        List<NewsArticle> allArticles = new ArrayList<>();
        for (NewsProvider provider : newsProviders) {
            if (provider.isConfigured()) {
                try {
                    List<NewsArticle> articles = provider.fetchNewsForHolding(companyName);
                    allArticles.addAll(articles);
                    log.info("NewsIngestion: {} returned {} articles for '{}'", provider.getProviderName(), articles.size(), sanitize(companyName));
                } catch (Exception e) {
                    log.error("NewsIngestion: provider {} failed for '{}': {}", provider.getProviderName(), sanitize(companyName), e.getMessage(), e);
                }
            } else {
                log.warn("NewsIngestion: provider {} is NOT configured (missing API key)", provider.getClass().getSimpleName());
            }
        }

        // 2. URL dedup (in-memory + DB — global across all holdings to avoid same article on multiple holdings)
        Set<String> seenUrls = new HashSet<>();
        List<NewsArticle> candidates = new ArrayList<>();
        for (NewsArticle article : allArticles) {
            String url = article.url();
            if (url == null || url.isBlank() || !seenUrls.add(url)) continue;
            if (evidenceRepository.existsBySourceUrl(url)) continue;
            candidates.add(article);
        }
        log.info("NewsIngestion: {} unique new candidates after dedup for '{}'", candidates.size(), sanitize(companyName));

        if (candidates.isEmpty()) {
            log.info("NewsIngestion: no new candidates for '{}', exiting", sanitize(companyName));
            return;
        }

        // 3. ESG relevance scoring — score all candidates, drop below threshold
        List<ScoredArticle> scored = new ArrayList<>();
        for (NewsArticle article : candidates) {
            String snippet = article.content() == null || article.content().isBlank()
                    ? article.title() : article.content();
            double relevance;
            if (aiAnalysisService != null && aiAnalysisService.isAvailable()) {
                relevance = aiAnalysisService.analyzeRelevance(companyName, snippet);
                if (relevance < RELEVANCE_THRESHOLD) {
                    log.info("NewsIngestion: skipped '{}' — ESG relevance {}", article.title(), relevance);
                    continue;
                }
                log.info("NewsIngestion: '{}' passed ESG filter (relevance {})", article.title(), relevance);
            } else {
                relevance = 1.0; // assume relevant when AI unavailable
            }
            scored.add(new ScoredArticle(article, snippet, relevance));
        }

        // 4. Respect per-holding cap — keep only the top-N most relevant
        long existing = evidenceRepository.countByHoldingId(holdingId);
        int remaining = (int) Math.max(0, MAX_EVIDENCE_PER_HOLDING - existing);
        if (remaining == 0) {
            log.info("NewsIngestion: holding '{}' already at evidence cap ({}), skipping", sanitize(holdingId), MAX_EVIDENCE_PER_HOLDING);
            return;
        }
        List<ScoredArticle> topCandidates = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredArticle::relevance).reversed())
                .limit(remaining)
                .toList();
        log.info("NewsIngestion: saving top {} articles for '{}' (cap {}, existing {})",
                topCandidates.size(), sanitize(companyName), MAX_EVIDENCE_PER_HOLDING, existing);

        // 5. Sentiment analysis + source weighting + save
        int saved = 0;
        for (ScoredArticle sc : topCandidates) {
            double sentiment = 0.0;
            try {
                if (aiAnalysisService != null && aiAnalysisService.isAvailable()) {
                    sentiment = aiAnalysisService.analyzeSentiment(sc.snippet());
                    String source = sc.article().sourceName() != null
                            ? sc.article().sourceName().toLowerCase() : "";
                    if (!isPremiumSource(source)) {
                        sentiment = sentiment * 0.5;
                    }
                }
            } catch (Exception e) {
                log.warn("NewsIngestion: sentiment analysis failed for '{}': {}", sc.article().title(), e.getMessage());
            }

            Evidence evidence = new Evidence(holdingId);
            evidence.setSourceUrl(sc.article().url());
            evidence.setContentSnippet(sc.snippet());
            evidence.setAiSentimentScore(sentiment);
            evidence.setSourceName(sc.article().sourceName());
            evidence.setPublishedAt(sc.article().publishedAt() != null ? sc.article().publishedAt() : LocalDate.now());
            evidenceRepository.save(evidence);
            saved++;
            log.info("NewsIngestion: saved evidence for holding '{}' — url: {}", sanitize(holdingId), sanitize(sc.article().url()));
        }

        log.info("NewsIngestion: saved {} new evidence entries for holding '{}' ({})",
                saved, sanitize(holdingId), sanitize(companyName));
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
