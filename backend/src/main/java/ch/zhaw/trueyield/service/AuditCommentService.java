package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.AuditComment;
import ch.zhaw.trueyield.model.dto.AuditCommentCreateDTO;
import ch.zhaw.trueyield.repository.AuditCommentRepository;
import ch.zhaw.trueyield.repository.AuditReportRepository;
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

    public AuditComment createComment(AuditCommentCreateDTO dto, String auditorId) {
        if (!auditReportRepository.existsById(dto.getAuditReportId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AuditReport not found: " + dto.getAuditReportId());
        }
        AuditComment comment = new AuditComment(dto.getAuditReportId(), dto.getComment(), auditorId);
        comment.setCreatedAt(LocalDateTime.now());
        return auditCommentRepository.save(comment);
    }

    public List<AuditComment> getCommentsByReportId(String auditReportId) {
        return auditCommentRepository.findByAuditReportId(auditReportId);
    }
}
