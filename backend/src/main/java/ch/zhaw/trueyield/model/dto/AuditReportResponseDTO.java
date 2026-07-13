package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import ch.zhaw.trueyield.model.AiAnalysisMetadata;

@Data
@AllArgsConstructor
public class AuditReportResponseDTO {

    private String id;
    private String portfolioId;
    private String portfolioName;
    private AuditStatus auditStatus;
    private String aiRiskSummary;
    private Integer aiRiskScore;
    private String aiRiskRationale;
    private Double aiTrainingSentiment;
    private AiAnalysisMetadata aiAnalysisMetadata;
    private String auditorId;
    private LocalDateTime createdAt;

    /** Backwards-compatible constructor for existing API consumers and fixtures. */
    public AuditReportResponseDTO(String id, String portfolioId, String portfolioName, AuditStatus auditStatus,
            String aiRiskSummary, Integer aiRiskScore, String aiRiskRationale, Double aiTrainingSentiment,
            String auditorId, LocalDateTime createdAt) {
        this(id, portfolioId, portfolioName, auditStatus, aiRiskSummary, aiRiskScore, aiRiskRationale,
                aiTrainingSentiment, null, auditorId, createdAt);
    }

    public static AuditReportResponseDTO fromEntity(AuditReport report) {
        return new AuditReportResponseDTO(
                report.getId(),
                report.getPortfolioId(),
                report.getPortfolioName(),
                report.getAuditStatus(),
                report.getAiRiskSummary(),
                report.getAiRiskScore(),
                report.getAiRiskRationale(),
                report.getAiTrainingSentiment(),
                report.getAiAnalysisMetadata(),
                report.getAuditorId(),
                report.getCreatedAt()
        );
    }
}
