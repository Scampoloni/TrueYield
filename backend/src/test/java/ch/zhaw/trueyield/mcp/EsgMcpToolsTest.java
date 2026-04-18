package ch.zhaw.trueyield.mcp;

import ch.zhaw.trueyield.service.AiAnalysisService;
import ch.zhaw.trueyield.service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EsgMcpToolsTest {

    @Mock
    private AiAnalysisService aiAnalysisService;

    @Mock
    private NewsService newsService;

    private EsgMcpTools esgMcpTools;

    @BeforeEach
    void setUp() {
        esgMcpTools = new EsgMcpTools(aiAnalysisService, newsService);
    }

    // ── generateEsgRiskSummary ───────────────────────────────────────────────

    @Test
    void generateEsgRiskSummary_delegatesToAiService() {
        when(aiAnalysisService.generateRiskSummary(anyList()))
                .thenReturn("High ESG risk for Tesla due to labor concerns.");

        String result = esgMcpTools.generateEsgRiskSummary("Tesla, Apple Inc.");

        assertNotNull(result);
        assertEquals("High ESG risk for Tesla due to labor concerns.", result);
        verify(aiAnalysisService, times(1)).generateRiskSummary(List.of("Tesla", "Apple Inc."));
    }

    @Test
    void generateEsgRiskSummary_trimsAndFiltersBlankEntries() {
        when(aiAnalysisService.generateRiskSummary(anyList())).thenReturn("Summary.");

        esgMcpTools.generateEsgRiskSummary("  Apple  ,  ,  Tesla  ");

        verify(aiAnalysisService).generateRiskSummary(List.of("Apple", "Tesla"));
    }

    @Test
    void generateEsgRiskSummary_handlesEmptyInput() {
        when(aiAnalysisService.generateRiskSummary(anyList())).thenReturn("AI analysis unavailable.");

        String result = esgMcpTools.generateEsgRiskSummary("   ");

        assertNotNull(result);
        verify(aiAnalysisService).generateRiskSummary(Collections.emptyList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Shell PLC", "Volkswagen AG", "Nestlé SA", "BP", "ExxonMobil"})
    void generateEsgRiskSummary_invokesServiceForAnyCompany(String company) {
        when(aiAnalysisService.generateRiskSummary(anyList())).thenReturn("Risk summary.");

        String result = esgMcpTools.generateEsgRiskSummary(company);

        assertNotNull(result);
        verify(aiAnalysisService, atLeastOnce()).generateRiskSummary(any());
    }

    // ── analyseEsgSentiment ──────────────────────────────────────────────────

    @Test
    void analyseEsgSentiment_returnsScoreFromAiService() {
        when(aiAnalysisService.analyzeSentiment("Positive ESG report.")).thenReturn(0.8);

        double result = esgMcpTools.analyseEsgSentiment("Positive ESG report.");

        assertEquals(0.8, result, 0.001);
        verify(aiAnalysisService, times(1)).analyzeSentiment("Positive ESG report.");
    }

    @Test
    void analyseEsgSentiment_returnsZeroForNeutralText() {
        when(aiAnalysisService.analyzeSentiment(any())).thenReturn(0.0);

        double result = esgMcpTools.analyseEsgSentiment("No ESG content here.");

        assertEquals(0.0, result, 0.001);
    }

    @Test
    void analyseEsgSentiment_returnsNegativeForBadNews() {
        when(aiAnalysisService.analyzeSentiment(any())).thenReturn(-0.9);

        double result = esgMcpTools.analyseEsgSentiment("Company fined for pollution.");

        assertEquals(-0.9, result, 0.001);
    }

    // ── fetchEsgNews ─────────────────────────────────────────────────────────

    @Test
    void fetchEsgNews_returnsFormattedArticles_whenNewsFound() {
        NewsService.NewsArticle article = new NewsService.NewsArticle(
                "Shell pledges net-zero by 2050",
                "Shell announced...",
                "https://theguardian.com/shell-netzero",
                LocalDate.of(2025, 1, 15));
        when(newsService.fetchNewsForHolding("Shell")).thenReturn(List.of(article));

        String result = esgMcpTools.fetchEsgNews("Shell");

        assertTrue(result.contains("Shell pledges net-zero by 2050"));
        assertTrue(result.contains("https://theguardian.com/shell-netzero"));
        verify(newsService, times(1)).fetchNewsForHolding("Shell");
    }

    @Test
    void fetchEsgNews_returnsNoNewsMessage_whenEmpty() {
        when(newsService.fetchNewsForHolding("UnknownCorp")).thenReturn(Collections.emptyList());

        String result = esgMcpTools.fetchEsgNews("UnknownCorp");

        assertTrue(result.contains("No recent ESG news found"));
        assertTrue(result.contains("UnknownCorp"));
    }

    @Test
    void fetchEsgNews_includesAllArticles_whenMultipleFound() {
        List<NewsService.NewsArticle> articles = List.of(
                new NewsService.NewsArticle("Title 1", "Content", "https://url1", LocalDate.now()),
                new NewsService.NewsArticle("Title 2", "Content", "https://url2", LocalDate.now()),
                new NewsService.NewsArticle("Title 3", "Content", "https://url3", LocalDate.now())
        );
        when(newsService.fetchNewsForHolding("Apple")).thenReturn(articles);

        String result = esgMcpTools.fetchEsgNews("Apple");

        assertTrue(result.contains("Title 1"));
        assertTrue(result.contains("Title 2"));
        assertTrue(result.contains("Title 3"));
    }
}
