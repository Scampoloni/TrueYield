package ch.zhaw.trueyield.service;

import java.time.LocalDateTime;
import java.util.List;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.AuditReportAggregationDTO;
import ch.zhaw.trueyield.model.dto.AuditReportCreateDTO;
import ch.zhaw.trueyield.model.dto.AuditReportResponseDTO;
import ch.zhaw.trueyield.model.dto.EvidenceCreateDTO;
import ch.zhaw.trueyield.model.dto.StateChangeDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.security.AccessControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditReportService {

    private static final Logger log = LoggerFactory.getLogger(AuditReportService.class);

    @Autowired
    private AuditReportRepository auditReportRepository;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private HoldingService holdingService;

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private NewsIngestionService newsIngestionService;

    @Autowired
    private AiAnalysisService aiAnalysisService;

    public AuditReport createAuditReport(AuditReportCreateDTO dto) {
        accessControlService.requireFundManagerPortfolioAccess(dto.getPortfolioId());
        AuditReport report = new AuditReport(dto.getPortfolioId(), AuditStatus.AI_ANALYZING);
        try {
            ch.zhaw.trueyield.model.Portfolio portfolio = portfolioService.getPortfolioByIdForAuditor(dto.getPortfolioId());
            report.setPortfolioName(portfolio.getName());
        } catch (Exception e) {
            log.warn("Could not load portfolio name for '{}': {}", dto.getPortfolioId(), e.getMessage());
        }
        report.setCreatedAt(LocalDateTime.now());
        report = auditReportRepository.save(report);

        List<Holding> holdings;
        try {
            holdings = holdingService.getHoldingsByPortfolioId(dto.getPortfolioId());
        } catch (Exception e) {
            log.warn("Could not load holdings for portfolio '{}': {}", dto.getPortfolioId(), e.getMessage());
            holdings = List.of();
        }
        List<String> holdingNames = holdings.stream()
                .map(h -> h.getName() != null ? h.getName() : h.getSymbol())
                .filter(n -> n != null && !n.isBlank())
                .toList();

        fetchAndStoreNewsEvidence(dto.getPortfolioId(), holdings);

        String summary;
        try {
            summary = aiAnalysisService.generateRiskSummary(holdingNames);
        } catch (Exception e) {
            log.warn("AI risk summary failed for portfolio '{}': {}", dto.getPortfolioId(), e.getMessage());
            summary = "AI analysis unavailable.";
        }
        report.setAiRiskSummary(summary);

        report.setAuditStatus(AuditStatus.PENDING_REVIEW);
        return auditReportRepository.save(report);
    }

    private static final int MAX_EVIDENCE_PER_HOLDING = 10;

    private void fetchAndStoreNewsEvidence(String portfolioId, List<Holding> holdings) {
        try {
            for (Holding holding : holdings) {
                if (evidenceService.countByHoldingId(holding.getId()) >= MAX_EVIDENCE_PER_HOLDING) {
                    continue;
                }
                String query = holding.getName() != null ? holding.getName() : holding.getSymbol();
                // This is async, so it won't block the report creation
                newsIngestionService.ingestNewsForHolding(holding.getId(), query, holding.getSymbol());
            }
        } catch (Exception e) {
            log.warn("News evidence fetch failed for portfolio '{}': {}", portfolioId, e.getMessage());
        }
    }

    public AuditReport getAuditReportById(String id) {
        AuditReport report = auditReportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "AuditReport not found: " + id));
        accessControlService.requireAuditReportAccess(report);
        return report;
    }

    public AuditReport rejectAuditReport(StateChangeDTO dto) {
        String auditorId = accessControlService.requireAuditorId();
        AuditReport report = auditReportRepository.findById(dto.getAuditReportId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + dto.getAuditReportId()));
        if (report.getAuditStatus() != AuditStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditReport must be in UNDER_REVIEW state");
        }
        if (report.getAuditorId() == null || !auditorId.equals(report.getAuditorId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditorId does not match assigned auditor");
        }
        log.info("AuditReport [{}] rejected by auditor [{}] — status: UNDER_REVIEW → REJECTED", sanitize(dto.getAuditReportId()), sanitize(auditorId));
        report.setAuditStatus(AuditStatus.REJECTED);
        return auditReportRepository.save(report);
    }

    public AuditReport assignAuditReport(StateChangeDTO dto) {
        String auditorId = accessControlService.requireAuditorId();
        AuditReport report = auditReportRepository.findById(dto.getAuditReportId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + dto.getAuditReportId()));
        if (report.getAuditStatus() != AuditStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditReport must be in PENDING_REVIEW state");
        }
        log.info("AuditReport [{}] assigned to auditor [{}] — status: PENDING_REVIEW → UNDER_REVIEW", sanitize(dto.getAuditReportId()), sanitize(auditorId));
        report.setAuditStatus(AuditStatus.UNDER_REVIEW);
        report.setAuditorId(auditorId);
        return auditReportRepository.save(report);
    }

    public AuditReport completeAuditReport(StateChangeDTO dto) {
        String auditorId = accessControlService.requireAuditorId();
        AuditReport report = auditReportRepository.findById(dto.getAuditReportId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + dto.getAuditReportId()));
        if (report.getAuditStatus() != AuditStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditReport must be in UNDER_REVIEW state");
        }
        if (report.getAuditorId() == null || !auditorId.equals(report.getAuditorId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditorId does not match assigned auditor");
        }
        log.info("AuditReport [{}] approved by auditor [{}] — status: UNDER_REVIEW → APPROVED", sanitize(dto.getAuditReportId()), sanitize(auditorId));
        report.setAuditStatus(AuditStatus.APPROVED);
        return auditReportRepository.save(report);
    }

    public List<AuditReportResponseDTO> getAuditorQueue(String auditorId) {
        return auditReportRepository
                .findByAuditStatusOrAuditorId(AuditStatus.PENDING_REVIEW, auditorId)
                .stream()
                .map(AuditReportResponseDTO::fromEntity)
                .toList();
    }

    public List<AuditReportAggregationDTO> getAuditReportDashboard(String portfolioId) {
        if (!portfolioService.portfolioExists(portfolioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Portfolio not found: " + portfolioId);
        }
        accessControlService.requirePortfolioAccess(portfolioId);
        return auditReportRepository.aggregateByPortfolioId(portfolioId);
    }

    private String sanitize(String value) {
        if (value == null) return "null";
        return value.replaceAll("[\r\n\t]", "_");
    }
}
