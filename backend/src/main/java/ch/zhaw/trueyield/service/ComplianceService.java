package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.ComplianceOverviewDTO;
import ch.zhaw.trueyield.model.dto.EsgEvidenceSignalDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.model.enums.EsgEvidenceSignal;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import ch.zhaw.trueyield.repository.HoldingRepository;
import ch.zhaw.trueyield.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ComplianceService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final AuditReportRepository auditReportRepository;
    private final EvidenceRepository evidenceRepository;

    public ComplianceService(PortfolioRepository portfolioRepository,
                             HoldingRepository holdingRepository,
                             AuditReportRepository auditReportRepository,
                             EvidenceRepository evidenceRepository) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.auditReportRepository = auditReportRepository;
        this.evidenceRepository = evidenceRepository;
    }

    public ComplianceOverviewDTO getOverview() {
        long totalPortfolios = portfolioRepository.count();
        long totalHoldings = holdingRepository.count();
        List<AuditReport> allReports = auditReportRepository.findAll();
        Map<String, Long> reportsByStatus = Arrays.stream(AuditStatus.values())
                .collect(Collectors.toMap(
                        AuditStatus::name,
                        status -> allReports.stream().filter(r -> r.getAuditStatus() == status).count()));
        return new ComplianceOverviewDTO(totalPortfolios, totalHoldings, allReports.size(), reportsByStatus);
    }

    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }

    public List<AuditReport> getAllReports() {
        return auditReportRepository.findAll();
    }

    /**
     * Non-regulatory aggregation of the stored evidence signals. This deliberately never mixes
     * in model training knowledge, an audit score, or an SFDR-style classification.
     */
    public List<EsgEvidenceSignalDTO> getEsgEvidenceSignals() {
        return portfolioRepository.findAll().stream().map(this::signalPortfolio).toList();
    }

    private EsgEvidenceSignalDTO signalPortfolio(Portfolio portfolio) {
        List<String> holdingIds = holdingRepository.findByPortfolioId(portfolio.getId()).stream()
                .map(Holding::getId).toList();
        List<Double> evidenceScores = holdingIds.stream()
                .flatMap(id -> evidenceRepository.findByHoldingId(id).stream())
                .map(Evidence::getAiSentimentScore)
                .filter(score -> score != null)
                .toList();

        if (evidenceScores.isEmpty()) {
            return new EsgEvidenceSignalDTO(portfolio.getId(), portfolio.getName(),
                    EsgEvidenceSignal.INSUFFICIENT_EVIDENCE, 0.0, 0);
        }
        double average = evidenceScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return new EsgEvidenceSignalDTO(portfolio.getId(), portfolio.getName(), classify(average),
                Math.round(average * 1000.0) / 1000.0, evidenceScores.size());
    }

    private static EsgEvidenceSignal classify(double averageEvidenceSentiment) {
        if (averageEvidenceSentiment > 0.3) return EsgEvidenceSignal.FAVOURABLE;
        if (averageEvidenceSentiment > -0.1) return EsgEvidenceSignal.MIXED;
        return EsgEvidenceSignal.ADVERSE;
    }
}
