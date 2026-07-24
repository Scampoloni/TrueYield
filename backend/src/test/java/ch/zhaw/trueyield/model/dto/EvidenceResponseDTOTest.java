package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.Evidence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceResponseDTOTest {

    private Evidence evidence(String snippet, Double score) {
        Evidence e = new Evidence("holding-1");
        e.setContentSnippet(snippet);
        e.setSourceUrl("https://example.com");
        e.setPublishedAt(LocalDate.now());
        e.setSourceName("Reuters");
        e.setAiSentimentScore(score);
        return e;
    }

    @Test
    void fromEntity_mapsAllFields() {
        Evidence e = evidence("Good ESG news", 0.8);
        EvidenceResponseDTO dto = EvidenceResponseDTO.fromEntity(e);

        assertEquals("holding-1", dto.getHoldingId());
        assertEquals("Good ESG news", dto.getHeadline());
        assertEquals("Good ESG news", dto.getSummary());
        assertEquals("POSITIVE", dto.getSentiment());
        assertEquals("https://example.com", dto.getSourceUrl());
        assertEquals("Reuters", dto.getSourceName());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void fromEntity_truncatesHeadline_whenSnippetExceeds80Chars() {
        String longSnippet = "A".repeat(90);
        EvidenceResponseDTO dto = EvidenceResponseDTO.fromEntity(evidence(longSnippet, 0.0));

        assertEquals(83, dto.getHeadline().length()); // 80 + "..."
        assertTrue(dto.getHeadline().endsWith("..."));
        assertEquals(longSnippet, dto.getSummary());
    }

    @Test
    void fromEntity_doesNotTruncate_whenSnippetIsExactly80Chars() {
        String snippet = "B".repeat(80);
        EvidenceResponseDTO dto = EvidenceResponseDTO.fromEntity(evidence(snippet, 0.0));

        assertEquals(snippet, dto.getHeadline());
        assertFalse(dto.getHeadline().endsWith("..."));
    }

    @Test
    void fromEntity_handlesNullScore_asZero() {
        EvidenceResponseDTO dto = EvidenceResponseDTO.fromEntity(evidence("News", null));

        // null → score=0.0 → riskScore = round((1 - 0.5) * 10) = 5
        assertEquals(5, dto.getRiskScore());
        assertEquals("NEUTRAL", dto.getSentiment());
    }

    @Test
    void fromEntity_handlesNullSnippet() {
        EvidenceResponseDTO dto = EvidenceResponseDTO.fromEntity(evidence(null, 0.5));

        assertEquals("", dto.getHeadline());
        assertEquals("", dto.getSummary());
    }

    @ParameterizedTest
    @CsvSource({
        // score,  sentiment,  riskScore = round((1-(score+1)/2)*10)
        " 1.0, POSITIVE,  0",
        " 0.5, POSITIVE,  3",
        " 0.31,POSITIVE,  3",
        " 0.3, NEUTRAL,   4",
        " 0.0, NEUTRAL,   5",
        "-0.3, NEGATIVE,  7",
        "-0.31,NEGATIVE,  7",
        "-1.0, NEGATIVE, 10"
    })
    void fromEntity_mapsScoreToSentimentAndRiskScore(double score, String expectedSentiment, int expectedRisk) {
        EvidenceResponseDTO dto = EvidenceResponseDTO.fromEntity(evidence("text", score));

        assertEquals(expectedSentiment, dto.getSentiment());
        assertEquals(expectedRisk, dto.getRiskScore());
    }
}
