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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @Mock
    private EvidenceRepository evidenceRepository;

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

    // ── getEsgEvidenceSignals ─────────────────────────────────────────────────

    @Test
    void getEsgEvidenceSignals_returnsInsufficientEvidence_whenNoEvidence() {
        Portfolio p = makePortfolio("p1", "Green Fund");
        Holding h = makeHolding("h1", "p1");
        when(portfolioRepository.findAll()).thenReturn(List.of(p));
        when(holdingRepository.findByPortfolioId("p1")).thenReturn(List.of(h));
        when(evidenceRepository.findByHoldingId("h1")).thenReturn(List.of());

        List<EsgEvidenceSignalDTO> scores = complianceService.getEsgEvidenceSignals();

        assertEquals(1, scores.size());
        assertEquals(EsgEvidenceSignal.INSUFFICIENT_EVIDENCE, scores.get(0).signal());
        assertEquals(0, scores.get(0).evidenceCount());
    }

    @ParameterizedTest
    @CsvSource({
        "0.5,  FAVOURABLE",
        "0.31, FAVOURABLE",
        "0.1,  MIXED",
        "-0.09, MIXED",
        "-0.2,  ADVERSE",
        "-1.0,  ADVERSE"
    })
    void getEsgEvidenceSignals_classifiesEvidenceOnly(double sentiment, EsgEvidenceSignal expected) {
        Portfolio p = makePortfolio("p1", "Test Fund");
        Holding h = makeHolding("h1", "p1");
        Evidence e = makeEvidence("h1", sentiment);
        when(portfolioRepository.findAll()).thenReturn(List.of(p));
        when(holdingRepository.findByPortfolioId("p1")).thenReturn(List.of(h));
        when(evidenceRepository.findByHoldingId("h1")).thenReturn(List.of(e));

        List<EsgEvidenceSignalDTO> scores = complianceService.getEsgEvidenceSignals();

        assertEquals(expected, scores.get(0).signal());
        assertEquals(1, scores.get(0).evidenceCount());
    }

    @Test
    void getEsgEvidenceSignals_aggregatesAcrossMultipleHoldings() {
        Portfolio p = makePortfolio("p1", "Multi Fund");
        Holding h1 = makeHolding("h1", "p1");
        Holding h2 = makeHolding("h2", "p1");
        when(portfolioRepository.findAll()).thenReturn(List.of(p));
        when(holdingRepository.findByPortfolioId("p1")).thenReturn(List.of(h1, h2));
        when(evidenceRepository.findByHoldingId("h1")).thenReturn(List.of(makeEvidence("h1", 0.4)));
        when(evidenceRepository.findByHoldingId("h2")).thenReturn(List.of(makeEvidence("h2", -0.2)));

        List<EsgEvidenceSignalDTO> scores = complianceService.getEsgEvidenceSignals();

        assertEquals(2, scores.get(0).evidenceCount());
        assertEquals(EsgEvidenceSignal.MIXED, scores.get(0).signal()); // avg = 0.1
    }

    @Test
    void getEsgEvidenceSignals_returnsEmptyList_whenNoPortfolios() {
        when(portfolioRepository.findAll()).thenReturn(List.of());

        List<EsgEvidenceSignalDTO> scores = complianceService.getEsgEvidenceSignals();

        assertTrue(scores.isEmpty());
    }

    @Test
    void getEsgEvidenceSignals_doesNotUseAuditScores_whenNoEvidence() {
        Portfolio p = makePortfolio("p1", "AI Fund");
        Holding h = makeHolding("h1", "p1");
        when(portfolioRepository.findAll()).thenReturn(List.of(p));
        when(holdingRepository.findByPortfolioId("p1")).thenReturn(List.of(h));
        when(evidenceRepository.findByHoldingId("h1")).thenReturn(List.of());

        List<EsgEvidenceSignalDTO> scores = complianceService.getEsgEvidenceSignals();

        assertEquals(EsgEvidenceSignal.INSUFFICIENT_EVIDENCE, scores.get(0).signal());
        assertEquals(0, scores.get(0).evidenceCount());
    }

    @Test
    void getEsgEvidenceSignals_usesEvidenceOnly_whenAuditScoreAlsoExists() {
        Portfolio p = makePortfolio("p1", "Blend Fund");
        Holding h = makeHolding("h1", "p1");
        AuditReport report = makeReportWithRiskScore("p1", 0);
        Evidence e = makeEvidence("h1", 0.5);
        when(portfolioRepository.findAll()).thenReturn(List.of(p));
        when(holdingRepository.findByPortfolioId("p1")).thenReturn(List.of(h));
        when(evidenceRepository.findByHoldingId("h1")).thenReturn(List.of(e));

        List<EsgEvidenceSignalDTO> scores = complianceService.getEsgEvidenceSignals();

        assertEquals(EsgEvidenceSignal.FAVOURABLE, scores.get(0).signal());
        assertEquals(1, scores.get(0).evidenceCount());
        assertEquals(0.5, scores.get(0).averageEvidenceSentiment(), 0.001);
    }

    @Test
    void getEsgEvidenceSignals_returnsInsufficientEvidence_whenNoEvidenceAndNoRiskScore() {
        Portfolio p = makePortfolio("p1", "Empty Fund");
        Holding h = makeHolding("h1", "p1");
        when(portfolioRepository.findAll()).thenReturn(List.of(p));
        when(holdingRepository.findByPortfolioId("p1")).thenReturn(List.of(h));
        when(evidenceRepository.findByHoldingId("h1")).thenReturn(List.of());

        List<EsgEvidenceSignalDTO> scores = complianceService.getEsgEvidenceSignals();

        assertEquals(EsgEvidenceSignal.INSUFFICIENT_EVIDENCE, scores.get(0).signal());
    }

    // ── getAllPortfolios / getAllReports ──────────────────────────────────────

    @Test
    void getAllPortfolios_delegatesToRepository() {
        Portfolio p = makePortfolio("p1", "Fund A");
        when(portfolioRepository.findAll()).thenReturn(List.of(p));

        List<Portfolio> result = complianceService.getAllPortfolios();

        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).getId());
        verify(portfolioRepository).findAll();
    }

    @Test
    void getAllReports_delegatesToRepository() {
        AuditReport r = makeReport(AuditStatus.APPROVED);
        when(auditReportRepository.findAll()).thenReturn(List.of(r));

        List<AuditReport> result = complianceService.getAllReports();

        assertEquals(1, result.size());
        verify(auditReportRepository).findAll();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AuditReport makeReport(AuditStatus status) {
        AuditReport report = new AuditReport("portfolio-001", status);
        report.setId("report-" + status.name().toLowerCase());
        return report;
    }

    private Portfolio makePortfolio(String id, String name) {
        Portfolio p = new Portfolio(name, "fm-1");
        p.setId(id);
        return p;
    }

    private Holding makeHolding(String id, String portfolioId) {
        Holding h = new Holding(portfolioId, "SYM");
        h.setId(id);
        return h;
    }

    private Evidence makeEvidence(String holdingId, double sentiment) {
        Evidence e = new Evidence(holdingId);
        e.setAiSentimentScore(sentiment);
        return e;
    }

    private AuditReport makeReportWithRiskScore(String portfolioId, int aiRiskScore) {
        AuditReport report = new AuditReport(portfolioId, AuditStatus.APPROVED);
        report.setAiRiskScore(aiRiskScore);
        return report;
    }
}
