package ch.zhaw.trueyield.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.AiAnalysisMetadata;
import ch.zhaw.trueyield.model.AuditEvent;
import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.AuditReportAggregationDTO;
import ch.zhaw.trueyield.model.dto.AuditReportCreateDTO;
import ch.zhaw.trueyield.model.dto.AuditReportResponseDTO;
import ch.zhaw.trueyield.model.dto.EvidenceCreateDTO;
import ch.zhaw.trueyield.model.dto.StateChangeDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.model.enums.AnalysisState;
import ch.zhaw.trueyield.model.enums.AuditEventType;
import ch.zhaw.trueyield.repository.AuditEventRepository;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.security.AccessControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Value("${spring.ai.anthropic.chat.options.model:unknown}")
    private String configuredModelId;

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
        recordEvent(report, AuditEventType.REPORT_CREATED, null, AuditStatus.AI_ANALYZING, "SYSTEM", "Audit report created");

        List<Holding> holdings;
        try {
            holdings = holdingService.getHoldingsByPortfolioId(dto.getPortfolioId());
        } catch (Exception e) {
            log.warn("Could not load holdings for portfolio '{}': {}", dto.getPortfolioId(), e.getMessage());
            holdings = List.of();
        }
        fetchAndStoreNewsEvidence(dto.getPortfolioId(), holdings);

        // News ingestion is intentionally synchronous: analysis only starts after the evidence collection attempt.
        List<Evidence> evidence = new ArrayList<>();
        for (Holding holding : holdings) {
            try {
                List<Evidence> evidenceList = evidenceService.getEvidenceByHoldingId(holding.getId());
                if (evidenceList != null) evidence.addAll(evidenceList.stream()
                        .filter(e -> e.getContentSnippet() != null && !e.getContentSnippet().isBlank()).toList());
            } catch (Exception e) {
                log.warn("Could not load evidence snippets for holding '{}': {}", holding.getId(), e.getMessage());
            }
        }

        applyEvidenceAnalysis(report, evidence);

        report.setAuditStatus(AuditStatus.PENDING_REVIEW);
        AuditReport saved = auditReportRepository.save(report);
        recordEvent(saved, AuditEventType.ANALYSIS_COMPLETED, AuditStatus.AI_ANALYZING, AuditStatus.PENDING_REVIEW,
                "SYSTEM", saved.getAiAnalysisMetadata().getAnalysisState().name());
        return saved;
    }

    private static final int MAX_EVIDENCE_PER_HOLDING = 10;

    private void fetchAndStoreNewsEvidence(String portfolioId, List<Holding> holdings) {
        try {
            for (Holding holding : holdings) {
                if (evidenceService.countByHoldingId(holding.getId()) >= MAX_EVIDENCE_PER_HOLDING) {
                    continue;
                }
                String query = holding.getName() != null ? holding.getName() : holding.getSymbol();
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
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "AuditReport must be in UNDER_REVIEW state");
        }
        validateDecision(report, auditorId, dto);
        log.info("AuditReport [{}] rejected by auditor [{}] — status: UNDER_REVIEW → REJECTED", sanitize(dto.getAuditReportId()), sanitize(auditorId));
        report.setAuditStatus(AuditStatus.REJECTED);
        AuditReport saved = auditReportRepository.save(report);
        saveDecisionCommentAndEvent(saved, auditorId, dto.getRationale(), AuditEventType.REPORT_REJECTED, AuditStatus.REJECTED);
        return saved;
    }

    public AuditReport assignAuditReport(StateChangeDTO dto) {
        String auditorId = accessControlService.requireAuditorId();
        AuditReport report = auditReportRepository.findById(dto.getAuditReportId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + dto.getAuditReportId()));
        if (report.getAuditStatus() != AuditStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "AuditReport must be in PENDING_REVIEW state");
        }
        log.info("AuditReport [{}] assigned to auditor [{}] — status: PENDING_REVIEW → UNDER_REVIEW", sanitize(dto.getAuditReportId()), sanitize(auditorId));
        report.setAuditStatus(AuditStatus.UNDER_REVIEW);
        report.setAuditorId(auditorId);
        AuditReport saved = auditReportRepository.save(report);
        recordEvent(saved, AuditEventType.REPORT_ASSIGNED, AuditStatus.PENDING_REVIEW, AuditStatus.UNDER_REVIEW, auditorId, "Assigned for review");
        return saved;
    }

    public AuditReport completeAuditReport(StateChangeDTO dto) {
        String auditorId = accessControlService.requireAuditorId();
        AuditReport report = auditReportRepository.findById(dto.getAuditReportId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + dto.getAuditReportId()));
        if (report.getAuditStatus() != AuditStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "AuditReport must be in UNDER_REVIEW state");
        }
        validateDecision(report, auditorId, dto);
        log.info("AuditReport [{}] approved by auditor [{}] — status: UNDER_REVIEW → APPROVED", sanitize(dto.getAuditReportId()), sanitize(auditorId));
        report.setAuditStatus(AuditStatus.APPROVED);
        AuditReport saved = auditReportRepository.save(report);
        saveDecisionCommentAndEvent(saved, auditorId, dto.getRationale(), AuditEventType.REPORT_APPROVED, AuditStatus.APPROVED);
        return saved;
    }

    public List<AuditReportResponseDTO> getAuditorQueue(String auditorId) {
        return auditReportRepository
                .findByAuditStatusOrAuditorId(AuditStatus.PENDING_REVIEW, auditorId)
                .stream()
                .map(AuditReportResponseDTO::fromEntity)
                .toList();
    }

    public AuditReportResponseDTO getLatestAuditReportByPortfolioId(String portfolioId) {
        accessControlService.requirePortfolioAccess(portfolioId);
        return auditReportRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId)
                .stream()
                .findFirst()
                .map(AuditReportResponseDTO::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No audit report found for portfolio: " + portfolioId));
    }

    public List<AuditReportAggregationDTO> getAuditReportDashboard(String portfolioId) {
        if (!portfolioService.portfolioExists(portfolioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Portfolio not found: " + portfolioId);
        }
        accessControlService.requirePortfolioAccess(portfolioId);
        return auditReportRepository.aggregateByPortfolioId(portfolioId);
    }

    public List<AuditEvent> getAuditHistory(String auditReportId) {
        AuditReport report = getAuditReportById(auditReportId);
        return auditEventRepository.findByAuditReportIdOrderByCreatedAtAsc(report.getId());
    }

    private void applyEvidenceAnalysis(AuditReport report, List<Evidence> evidence) {
        AiAnalysisMetadata metadata = new AiAnalysisMetadata();
        metadata.setAnalyzedAt(LocalDateTime.now());
        metadata.setPromptVersion("evidence-only-v1");
        metadata.setModelId(aiAnalysisService.isAvailable() ? configuredModelId : null);
        metadata.setInputEvidenceCount(evidence.size());
        if (evidence.isEmpty()) {
            metadata.setAnalysisState(AnalysisState.INSUFFICIENT_EVIDENCE);
            metadata.setFallbackReason("No evidence snippets were available after ingestion.");
            report.setAiRiskSummary("Insufficient evidence for an advisory ESG risk assessment.");
            report.setAiRiskRationale("A human reviewer must assess the available source material.");
            report.setAiAnalysisMetadata(metadata);
            return;
        }
        if (!aiAnalysisService.isAvailable()) {
            metadata.setAnalysisState(AnalysisState.ANALYSIS_UNAVAILABLE);
            metadata.setFallbackReason("AI model is not configured or unavailable.");
            report.setAiRiskSummary("AI analysis unavailable; evidence remains available for human review.");
            report.setAiRiskRationale("No advisory AI score was produced.");
            report.setAiAnalysisMetadata(metadata);
            return;
        }
        List<AiAnalysisService.EvidenceInput> inputs = evidence.stream()
                .map(item -> new AiAnalysisService.EvidenceInput(item.getId(), item.getContentSnippet())).toList();
        AiAnalysisService.EvidenceAnalysisResult result = aiAnalysisService.analyzeEvidence(inputs);
        if (result == null) {
            result = new AiAnalysisService.EvidenceAnalysisResult(null, null, null, List.of());
        }
        LinkedHashSet<String> allowedIds = evidence.stream().map(Evidence::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<String> citedIds = result.citedEvidenceIds().stream().filter(allowedIds::contains).distinct().toList();
        metadata.setCitedEvidenceIds(citedIds);
        metadata.setEvidenceCoverage((double) citedIds.size() / evidence.size());
        if (result.score() == null || result.summary() == null || result.summary().isBlank()
                || result.rationale() == null || result.rationale().isBlank() || citedIds.isEmpty()) {
            metadata.setAnalysisState(AnalysisState.INSUFFICIENT_EVIDENCE);
            metadata.setFallbackReason("The advisory output was incomplete or did not cite supplied evidence.");
            report.setAiRiskSummary("Insufficient evidence for a cited advisory risk assessment.");
            report.setAiRiskRationale("A human reviewer must assess the evidence directly.");
        } else {
            metadata.setAnalysisState(AnalysisState.COMPLETED);
            report.setAiRiskScore(result.score());
            report.setAiRiskSummary(result.summary());
            report.setAiRiskRationale(result.rationale());
        }
        report.setAiAnalysisMetadata(metadata);
    }

    private void validateDecision(AuditReport report, String auditorId, StateChangeDTO dto) {
        if (dto.getRationale() == null || dto.getRationale().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A decision rationale is required");
        }
        if (report.getAuditorId() == null || !auditorId.equals(report.getAuditorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AuditorId does not match assigned auditor");
        }
    }

    private void saveDecisionCommentAndEvent(AuditReport report, String auditorId, String rationale,
                                              AuditEventType eventType, AuditStatus toStatus) {
        recordEvent(report, AuditEventType.COMMENT_ADDED, toStatus, toStatus, auditorId, rationale.trim());
        recordEvent(report, eventType, AuditStatus.UNDER_REVIEW, toStatus, auditorId, "Auditor decision recorded");
    }

    private void recordEvent(AuditReport report, AuditEventType type, AuditStatus from, AuditStatus to,
                             String actorId, String detail) {
        auditEventRepository.save(new AuditEvent(report.getId(), type, from, to, actorId, detail));
    }

    private String sanitize(String value) {
        if (value == null) return "null";
        return value.replaceAll("[\r\n\t]", "_");
    }
}
