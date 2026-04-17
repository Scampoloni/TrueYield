package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditComment;
import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.AuditCommentCreateDTO;
import ch.zhaw.trueyield.repository.AuditCommentRepository;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.security.AccessControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditCommentServiceTest {

    @Mock
    private AuditCommentRepository auditCommentRepository;

    @Mock
    private AuditReportRepository auditReportRepository;

    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private AuditCommentService auditCommentService;

    private AuditCommentCreateDTO dto;

    @BeforeEach
    void setUp() {
        dto = mock(AuditCommentCreateDTO.class);
        lenient().when(dto.getAuditReportId()).thenReturn("report-001");
        lenient().when(dto.getComment()).thenReturn("Looks compliant.");
    }

    // ── createComment ────────────────────────────────────────────────────────

    @Test
    void createComment_savesComment_whenReportExists() {
        AuditReport report = new AuditReport("portfolio-001", ch.zhaw.trueyield.model.enums.AuditStatus.UNDER_REVIEW);
        report.setId("report-001");
        when(auditReportRepository.findById("report-001")).thenReturn(Optional.of(report));
        org.mockito.Mockito.doNothing().when(accessControlService).requireAuditReportAccess(report);
        when(auditCommentRepository.save(any(AuditComment.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditComment result = auditCommentService.createComment(dto, "auditor-001");

        assertNotNull(result);
        assertEquals("report-001", result.getAuditReportId());
        assertEquals("Looks compliant.", result.getComment());
        assertEquals("auditor-001", result.getAuditorId());
        assertNotNull(result.getCreatedAt());
        verify(auditCommentRepository, times(1)).save(any(AuditComment.class));
    }

    @Test
    void createComment_throwsBadRequest_whenReportNotFound() {
        when(auditReportRepository.findById("report-001")).thenReturn(Optional.empty());

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> auditCommentService.createComment(dto, "auditor-001"));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(auditCommentRepository, never()).save(any());
    }

    @Test
    void createComment_throwsForbidden_whenAccessDenied() {
        AuditReport report = new AuditReport("portfolio-001", ch.zhaw.trueyield.model.enums.AuditStatus.UNDER_REVIEW);
        report.setId("report-001");
        when(auditReportRepository.findById("report-001")).thenReturn(Optional.of(report));
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN))
                .when(accessControlService).requireAuditReportAccess(report);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> auditCommentService.createComment(dto, "auditor-001"));

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(auditCommentRepository, never()).save(any());
    }

    // ── getCommentsByReportId ────────────────────────────────────────────────

    @Test
    void getCommentsByReportId_returnsList() {
        AuditComment c1 = new AuditComment("report-001", "Comment A", "auditor-001");
        AuditComment c2 = new AuditComment("report-001", "Comment B", "auditor-001");
        AuditReport report = new AuditReport("portfolio-001", ch.zhaw.trueyield.model.enums.AuditStatus.UNDER_REVIEW);
        report.setId("report-001");
        when(auditReportRepository.findById("report-001")).thenReturn(Optional.of(report));
        org.mockito.Mockito.doNothing().when(accessControlService).requireAuditReportAccess(report);
        when(auditCommentRepository.findByAuditReportId("report-001")).thenReturn(List.of(c1, c2));

        List<AuditComment> result = auditCommentService.getCommentsByReportId("report-001");

        assertEquals(2, result.size());
        verify(auditCommentRepository, times(1)).findByAuditReportId("report-001");
    }

    @Test
    void getCommentsByReportId_returnsEmptyList_whenNoComments() {
        AuditReport report = new AuditReport("portfolio-001", ch.zhaw.trueyield.model.enums.AuditStatus.UNDER_REVIEW);
        report.setId("report-empty");
        when(auditReportRepository.findById("report-empty")).thenReturn(Optional.of(report));
        org.mockito.Mockito.doNothing().when(accessControlService).requireAuditReportAccess(report);
        when(auditCommentRepository.findByAuditReportId("report-empty")).thenReturn(List.of());

        List<AuditComment> result = auditCommentService.getCommentsByReportId("report-empty");

        assertTrue(result.isEmpty());
    }

    @Test
    void getCommentsByReportId_throwsBadRequest_whenReportMissing() {
        when(auditReportRepository.findById("missing")).thenReturn(Optional.empty());

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> auditCommentService.getCommentsByReportId("missing"));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getCommentsByReportId_throwsForbidden_whenAccessDenied() {
        AuditReport report = new AuditReport("portfolio-001", ch.zhaw.trueyield.model.enums.AuditStatus.UNDER_REVIEW);
        report.setId("report-001");
        when(auditReportRepository.findById("report-001")).thenReturn(Optional.of(report));
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN))
                .when(accessControlService).requireAuditReportAccess(report);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> auditCommentService.getCommentsByReportId("report-001"));

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
