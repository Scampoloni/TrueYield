package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class NewsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(NewsIngestionService.class);

    @Autowired
    private NewsService newsService;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired(required = false)
    private AiAnalysisService aiAnalysisService;

    @Async
    public void ingestNewsForHolding(String holdingId, String companyName) {
        if (!newsService.isConfigured()) {
            log.debug("NewsIngestion: skipped for '{}' — NEWS_API_KEY not set", companyName);
            return;
        }
        List<NewsService.NewsArticle> articles = newsService.fetchNewsForHolding(companyName);
        int saved = 0;
        for (NewsService.NewsArticle article : articles) {
            if (article.url() != null && evidenceRepository.existsByHoldingIdAndSourceUrl(holdingId, article.url())) {
                continue;
            }
            double sentiment = 0.0;
            String snippet = article.content().isBlank() ? article.title() : article.content();
            try {
                if (aiAnalysisService != null && aiAnalysisService.isAvailable()) {
                    sentiment = aiAnalysisService.analyzeSentiment(snippet);
                }
            } catch (Exception e) {
                log.warn("NewsIngestion: sentiment analysis failed for '{}': {}", article.title(), e.getMessage());
            }
            Evidence evidence = new Evidence(holdingId);
            evidence.setSourceUrl(article.url());
            evidence.setContentSnippet(snippet);
            evidence.setAiSentimentScore(sentiment);
            evidence.setPublishedAt(article.publishedAt() != null ? article.publishedAt() : LocalDate.now());
            evidenceRepository.save(evidence);
            saved++;
        }
        log.info("NewsIngestion: saved {} evidence entries for holding '{}' ({})", saved, holdingId, companyName);
    }
}
