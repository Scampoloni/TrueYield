package ch.zhaw.trueyield.service;

import java.util.List;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.AuditReportAggregationDTO;
import ch.zhaw.trueyield.model.dto.StateChangeDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditReportService {

    @Autowired
    private AuditReportRepository auditReportRepository;

    @Autowired
    private PortfolioService portfolioService;

    public AuditReport assignAuditReport(StateChangeDTO dto) {
        AuditReport report = auditReportRepository.findById(dto.getAuditReportId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + dto.getAuditReportId()));
        if (report.getAuditStatus() != AuditStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditReport must be in PENDING_REVIEW state");
        }
        report.setAuditStatus(AuditStatus.UNDER_REVIEW);
        report.setAuditorId(dto.getAuditorId());
        return auditReportRepository.save(report);
    }

    public AuditReport completeAuditReport(StateChangeDTO dto) {
        AuditReport report = auditReportRepository.findById(dto.getAuditReportId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + dto.getAuditReportId()));
        if (report.getAuditStatus() != AuditStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditReport must be in UNDER_REVIEW state");
        }
        if (!dto.getAuditorId().equals(report.getAuditorId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditorId does not match assigned auditor");
        }
        report.setAuditStatus(AuditStatus.APPROVED);
        return auditReportRepository.save(report);
    }

    public List<AuditReportAggregationDTO> getAuditReportDashboard(String portfolioId) {
        if (!portfolioService.portfolioExists(portfolioId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Portfolio not found: " + portfolioId);
        }
        return auditReportRepository.aggregateByPortfolioId(portfolioId);
    }
}
