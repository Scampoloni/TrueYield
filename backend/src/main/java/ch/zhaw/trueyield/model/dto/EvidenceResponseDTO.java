package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.Evidence;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class EvidenceResponseDTO {

    private String id;
    private String holdingId;
    private String headline;
    private String summary;
    private String sentiment;
    private int riskScore;
    private LocalDate createdAt;
    private String sourceUrl;
    private String sourceName;

    public static EvidenceResponseDTO fromEntity(Evidence evidence) {
        double score = evidence.getAiSentimentScore() == null ? 0.0 : evidence.getAiSentimentScore();
        String snippet = evidence.getContentSnippet() == null ? "" : evidence.getContentSnippet().trim();
        String headline = snippet.length() <= 80 ? snippet : snippet.substring(0, 80) + "...";

        return new EvidenceResponseDTO(
                evidence.getId(),
                evidence.getHoldingId(),
                headline,
                snippet,
                toSentiment(score),
                toRiskScore(score),
                evidence.getPublishedAt(),
                evidence.getSourceUrl(),
                evidence.getSourceName()
        );
    }

    private static String toSentiment(double score) {
        if (score > 0.3) {
            return "POSITIVE";
        }
        if (score > -0.3) {
            return "NEUTRAL";
        }
        return "NEGATIVE";
    }

    private static int toRiskScore(double score) {
        return (int) Math.round((1 - (score + 1) / 2) * 10);
    }
}
