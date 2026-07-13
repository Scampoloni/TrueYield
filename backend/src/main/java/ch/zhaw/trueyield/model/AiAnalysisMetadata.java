package ch.zhaw.trueyield.model;

import ch.zhaw.trueyield.model.enums.AnalysisState;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AiAnalysisMetadata {
    private AnalysisState analysisState;
    private String modelId;
    private String promptVersion;
    private LocalDateTime analyzedAt;
    private int inputEvidenceCount;
    private List<String> citedEvidenceIds = List.of();
    private double evidenceCoverage;
    private String fallbackReason;
}
