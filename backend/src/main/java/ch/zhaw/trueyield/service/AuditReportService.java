package ch.zhaw.trueyield.service;

import java.util.List;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.AuditReportAggregationDTO;
import ch.zhaw.trueyield.model.dto.AuditReportCreateDTO;
import ch.zhaw.trueyield.model.dto.EvidenceCreateDTO;
import ch.zhaw.trueyield.model.dto.StateChangeDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.repository.AuditReportRepository;
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
    private PortfolioService portfolioService;

    @Autowired
    private HoldingService holdingService;

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private NewsService newsService;

    @Autowired
    private AiAnalysisService aiAnalysisService;

    public AuditReport createAuditReport(AuditReportCreateDTO dto) {
        if (!portfolioService.portfolioExists(dto.getPortfolioId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Portfolio not found: " + dto.getPortfolioId());
        }
        AuditReport report = new AuditReport(dto.getPortfolioId(), AuditStatus.AI_ANALYZING);
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

        String summary = aiAnalysisService.generateRiskSummary(holdingNames);
        report.setAiRiskSummary(summary);

        report.setAuditStatus(AuditStatus.PENDING_REVIEW);
        return auditReportRepository.save(report);
    }

    private void fetchAndStoreNewsEvidence(String portfolioId, List<Holding> holdings) {
        if (!newsService.isConfigured()) {
            return;
        }
        try {
            for (Holding holding : holdings) {
                String query = holding.getName() != null ? holding.getName() : holding.getSymbol();
                List<NewsService.NewsArticle> articles = newsService.fetchNewsForHolding(query);
                for (NewsService.NewsArticle article : articles) {
                    EvidenceCreateDTO evidenceDTO = new EvidenceCreateDTO();
                    evidenceDTO.setHoldingId(holding.getId());
                    evidenceDTO.setSourceUrl(article.url());
                    evidenceDTO.setContentSnippet(article.content());
                    evidenceService.createEvidence(evidenceDTO);
                }
            }
        } catch (Exception e) {
            log.warn("News evidence fetch failed for portfolio '{}': {}", portfolioId, e.getMessage());
        }
    }

    public AuditReport getAuditReportById(String id) {
        return auditReportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "AuditReport not found: " + id));
    }

    public AuditReport rejectAuditReport(StateChangeDTO dto) {
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
        report.setAuditStatus(AuditStatus.REJECTED);
        return auditReportRepository.save(report);
    }

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
