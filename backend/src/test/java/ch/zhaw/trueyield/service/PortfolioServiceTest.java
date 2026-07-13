package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.model.dto.PortfolioUpdateDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.repository.PortfolioRepository;
import ch.zhaw.trueyield.repository.HoldingRepository;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private AuditReportRepository auditReportRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private Portfolio portfolio;
    private PortfolioCreateDTO createDTO;
    private PortfolioUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio("ESG Global Fund", "manager-001");
        portfolio.setDescription("A diversified ESG portfolio");

        // DTOs haben keine Setter (Lombok @Getter only) → via Mockito simulieren
        // lenient: nicht jeder Test braucht beide DTOs
        createDTO = mock(PortfolioCreateDTO.class);
        lenient().when(createDTO.getName()).thenReturn("ESG Global Fund");
        lenient().when(createDTO.getDescription()).thenReturn("A diversified ESG portfolio");

        updateDTO = mock(PortfolioUpdateDTO.class);
        lenient().when(updateDTO.getName()).thenReturn("Updated Fund");
        lenient().when(updateDTO.getDescription()).thenReturn("Updated description");
    }

    // ── createPortfolio ──────────────────────────────────────────────────────

    @Test
    void createPortfolio_savesPortfolioWithCorrectOwner() {
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

        Portfolio result = portfolioService.createPortfolio(createDTO, "manager-001");

        assertNotNull(result);
        assertEquals("ESG Global Fund", result.getName());
        assertEquals("manager-001", result.getFundManagerId());
        assertEquals("A diversified ESG portfolio", result.getDescription());
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    // ── getAllPortfoliosByFundManager ─────────────────────────────────────────

    @Test
    void getAllPortfoliosByFundManager_returnsOwnedPortfolios() {
        when(portfolioRepository.findByFundManagerId("manager-001"))
                .thenReturn(List.of(portfolio));

        List<Portfolio> result = portfolioService.getAllPortfoliosByFundManager("manager-001");

        assertEquals(1, result.size());
        assertEquals("ESG Global Fund", result.get(0).getName());
    }

    @Test
    void getAllPortfoliosByFundManager_returnsEmptyList_whenNoneExist() {
        when(portfolioRepository.findByFundManagerId("unknown")).thenReturn(List.of());

        List<Portfolio> result = portfolioService.getAllPortfoliosByFundManager("unknown");

        assertTrue(result.isEmpty());
    }

    // Parametrisiert: jeder Manager bekommt nur eigene Portfolios
    @ParameterizedTest
    @ValueSource(strings = {"manager-001", "manager-002", "manager-003"})
    void getAllPortfoliosByFundManager_isolatesPerManager(String managerId) {
        Portfolio ownPortfolio = new Portfolio("Fund", managerId);
        when(portfolioRepository.findByFundManagerId(managerId))
                .thenReturn(List.of(ownPortfolio));

        List<Portfolio> result = portfolioService.getAllPortfoliosByFundManager(managerId);

        assertEquals(1, result.size());
        assertEquals(managerId, result.get(0).getFundManagerId());
    }

    // ── getPortfolioById ─────────────────────────────────────────────────────

    @Test
    void getPortfolioById_returnsPortfolio_whenOwnerMatches() {
        when(portfolioRepository.findById("portfolio-1"))
                .thenReturn(Optional.of(portfolio));

        Portfolio result = portfolioService.getPortfolioById("portfolio-1", "manager-001");

        assertNotNull(result);
        assertEquals("ESG Global Fund", result.getName());
    }

    // Parametrisiert mit CsvSource: NOT_FOUND und FORBIDDEN in einem Test
    @ParameterizedTest
    @CsvSource({
        "nonexistent, manager-001, NOT_FOUND",
        "portfolio-1, wrong-user,  FORBIDDEN"
    })
    void getPortfolioById_throwsCorrectException(
            String portfolioId, String requestingUser, String expectedStatus) {
        lenient().when(portfolioRepository.findById("nonexistent")).thenReturn(Optional.empty());
        lenient().when(portfolioRepository.findById("portfolio-1")).thenReturn(Optional.of(portfolio));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> portfolioService.getPortfolioById(portfolioId, requestingUser));

        assertEquals(HttpStatus.valueOf(expectedStatus.trim()), ex.getStatusCode());
    }

    // ── getPortfolioByIdForAuditor ───────────────────────────────────────────

    @Test
    void getPortfolioByIdForAuditor_returnsPortfolio_withoutOwnershipCheck() {
        Portfolio otherManagerPortfolio = new Portfolio("Other Fund", "manager-other");
        when(portfolioRepository.findById("portfolio-1")).thenReturn(Optional.of(otherManagerPortfolio));

        Portfolio result = portfolioService.getPortfolioByIdForAuditor("portfolio-1");

        assertNotNull(result);
        assertEquals("Other Fund", result.getName());
        assertEquals("manager-other", result.getFundManagerId());
    }

    @Test
    void getPortfolioByIdForAuditor_throwsNotFound_whenIdDoesNotExist() {
        when(portfolioRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> portfolioService.getPortfolioByIdForAuditor("nonexistent"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ── updatePortfolio ──────────────────────────────────────────────────────

    @Test
    void updatePortfolio_updatesAndSavesPortfolio_whenOwnerMatches() {
        when(portfolioRepository.findById("portfolio-1"))
                .thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

        Portfolio result = portfolioService.updatePortfolio("portfolio-1", updateDTO, "manager-001");

        assertNotNull(result);
        verify(portfolioRepository, times(1)).save(portfolio);
    }

    @Test
    void updatePortfolio_throwsNotFound_whenIdDoesNotExist() {
        when(portfolioRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> portfolioService.updatePortfolio("nonexistent", updateDTO, "manager-001"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(portfolioRepository, never()).save(any());
    }

    @Test
    void updatePortfolio_throwsForbidden_whenRequesterIsNotOwner() {
        when(portfolioRepository.findById("portfolio-1"))
                .thenReturn(Optional.of(portfolio));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> portfolioService.updatePortfolio("portfolio-1", updateDTO, "other-manager"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(portfolioRepository, never()).save(any());
    }

    // ── deletePortfolio ──────────────────────────────────────────────────────

    @Test
    void deletePortfolio_deletesSuccessfully_whenOwnerMatches() {
        when(portfolioRepository.findById("portfolio-1"))
                .thenReturn(Optional.of(portfolio));

        assertDoesNotThrow(() -> portfolioService.deletePortfolio("portfolio-1", "manager-001"));

        verify(portfolioRepository, times(1)).deleteById("portfolio-1");
    }

    @Test
    void deletePortfolio_removesEvidenceAndHoldings_whenNoAuditReportExists() {
        portfolio.setId("portfolio-1");
        ch.zhaw.trueyield.model.Holding holding = new ch.zhaw.trueyield.model.Holding("portfolio-1", "ABC");
        holding.setId("holding-1");
        ch.zhaw.trueyield.model.Evidence evidence = new ch.zhaw.trueyield.model.Evidence("holding-1");
        when(portfolioRepository.findById("portfolio-1")).thenReturn(Optional.of(portfolio));
        when(auditReportRepository.findByPortfolioId("portfolio-1")).thenReturn(List.of());
        when(holdingRepository.findByPortfolioId("portfolio-1")).thenReturn(List.of(holding));
        when(evidenceRepository.findByHoldingId("holding-1")).thenReturn(List.of(evidence));

        portfolioService.deletePortfolio("portfolio-1", "manager-001");

        verify(evidenceRepository).deleteAll(List.of(evidence));
        verify(holdingRepository).deleteAll(List.of(holding));
        verify(portfolioRepository).deleteById("portfolio-1");
    }

    @Test
    void deletePortfolio_preservesEvidence_whenAuditReportsExist() {
        when(portfolioRepository.findById("portfolio-1")).thenReturn(Optional.of(portfolio));
        when(auditReportRepository.findByPortfolioId("portfolio-1")).thenReturn(List.of(mock(AuditReport.class)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> portfolioService.deletePortfolio("portfolio-1", "manager-001"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(holdingRepository, evidenceRepository);
        verify(portfolioRepository, never()).deleteById(anyString());
    }

    @Test
    void deletePortfolio_throwsNotFound_neverCallsDelete() {
        when(portfolioRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> portfolioService.deletePortfolio("nonexistent", "manager-001"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(portfolioRepository, never()).deleteById(anyString());
    }

    @Test
    void deletePortfolio_throwsForbidden_neverCallsDelete() {
        when(portfolioRepository.findById("portfolio-1"))
                .thenReturn(Optional.of(portfolio));

        assertThrows(ResponseStatusException.class,
                () -> portfolioService.deletePortfolio("portfolio-1", "other-manager"));

        verify(portfolioRepository, never()).deleteById(anyString());
    }

    // ── portfolioExists ──────────────────────────────────────────────────────

    @Test
    void portfolioExists_returnsTrue_whenPortfolioExists() {
        when(portfolioRepository.existsById("portfolio-1")).thenReturn(true);

        assertTrue(portfolioService.portfolioExists("portfolio-1"));
    }

    @Test
    void portfolioExists_returnsFalse_whenPortfolioDoesNotExist() {
        when(portfolioRepository.existsById("nonexistent")).thenReturn(false);

        assertFalse(portfolioService.portfolioExists("nonexistent"));
    }

    // ── getLatestAuditStatusByPortfolioIds ───────────────────────────────────

    @Test
    void getLatestAuditStatusByPortfolioIds_returnsStatusMap_forMatchingIds() {
        AuditReport report = mock(AuditReport.class);
        when(report.getPortfolioId()).thenReturn("portfolio-1");
        when(report.getAuditStatus()).thenReturn(AuditStatus.APPROVED);
        when(auditReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(report));

        Map<String, String> result = portfolioService.getLatestAuditStatusByPortfolioIds(List.of("portfolio-1"));

        assertEquals(1, result.size());
        assertEquals("APPROVED", result.get("portfolio-1"));
    }

    @Test
    void getLatestAuditStatusByPortfolioIds_filtersOutUnrelatedPortfolios() {
        AuditReport report = mock(AuditReport.class);
        when(report.getPortfolioId()).thenReturn("other-portfolio");
        when(auditReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(report));

        Map<String, String> result = portfolioService.getLatestAuditStatusByPortfolioIds(List.of("portfolio-1"));

        assertTrue(result.isEmpty());
    }

    @Test
    void getLatestAuditStatusByPortfolioIds_returnsEmpty_whenNoReportsExist() {
        when(auditReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        Map<String, String> result = portfolioService.getLatestAuditStatusByPortfolioIds(List.of("portfolio-1"));

        assertTrue(result.isEmpty());
    }
}
