package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.ComplianceOverviewDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.repository.HoldingRepository;
import ch.zhaw.trueyield.repository.PortfolioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private AuditReportRepository auditReportRepository;

    @InjectMocks
    private ComplianceService complianceService;

    // ── getOverview ──────────────────────────────────────────────────────────

    @Test
    void getOverview_returnsCorrectCounts() {
        when(portfolioRepository.count()).thenReturn(5L);
        when(holdingRepository.count()).thenReturn(20L);
        AuditReport approved = makeReport(AuditStatus.APPROVED);
        AuditReport pending = makeReport(AuditStatus.PENDING_REVIEW);
        AuditReport underReview = makeReport(AuditStatus.UNDER_REVIEW);
        when(auditReportRepository.findAll()).thenReturn(List.of(approved, pending, underReview));

        ComplianceOverviewDTO dto = complianceService.getOverview();

        assertEquals(5L, dto.totalPortfolios());
        assertEquals(20L, dto.totalHoldings());
        assertEquals(3L, dto.totalAuditReports());
    }

    @Test
    void getOverview_reportsByStatus_containsAllStatuses() {
        when(portfolioRepository.count()).thenReturn(0L);
        when(holdingRepository.count()).thenReturn(0L);
        when(auditReportRepository.findAll()).thenReturn(List.of());

        ComplianceOverviewDTO dto = complianceService.getOverview();

        for (AuditStatus status : AuditStatus.values()) {
            assertTrue(dto.reportsByStatus().containsKey(status.name()),
                    "reportsByStatus must contain key: " + status.name());
        }
    }

    @Test
    void getOverview_reportsByStatus_countsCorrectly() {
        when(portfolioRepository.count()).thenReturn(2L);
        when(holdingRepository.count()).thenReturn(8L);
        AuditReport a1 = makeReport(AuditStatus.APPROVED);
        AuditReport a2 = makeReport(AuditStatus.APPROVED);
        AuditReport r1 = makeReport(AuditStatus.REJECTED);
        when(auditReportRepository.findAll()).thenReturn(List.of(a1, a2, r1));

        ComplianceOverviewDTO dto = complianceService.getOverview();

        assertEquals(2L, dto.reportsByStatus().get(AuditStatus.APPROVED.name()));
        assertEquals(1L, dto.reportsByStatus().get(AuditStatus.REJECTED.name()));
        assertEquals(0L, dto.reportsByStatus().get(AuditStatus.PENDING_REVIEW.name()));
    }

    @Test
    void getOverview_withEmptyDatabase_returnsZeroCounts() {
        when(portfolioRepository.count()).thenReturn(0L);
        when(holdingRepository.count()).thenReturn(0L);
        when(auditReportRepository.findAll()).thenReturn(List.of());

        ComplianceOverviewDTO dto = complianceService.getOverview();

        assertEquals(0L, dto.totalPortfolios());
        assertEquals(0L, dto.totalHoldings());
        assertEquals(0L, dto.totalAuditReports());
        dto.reportsByStatus().values().forEach(count -> assertEquals(0L, count));
    }

    @Test
    void getOverview_callsAllRepositories() {
        when(portfolioRepository.count()).thenReturn(1L);
        when(holdingRepository.count()).thenReturn(1L);
        when(auditReportRepository.findAll()).thenReturn(List.of());

        complianceService.getOverview();

        verify(portfolioRepository, times(1)).count();
        verify(holdingRepository, times(1)).count();
        verify(auditReportRepository, times(1)).findAll();
    }

    private AuditReport makeReport(AuditStatus status) {
        AuditReport report = new AuditReport("portfolio-001", status);
        report.setId("report-" + status.name().toLowerCase());
        return report;
    }
}
