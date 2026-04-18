package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.ComplianceOverviewDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.repository.AuditReportRepository;
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

    public ComplianceService(PortfolioRepository portfolioRepository,
                             HoldingRepository holdingRepository,
                             AuditReportRepository auditReportRepository) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.auditReportRepository = auditReportRepository;
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
}
