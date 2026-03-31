package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditComment;
import ch.zhaw.trueyield.model.dto.AuditCommentCreateDTO;
import ch.zhaw.trueyield.repository.AuditCommentRepository;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditCommentServiceTest {

    @Mock
    private AuditCommentRepository auditCommentRepository;

    @Mock
    private AuditReportRepository auditReportRepository;

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
        when(auditReportRepository.existsById("report-001")).thenReturn(true);
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
        when(auditReportRepository.existsById("report-001")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditCommentService.createComment(dto, "auditor-001"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(auditCommentRepository, never()).save(any());
    }

    // ── getCommentsByReportId ────────────────────────────────────────────────

    @Test
    void getCommentsByReportId_returnsList() {
        AuditComment c1 = new AuditComment("report-001", "Comment A", "auditor-001");
        AuditComment c2 = new AuditComment("report-001", "Comment B", "auditor-001");
        when(auditCommentRepository.findByAuditReportId("report-001")).thenReturn(List.of(c1, c2));

        List<AuditComment> result = auditCommentService.getCommentsByReportId("report-001");

        assertEquals(2, result.size());
        verify(auditCommentRepository, times(1)).findByAuditReportId("report-001");
    }

    @Test
    void getCommentsByReportId_returnsEmptyList_whenNoComments() {
        when(auditCommentRepository.findByAuditReportId("report-empty")).thenReturn(List.of());

        List<AuditComment> result = auditCommentService.getCommentsByReportId("report-empty");

        assertTrue(result.isEmpty());
    }
}
