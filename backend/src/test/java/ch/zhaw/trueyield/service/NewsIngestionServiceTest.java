package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import ch.zhaw.trueyield.service.provider.NewsArticle;
import ch.zhaw.trueyield.service.provider.NewsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsIngestionServiceTest {

    @Mock
    private NewsProvider mockProvider;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @InjectMocks
    private NewsIngestionService newsIngestionService;

    private static final String HOLDING_ID = "holding-001";
    private static final String COMPANY = "Nestlé SA";

    @BeforeEach
    void setUp() {
        List<NewsProvider> providers = new ArrayList<>();
        providers.add(mockProvider);
        ReflectionTestUtils.setField(newsIngestionService, "newsProviders", providers);
        
        lenient().when(aiAnalysisService.isAvailable()).thenReturn(true);
        lenient().when(aiAnalysisService.analyzeSentiment(anyString())).thenReturn(0.5);
        lenient().when(aiAnalysisService.analyzeRelevance(anyString(), anyString())).thenReturn(0.8);
        lenient().when(evidenceRepository.existsBySourceUrl(anyString())).thenReturn(false);
        lenient().when(mockProvider.getProviderName()).thenReturn("MockProvider");
    }

    @Test
    void ingestNewsForHolding_skips_whenProviderNotConfigured() {
        when(mockProvider.isConfigured()).thenReturn(false);

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(mockProvider, never()).fetchNewsForHolding(any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void ingestNewsForHolding_savesEvidence_whenArticlesReturned() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("ESG report positive", "Good ESG news", "https://example.com/1", LocalDate.now(), "Reuters"),
                new NewsArticle("Carbon neutral goal", "Net zero pledge", "https://example.com/2", LocalDate.now(), "Blog")
        ));
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        ArgumentCaptor<Evidence> captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository, times(2)).save(captor.capture());
        List<Evidence> saved = captor.getAllValues();
        assertEquals(HOLDING_ID, saved.get(0).getHoldingId());
        assertEquals("https://example.com/1", saved.get(0).getSourceUrl());
        assertEquals(0.5, saved.get(0).getAiSentimentScore());
    }

    @Test
    void ingestNewsForHolding_skipsDuplicateUrl() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Duplicate article", "Content", "https://example.com/dup", LocalDate.now(), "Mock")
        ));
        when(evidenceRepository.existsBySourceUrl("https://example.com/dup")).thenReturn(true);

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void ingestNewsForHolding_savesWithNeutralSentiment_whenAiUnavailable() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Some news", "Content", "https://example.com/3", LocalDate.now(), "Mock")
        ));
        when(aiAnalysisService.isAvailable()).thenReturn(false);
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        ArgumentCaptor<Evidence> captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(captor.capture());
        assertEquals(0.0, captor.getValue().getAiSentimentScore());
    }

    @Test
    void ingestNewsForHolding_skipsIrrelevantArticle() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Some generic news", "Content", "https://example.com/4", LocalDate.now(), "Mock")
        ));
        when(aiAnalysisService.analyzeRelevance(eq(COMPANY), anyString())).thenReturn(0.05);

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void ingestNewsForHolding_skipsArticleWithNullOrBlankUrl() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Title", "Content", null, LocalDate.now(), "Mock"),
                new NewsArticle("Title2", "Content2", "   ", LocalDate.now(), "Mock")
        ));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void ingestNewsForHolding_usesTitleAsSnippet_whenContentIsBlank() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Headline used as snippet", "", "https://example.com/5", LocalDate.now(), "Reuters")
        ));
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        ArgumentCaptor<Evidence> captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(captor.capture());
        assertEquals("Headline used as snippet", captor.getValue().getContentSnippet());
    }

    @Test
    void ingestNewsForHolding_dampensSentiment_forNonPremiumSource() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Some news", "Content", "https://example.com/6", LocalDate.now(), "SomeBlog")
        ));
        when(aiAnalysisService.analyzeSentiment(anyString())).thenReturn(0.8);
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        ArgumentCaptor<Evidence> captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(captor.capture());
        assertEquals(0.4, captor.getValue().getAiSentimentScore(), 0.001);
    }

    @Test
    void ingestNewsForHolding_doesNotDampenSentiment_forPremiumSource() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Reuters article", "Content", "https://reuters.com/1", LocalDate.now(), "Reuters")
        ));
        when(aiAnalysisService.analyzeSentiment(anyString())).thenReturn(0.8);
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        ArgumentCaptor<Evidence> captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(captor.capture());
        assertEquals(0.8, captor.getValue().getAiSentimentScore(), 0.001);
    }

    @Test
    void ingestNewsForHolding_continuesGracefully_whenProviderThrows() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenThrow(new RuntimeException("API down"));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void ingestNewsForHolding_savesWithZeroSentiment_whenSentimentThrows() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("News", "Content", "https://example.com/7", LocalDate.now(), "Reuters")
        ));
        when(aiAnalysisService.analyzeSentiment(anyString())).thenThrow(new RuntimeException("AI error"));
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        ArgumentCaptor<Evidence> captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(captor.capture());
        assertEquals(0.0, captor.getValue().getAiSentimentScore(), 0.001);
    }

    @Test
    void ingestNewsForHolding_skipsAll_whenEvidenceCapReached() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("New ESG article", "Content", "https://example.com/new", LocalDate.now(), "Reuters")
        ));
        when(evidenceRepository.countByHoldingId(HOLDING_ID))
                .thenReturn((long) NewsIngestionService.MAX_EVIDENCE_PER_HOLDING);

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void ingestNewsForHolding_savesOnlyTopNByRelevance_whenCapPartiallyFilled() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Low relevance",  "Content", "https://example.com/a", LocalDate.now(), "Mock"),
                new NewsArticle("High relevance", "Content", "https://example.com/b", LocalDate.now(), "Mock"),
                new NewsArticle("Mid relevance",  "Content", "https://example.com/c", LocalDate.now(), "Mock")
        ));
        // 9 existing → only 1 slot remaining
        when(evidenceRepository.countByHoldingId(HOLDING_ID)).thenReturn(9L);
        when(aiAnalysisService.analyzeRelevance(eq(COMPANY), anyString()))
                .thenReturn(0.4)  // a
                .thenReturn(0.9)  // b
                .thenReturn(0.6); // c
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        // Only 1 article saved (the highest-relevance one: b)
        ArgumentCaptor<Evidence> captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository, times(1)).save(captor.capture());
        assertEquals("https://example.com/b", captor.getValue().getSourceUrl());
    }

    @Test
    void ingestNewsForHolding_returnsEarly_whenNoCandidatesAfterDedup() {
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Already saved", "Content", "https://example.com/existing", LocalDate.now(), "Mock")
        ));
        when(evidenceRepository.existsBySourceUrl("https://example.com/existing"))
                .thenReturn(true);

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(aiAnalysisService, never()).analyzeRelevance(any(), any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void ingestNewsForHolding_deduplicatesAcrossProviders() {
        NewsProvider secondProvider = mock(NewsProvider.class);
        when(secondProvider.getProviderName()).thenReturn("SecondProvider");
        when(secondProvider.isConfigured()).thenReturn(true);
        ReflectionTestUtils.setField(newsIngestionService, "newsProviders",
                List.of(mockProvider, secondProvider));

        String sharedUrl = "https://example.com/shared";
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Article", "Content", sharedUrl, LocalDate.now(), "Mock")));
        when(secondProvider.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsArticle("Article", "Content", sharedUrl, LocalDate.now(), "Mock")));
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(evidenceRepository, times(1)).save(any());
    }
}
