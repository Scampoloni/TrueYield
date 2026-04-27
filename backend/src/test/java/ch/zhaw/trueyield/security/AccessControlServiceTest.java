package ch.zhaw.trueyield.security;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.repository.EvidenceRepository;
import ch.zhaw.trueyield.repository.HoldingRepository;
import ch.zhaw.trueyield.service.PortfolioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock private UserService userService;
    @Mock private PortfolioService portfolioService;
    @Mock private AuditReportRepository auditReportRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private EvidenceRepository evidenceRepository;

    @InjectMocks
    private AccessControlService accessControlService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ── requirePortfolioAccess ────────────────────────────────────────────────

    @Test
    void requirePortfolioAccess_asComplianceOfficer_passes() {
        when(userService.userHasRole("compliance-officer")).thenReturn(true);
        assertDoesNotThrow(() -> accessControlService.requirePortfolioAccess("p-1"));
        verifyNoInteractions(portfolioService);
    }

    @Test
    void requirePortfolioAccess_asAuditor_withPendingReport_passes() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-1");

        AuditReport pending = new AuditReport("p-1", AuditStatus.PENDING_REVIEW);
        when(auditReportRepository.findByPortfolioId("p-1")).thenReturn(List.of(pending));

        assertDoesNotThrow(() -> accessControlService.requirePortfolioAccess("p-1"));
    }

    @Test
    void requirePortfolioAccess_asAuditor_withAssignedReport_passes() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-1");

        AuditReport assigned = new AuditReport("p-1", AuditStatus.UNDER_REVIEW);
        assigned.setAuditorId("auditor-1");
        when(auditReportRepository.findByPortfolioId("p-1")).thenReturn(List.of(assigned));

        assertDoesNotThrow(() -> accessControlService.requirePortfolioAccess("p-1"));
    }

    @Test
    void requirePortfolioAccess_asAuditor_noAccessibleReport_throwsForbidden() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-1");

        AuditReport other = new AuditReport("p-1", AuditStatus.UNDER_REVIEW);
        other.setAuditorId("other-auditor");
        when(auditReportRepository.findByPortfolioId("p-1")).thenReturn(List.of(other));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requirePortfolioAccess("p-1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requirePortfolioAccess_asAuditor_noReports_throwsForbidden() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-1");
        when(auditReportRepository.findByPortfolioId("p-1")).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requirePortfolioAccess("p-1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requirePortfolioAccess_asFundManager_delegatesToPortfolioService() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(false);
        when(userService.getCurrentUserId()).thenReturn("fm-1");

        assertDoesNotThrow(() -> accessControlService.requirePortfolioAccess("p-1"));
        verify(portfolioService).getPortfolioById("p-1", "fm-1");
    }

    // ── requireFundManagerPortfolioAccess ─────────────────────────────────────

    @Test
    void requireFundManagerPortfolioAccess_asAuditor_throwsForbidden() {
        when(userService.userHasRole("auditor")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireFundManagerPortfolioAccess("p-1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireFundManagerPortfolioAccess_asComplianceOfficer_throwsForbidden() {
        when(userService.userHasRole("auditor")).thenReturn(false);
        when(userService.userHasRole("compliance-officer")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireFundManagerPortfolioAccess("p-1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireFundManagerPortfolioAccess_asFundManager_delegatesToPortfolioService() {
        when(userService.userHasRole("auditor")).thenReturn(false);
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.getCurrentUserId()).thenReturn("fm-1");

        assertDoesNotThrow(() -> accessControlService.requireFundManagerPortfolioAccess("p-1"));
        verify(portfolioService).getPortfolioById("p-1", "fm-1");
    }

    // ── requireHoldingAccess ──────────────────────────────────────────────────

    @Test
    void requireHoldingAccess_asComplianceOfficer_returnsHolding() {
        Holding holding = new Holding("p-1", "AAPL");
        holding.setId("h-1");
        when(holdingRepository.findById("h-1")).thenReturn(Optional.of(holding));
        when(userService.userHasRole("compliance-officer")).thenReturn(true);

        Holding result = accessControlService.requireHoldingAccess("h-1");
        assertSame(holding, result);
    }

    @Test
    void requireHoldingAccess_holdingNotFound_throwsNotFound() {
        when(holdingRepository.findById("h-99")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireHoldingAccess("h-99"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void requireHoldingAccess_asFundManager_wrongOwner_throwsForbidden() {
        Holding holding = new Holding("p-1", "AAPL");
        holding.setId("h-1");
        when(holdingRepository.findById("h-1")).thenReturn(Optional.of(holding));
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(false);
        when(userService.getCurrentUserId()).thenReturn("fm-other");
        when(portfolioService.getPortfolioById("p-1", "fm-other"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireHoldingAccess("h-1"));
    }

    // ── requireEvidenceAccess ─────────────────────────────────────────────────

    @Test
    void requireEvidenceAccess_asComplianceOfficer_returnsEvidence() {
        Evidence evidence = new Evidence("h-1");
        evidence.setId("e-1");
        Holding holding = new Holding("p-1", "AAPL");
        holding.setId("h-1");

        when(evidenceRepository.findById("e-1")).thenReturn(Optional.of(evidence));
        when(holdingRepository.findById("h-1")).thenReturn(Optional.of(holding));
        when(userService.userHasRole("compliance-officer")).thenReturn(true);

        Evidence result = accessControlService.requireEvidenceAccess("e-1");
        assertSame(evidence, result);
    }

    @Test
    void requireEvidenceAccess_evidenceNotFound_throwsNotFound() {
        when(evidenceRepository.findById("e-99")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireEvidenceAccess("e-99"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ── requireAuditReportAccess ──────────────────────────────────────────────

    @Test
    void requireAuditReportAccess_asComplianceOfficer_passes() {
        when(userService.userHasRole("compliance-officer")).thenReturn(true);
        AuditReport report = new AuditReport("p-1", AuditStatus.UNDER_REVIEW);

        assertDoesNotThrow(() -> accessControlService.requireAuditReportAccess(report));
    }

    @Test
    void requireAuditReportAccess_asAuditor_pendingReport_passes() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-1");

        AuditReport report = new AuditReport("p-1", AuditStatus.PENDING_REVIEW);
        assertDoesNotThrow(() -> accessControlService.requireAuditReportAccess(report));
    }

    @Test
    void requireAuditReportAccess_asAuditor_assignedReport_passes() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-1");

        AuditReport report = new AuditReport("p-1", AuditStatus.UNDER_REVIEW);
        report.setAuditorId("auditor-1");
        assertDoesNotThrow(() -> accessControlService.requireAuditReportAccess(report));
    }

    @Test
    void requireAuditReportAccess_asAuditor_reportAssignedToOther_throwsForbidden() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-1");

        AuditReport report = new AuditReport("p-1", AuditStatus.UNDER_REVIEW);
        report.setAuditorId("other-auditor");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireAuditReportAccess(report));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireAuditReportAccess_asAuditor_reportWithNullAuditorId_throwsForbidden() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-1");

        AuditReport report = new AuditReport("p-1", AuditStatus.UNDER_REVIEW);
        report.setAuditorId(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireAuditReportAccess(report));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireAuditReportAccess_asFundManager_ownPortfolio_passes() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(false);
        when(userService.getCurrentUserId()).thenReturn("fm-1");

        AuditReport report = new AuditReport("p-1", AuditStatus.UNDER_REVIEW);
        assertDoesNotThrow(() -> accessControlService.requireAuditReportAccess(report));
        verify(portfolioService).getPortfolioById("p-1", "fm-1");
    }

    // ── requireAuditorId ─────────────────────────────────────────────────────

    @Test
    void requireAuditorId_asAuditor_returnsUserId() {
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-42");

        assertEquals("auditor-42", accessControlService.requireAuditorId());
    }

    @Test
    void requireAuditorId_asFundManager_throwsForbidden() {
        when(userService.userHasRole("auditor")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireAuditorId());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ── auditorCanAccessPortfolio (via requirePortfolioAccess) ─────────────────

    @Test
    void requirePortfolioAccess_asAuditor_rejectedReportForOtherAuditor_throwsForbidden() {
        when(userService.userHasRole("compliance-officer")).thenReturn(false);
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(userService.getCurrentUserId()).thenReturn("auditor-1");

        AuditReport rejected = new AuditReport("p-1", AuditStatus.REJECTED);
        rejected.setAuditorId("auditor-2");
        when(auditReportRepository.findByPortfolioId("p-1")).thenReturn(List.of(rejected));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessControlService.requirePortfolioAccess("p-1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
