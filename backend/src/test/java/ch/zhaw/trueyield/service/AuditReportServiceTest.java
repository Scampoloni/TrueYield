package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.model.dto.AuditReportCreateDTO;
import ch.zhaw.trueyield.model.dto.StateChangeDTO;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.security.AccessControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditReportServiceTest {

    @Mock
    private AuditReportRepository auditReportRepository;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private HoldingService holdingService;

    @Mock
    private EvidenceService evidenceService;

    @Mock
    private NewsIngestionService newsIngestionService;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @InjectMocks
    private AuditReportService auditReportService;

    private AuditReport pendingReport;
    private AuditReport underReviewReport;
    private StateChangeDTO dto;

    @BeforeEach
    void setUp() {
        pendingReport = new AuditReport("portfolio-001", AuditStatus.PENDING_REVIEW);
        pendingReport.setAuditorId(null);

        underReviewReport = new AuditReport("portfolio-001", AuditStatus.UNDER_REVIEW);
        underReviewReport.setAuditorId("auditor-001");

        dto = mock(StateChangeDTO.class);
        lenient().when(dto.getAuditReportId()).thenReturn("report-001");
        lenient().when(aiAnalysisService.generateRiskSummary(anyList()))
            .thenReturn("Mock AI risk summary.");
        lenient().when(aiAnalysisService.generateRiskSummary(anyList()))
            .thenReturn("Mock AI risk summary.");
    }

    // ── getAuditReportById ───────────────────────────────────────────────────

    @Test
    void getAuditReportById_returnsReport_whenFound() {
        when(auditReportRepository.findById("report-001"))
                .thenReturn(Optional.of(pendingReport));
        doNothing().when(accessControlService).requireAuditReportAccess(pendingReport);

        AuditReport result = auditReportService.getAuditReportById("report-001");

        assertNotNull(result);
        assertEquals(AuditStatus.PENDING_REVIEW, result.getAuditStatus());
    }

    @Test
    void getAuditReportById_throwsNotFound_whenMissing() {
        when(auditReportRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditReportService.getAuditReportById("missing"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ── assignAuditReport ────────────────────────────────────────────────────

    @Test
    void assignAuditReport_transitionsToUnderReview_whenPending() {
        when(auditReportRepository.findById("report-001"))
                .thenReturn(Optional.of(pendingReport));
        when(auditReportRepository.save(any(AuditReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accessControlService.requireAuditorId()).thenReturn("auditor-001");

        AuditReport result = auditReportService.assignAuditReport(dto);

        assertEquals(AuditStatus.UNDER_REVIEW, result.getAuditStatus());
        assertEquals("auditor-001", result.getAuditorId());
        verify(auditReportRepository, times(1)).save(pendingReport);
    }

    @Test
    void assignAuditReport_throwsBadRequest_whenReportNotFound() {
        when(auditReportRepository.findById("report-001")).thenReturn(Optional.empty());
        when(accessControlService.requireAuditorId()).thenReturn("auditor-001");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditReportService.assignAuditReport(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(auditReportRepository, never()).save(any());
    }

    // Parametrisiert: alle Status ausser PENDING_REVIEW dürfen nicht zugewiesen werden.
    // Spalten: statusName, expectedHttpStatus
    @ParameterizedTest
    @CsvSource({
        "AI_ANALYZING,   BAD_REQUEST",
        "UNDER_REVIEW,   BAD_REQUEST",
        "APPROVED,       BAD_REQUEST",
        "REJECTED,       BAD_REQUEST"
    })
    void assignAuditReport_throwsBadRequest_forNonPendingStatus(ArgumentsAccessor args) {
        AuditStatus status = AuditStatus.valueOf(args.getString(0).trim());
        HttpStatus expected = HttpStatus.valueOf(args.getString(1).trim());

        AuditReport report = new AuditReport("portfolio-001", status);
        when(auditReportRepository.findById("report-001")).thenReturn(Optional.of(report));
        when(accessControlService.requireAuditorId()).thenReturn("auditor-001");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditReportService.assignAuditReport(dto));

        assertEquals(expected, ex.getStatusCode());
        verify(auditReportRepository, never()).save(any());
    }

    // ── rejectAuditReport ────────────────────────────────────────────────────

    @Test
    void rejectAuditReport_transitionsToRejected_whenUnderReviewAndAuditorMatches() {
        when(auditReportRepository.findById("report-001"))
                .thenReturn(Optional.of(underReviewReport));
        when(auditReportRepository.save(any(AuditReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accessControlService.requireAuditorId()).thenReturn("auditor-001");

        AuditReport result = auditReportService.rejectAuditReport(dto);

        assertEquals(AuditStatus.REJECTED, result.getAuditStatus());
        verify(auditReportRepository, times(1)).save(underReviewReport);
    }

    @Test
    void rejectAuditReport_throwsBadRequest_whenAuditorDoesNotMatch() {
        when(auditReportRepository.findById("report-001"))
                .thenReturn(Optional.of(underReviewReport));
        when(accessControlService.requireAuditorId()).thenReturn("wrong-auditor");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditReportService.rejectAuditReport(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(auditReportRepository, never()).save(any());
    }

    // Parametrisiert mit @ValueSource: alle Status ausser UNDER_REVIEW dürfen nicht rejected werden
    @ParameterizedTest
    @ValueSource(strings = {"AI_ANALYZING", "PENDING_REVIEW", "APPROVED", "REJECTED"})
    void rejectAuditReport_throwsBadRequest_forNonUnderReviewStatus(String statusName) {
        AuditReport report = new AuditReport("portfolio-001", AuditStatus.valueOf(statusName));
        when(auditReportRepository.findById("report-001")).thenReturn(Optional.of(report));
        when(accessControlService.requireAuditorId()).thenReturn("auditor-001");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditReportService.rejectAuditReport(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(auditReportRepository, never()).save(any());
    }

    // ── completeAuditReport ──────────────────────────────────────────────────

    @Test
    void completeAuditReport_transitionsToApproved_whenUnderReviewAndAuditorMatches() {
        when(auditReportRepository.findById("report-001"))
                .thenReturn(Optional.of(underReviewReport));
        when(auditReportRepository.save(any(AuditReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accessControlService.requireAuditorId()).thenReturn("auditor-001");

        AuditReport result = auditReportService.completeAuditReport(dto);

        assertEquals(AuditStatus.APPROVED, result.getAuditStatus());
        verify(auditReportRepository, times(1)).save(underReviewReport);
    }

    @Test
    void completeAuditReport_throwsBadRequest_whenAuditorDoesNotMatch() {
        when(auditReportRepository.findById("report-001"))
                .thenReturn(Optional.of(underReviewReport));
        when(accessControlService.requireAuditorId()).thenReturn("wrong-auditor");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditReportService.completeAuditReport(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(auditReportRepository, never()).save(any());
    }

    // Parametrisiert mit @ValueSource: alle Status ausser UNDER_REVIEW dürfen nicht completed werden
    @ParameterizedTest
    @ValueSource(strings = {"AI_ANALYZING", "PENDING_REVIEW", "APPROVED", "REJECTED"})
    void completeAuditReport_throwsBadRequest_forNonUnderReviewStatus(String statusName) {
        AuditReport report = new AuditReport("portfolio-001", AuditStatus.valueOf(statusName));
        when(auditReportRepository.findById("report-001")).thenReturn(Optional.of(report));
        when(accessControlService.requireAuditorId()).thenReturn("auditor-001");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditReportService.completeAuditReport(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(auditReportRepository, never()).save(any());
    }

    // ── getAuditReportDashboard ──────────────────────────────────────────────

    @Test
    void getAuditReportDashboard_throwsBadRequest_whenPortfolioNotFound() {
        when(portfolioService.portfolioExists("unknown-portfolio")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditReportService.getAuditReportDashboard("unknown-portfolio"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(auditReportRepository, never()).aggregateByPortfolioId(any());
    }

    @Test
    void getAuditReportDashboard_callsRepository_whenPortfolioExists() {
        when(portfolioService.portfolioExists("portfolio-001")).thenReturn(true);
        doNothing().when(accessControlService).requirePortfolioAccess("portfolio-001");
        when(auditReportRepository.aggregateByPortfolioId("portfolio-001")).thenReturn(java.util.List.of());

        auditReportService.getAuditReportDashboard("portfolio-001");

        verify(auditReportRepository, times(1)).aggregateByPortfolioId("portfolio-001");
    }

    // ── createAuditReport ────────────────────────────────────────────────────

    @Test
    void createAuditReport_savesReport_whenPortfolioExists() {
        AuditReportCreateDTO createDTO = mock(AuditReportCreateDTO.class);
        when(createDTO.getPortfolioId()).thenReturn("portfolio-001");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        AuditReport saved = new AuditReport("portfolio-001", AuditStatus.PENDING_REVIEW);
        when(auditReportRepository.save(any(AuditReport.class))).thenReturn(saved);

        AuditReport result = auditReportService.createAuditReport(createDTO);

        assertEquals("portfolio-001", result.getPortfolioId());
        assertEquals(AuditStatus.PENDING_REVIEW, result.getAuditStatus());
        verify(auditReportRepository, times(2)).save(any(AuditReport.class));
    }

    @Test
    void createAuditReport_setsUnavailableSummary_whenAiNotAvailable() {
        AuditReportCreateDTO createDTO = mock(AuditReportCreateDTO.class);
        when(createDTO.getPortfolioId()).thenReturn("portfolio-001");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        AuditReport saved = new AuditReport("portfolio-001", AuditStatus.PENDING_REVIEW);
        when(auditReportRepository.save(any(AuditReport.class))).thenReturn(saved);
        when(aiAnalysisService.generateRiskSummary(anyList())).thenReturn("AI analysis unavailable.");

        AuditReport result = auditReportService.createAuditReport(createDTO);

        assertEquals(AuditStatus.PENDING_REVIEW, result.getAuditStatus());
        verify(auditReportRepository, times(2)).save(any(AuditReport.class));
    }

    @Test
    void createAuditReport_fetchesNews_viaIngestionService() {
        AuditReportCreateDTO createDTO = mock(AuditReportCreateDTO.class);
        when(createDTO.getPortfolioId()).thenReturn("portfolio-001");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        AuditReport saved = new AuditReport("portfolio-001", AuditStatus.PENDING_REVIEW);
        when(auditReportRepository.save(any(AuditReport.class))).thenReturn(saved);

        ch.zhaw.trueyield.model.Holding holding = new ch.zhaw.trueyield.model.Holding("portfolio-001", "AAPL");
        holding.setId("holding-001");
        holding.setName("Apple Inc.");
        when(holdingService.getHoldingsByPortfolioId("portfolio-001")).thenReturn(java.util.List.of(holding));

        auditReportService.createAuditReport(createDTO);

        verify(newsIngestionService, times(1)).ingestNewsForHolding("holding-001", "Apple Inc.");
    }

    @Test
    void createAuditReport_skipsIngestion_whenHoldingAlreadyAtMaxCapacity() {
        AuditReportCreateDTO createDTO = mock(AuditReportCreateDTO.class);
        when(createDTO.getPortfolioId()).thenReturn("portfolio-001");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        AuditReport saved = new AuditReport("portfolio-001", AuditStatus.PENDING_REVIEW);
        when(auditReportRepository.save(any(AuditReport.class))).thenReturn(saved);

        ch.zhaw.trueyield.model.Holding holding = new ch.zhaw.trueyield.model.Holding("portfolio-001", "AAPL");
        holding.setId("holding-001");
        holding.setName("Apple Inc.");
        when(holdingService.getHoldingsByPortfolioId("portfolio-001")).thenReturn(java.util.List.of(holding));
        when(evidenceService.countByHoldingId("holding-001")).thenReturn(10L); // MAX_EVIDENCE_PER_HOLDING is 10

        auditReportService.createAuditReport(createDTO);

        verify(newsIngestionService, never()).ingestNewsForHolding(anyString(), anyString());
    }

    @Test
    void createAuditReport_usesSymbol_whenNameIsNull() {
        AuditReportCreateDTO createDTO = mock(AuditReportCreateDTO.class);
        when(createDTO.getPortfolioId()).thenReturn("portfolio-001");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        AuditReport saved = new AuditReport("portfolio-001", AuditStatus.PENDING_REVIEW);
        when(auditReportRepository.save(any(AuditReport.class))).thenReturn(saved);

        ch.zhaw.trueyield.model.Holding holding = new ch.zhaw.trueyield.model.Holding("portfolio-001", "AAPL");
        holding.setId("holding-001");
        holding.setName(null); // Name is null
        when(holdingService.getHoldingsByPortfolioId("portfolio-001")).thenReturn(java.util.List.of(holding));

        auditReportService.createAuditReport(createDTO);

        verify(newsIngestionService, times(1)).ingestNewsForHolding("holding-001", "AAPL");
    }

    @Test
    void createAuditReport_continuesGracefully_whenEvidenceServiceThrows() {
        AuditReportCreateDTO createDTO = mock(AuditReportCreateDTO.class);
        when(createDTO.getPortfolioId()).thenReturn("portfolio-001");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        AuditReport saved = new AuditReport("portfolio-001", AuditStatus.PENDING_REVIEW);
        when(auditReportRepository.save(any(AuditReport.class))).thenReturn(saved);

        ch.zhaw.trueyield.model.Holding holding = new ch.zhaw.trueyield.model.Holding("portfolio-001", "AAPL");
        holding.setId("holding-001");
        when(holdingService.getHoldingsByPortfolioId("portfolio-001")).thenReturn(java.util.List.of(holding));
        when(evidenceService.countByHoldingId(anyString())).thenThrow(new RuntimeException("DB offline"));

        AuditReport result = auditReportService.createAuditReport(createDTO);

        assertEquals(AuditStatus.PENDING_REVIEW, result.getAuditStatus());
        verify(newsIngestionService, never()).ingestNewsForHolding(anyString(), anyString());
    }

    @Test
    void createAuditReport_throwsBadRequest_whenPortfolioNotFound() {
        AuditReportCreateDTO createDTO = mock(AuditReportCreateDTO.class);
        when(createDTO.getPortfolioId()).thenReturn("unknown-portfolio");
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST))
            .when(accessControlService).requireFundManagerPortfolioAccess("unknown-portfolio");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditReportService.createAuditReport(createDTO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(auditReportRepository, never()).save(any());
    }



    @Test
    void createAuditReport_setsPortfolioName_whenPortfolioFound() {
        AuditReportCreateDTO createDTO = mock(AuditReportCreateDTO.class);
        when(createDTO.getPortfolioId()).thenReturn("portfolio-001");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        AuditReport saved = new AuditReport("portfolio-001", AuditStatus.PENDING_REVIEW);
        when(auditReportRepository.save(any(AuditReport.class))).thenReturn(saved);

        ch.zhaw.trueyield.model.Portfolio portfolio = new ch.zhaw.trueyield.model.Portfolio("Green Fund", "manager-001");
        when(portfolioService.getPortfolioByIdForAuditor("portfolio-001")).thenReturn(portfolio);

        auditReportService.createAuditReport(createDTO);

        verify(portfolioService, times(1)).getPortfolioByIdForAuditor("portfolio-001");
    }

    @Test
    void createAuditReport_transitionsToPendingReview_whenAiThrowsUnexpectedException() {
        AuditReportCreateDTO createDTO = mock(AuditReportCreateDTO.class);
        when(createDTO.getPortfolioId()).thenReturn("portfolio-001");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        AuditReport saved = new AuditReport("portfolio-001", AuditStatus.PENDING_REVIEW);
        when(auditReportRepository.save(any(AuditReport.class))).thenReturn(saved);
        when(aiAnalysisService.generateRiskSummary(anyList()))
                .thenThrow(new RuntimeException("Anthropic API timeout"));

        AuditReport result = auditReportService.createAuditReport(createDTO);

        assertEquals(AuditStatus.PENDING_REVIEW, result.getAuditStatus());
        verify(auditReportRepository, times(2)).save(any(AuditReport.class));
    }


}
