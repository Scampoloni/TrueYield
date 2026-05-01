package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AuditReportResponseDTO {

    private String id;
    private String portfolioId;
    private String portfolioName;
    private AuditStatus auditStatus;
    private String aiRiskSummary;
    private String auditorId;
    private LocalDateTime createdAt;

    public static AuditReportResponseDTO fromEntity(AuditReport report) {
        return new AuditReportResponseDTO(
                report.getId(),
                report.getPortfolioId(),
                report.getPortfolioName(),
                report.getAuditStatus(),
                report.getAiRiskSummary(),
                report.getAuditorId(),
                report.getCreatedAt()
        );
    }
}
