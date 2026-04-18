package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsIngestionServiceTest {

    @Mock
    private NewsService newsService;

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
        lenient().when(aiAnalysisService.isAvailable()).thenReturn(true);
        lenient().when(aiAnalysisService.analyzeSentiment(anyString())).thenReturn(0.5);
        lenient().when(evidenceRepository.existsByHoldingIdAndSourceUrl(anyString(), anyString())).thenReturn(false);
    }

    @Test
    void ingestNewsForHolding_skips_whenNewsServiceNotConfigured() {
        when(newsService.isConfigured()).thenReturn(false);

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(newsService, never()).fetchNewsForHolding(any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void ingestNewsForHolding_savesEvidence_whenArticlesReturned() {
        when(newsService.isConfigured()).thenReturn(true);
        when(newsService.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsService.NewsArticle("ESG report positive", "Good ESG news", "https://example.com/1", LocalDate.now()),
                new NewsService.NewsArticle("Carbon neutral goal", "Net zero pledge", "https://example.com/2", LocalDate.now())
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
        when(newsService.isConfigured()).thenReturn(true);
        when(newsService.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsService.NewsArticle("Duplicate article", "Content", "https://example.com/dup", LocalDate.now())
        ));
        when(evidenceRepository.existsByHoldingIdAndSourceUrl(HOLDING_ID, "https://example.com/dup")).thenReturn(true);

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void ingestNewsForHolding_savesWithNeutralSentiment_whenAiUnavailable() {
        when(newsService.isConfigured()).thenReturn(true);
        when(newsService.fetchNewsForHolding(COMPANY)).thenReturn(List.of(
                new NewsService.NewsArticle("Some news", "Content", "https://example.com/3", LocalDate.now())
        ));
        when(aiAnalysisService.isAvailable()).thenReturn(false);
        when(evidenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        ArgumentCaptor<Evidence> captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(captor.capture());
        assertEquals(0.0, captor.getValue().getAiSentimentScore());
    }

    @Test
    void ingestNewsForHolding_savesNothing_whenNoArticlesFound() {
        when(newsService.isConfigured()).thenReturn(true);
        when(newsService.fetchNewsForHolding(COMPANY)).thenReturn(List.of());

        newsIngestionService.ingestNewsForHolding(HOLDING_ID, COMPANY);

        verify(evidenceRepository, never()).save(any());
    }
}
