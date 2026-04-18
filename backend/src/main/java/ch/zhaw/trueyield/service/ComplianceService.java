package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.ComplianceOverviewDTO;
import ch.zhaw.trueyield.model.dto.SfdrPortfolioScoreDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.model.enums.SfdrClassification;
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
        long totalAuditReports = allReports.size();

        Map<String, Long> reportsByStatus = Arrays.stream(AuditStatus.values())
                .collect(Collectors.toMap(
                        AuditStatus::name,
                        status -> allReports.stream()
                                .filter(r -> r.getAuditStatus() == status)
                                .count()
                ));

        return new ComplianceOverviewDTO(totalPortfolios, totalHoldings, totalAuditReports, reportsByStatus);
    }

    public List<SfdrPortfolioScoreDTO> getSfdrScores() {
        return portfolioRepository.findAll().stream()
                .map(this::scorePortfolio)
                .toList();
    }

    private SfdrPortfolioScoreDTO scorePortfolio(Portfolio portfolio) {
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolio.getId());
        List<String> holdingIds = holdings.stream().map(Holding::getId).toList();

        List<Double> scores = holdingIds.stream()
                .flatMap(hid -> evidenceRepository.findByHoldingId(hid).stream())
                .map(Evidence::getAiSentimentScore)
                .filter(s -> s != null)
                .toList();

        if (scores.isEmpty()) {
            return new SfdrPortfolioScoreDTO(
                    portfolio.getId(), portfolio.getName(),
                    SfdrClassification.INSUFFICIENT_DATA, 0.0, 0);
        }

        double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        SfdrClassification classification = classify(avg);
        return new SfdrPortfolioScoreDTO(
                portfolio.getId(), portfolio.getName(), classification,
                Math.round(avg * 1000.0) / 1000.0, scores.size());
    }

    private static SfdrClassification classify(double avgSentiment) {
        if (avgSentiment > 0.3) return SfdrClassification.ARTICLE_9;
        if (avgSentiment > -0.1) return SfdrClassification.ARTICLE_8;
        return SfdrClassification.NON_SFDR;
    }
}
