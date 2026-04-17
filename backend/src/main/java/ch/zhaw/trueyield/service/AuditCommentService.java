package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditComment;
import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.AuditCommentCreateDTO;
import ch.zhaw.trueyield.repository.AuditCommentRepository;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.security.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditCommentService {

    @Autowired
    private AuditCommentRepository auditCommentRepository;

    @Autowired
    private AuditReportRepository auditReportRepository;

    @Autowired
    private AccessControlService accessControlService;

    public AuditComment createComment(AuditCommentCreateDTO dto, String auditorId) {
        AuditReport report = auditReportRepository.findById(dto.getAuditReportId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + dto.getAuditReportId()));
        accessControlService.requireAuditReportAccess(report);
        AuditComment comment = new AuditComment(dto.getAuditReportId(), dto.getComment(), auditorId);
        comment.setCreatedAt(LocalDateTime.now());
        return auditCommentRepository.save(comment);
    }

    public List<AuditComment> getCommentsByReportId(String auditReportId) {
        AuditReport report = auditReportRepository.findById(auditReportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "AuditReport not found: " + auditReportId));
        accessControlService.requireAuditReportAccess(report);
        return auditCommentRepository.findByAuditReportId(auditReportId);
    }
}
