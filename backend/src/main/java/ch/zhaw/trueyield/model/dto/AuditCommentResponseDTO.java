package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.AuditComment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AuditCommentResponseDTO {

    private String id;
    private String auditReportId;
    private String comment;
    private String auditorId;
    private LocalDateTime createdAt;

    public static AuditCommentResponseDTO fromEntity(AuditComment comment) {
        return new AuditCommentResponseDTO(
                comment.getId(),
                comment.getAuditReportId(),
                comment.getComment(),
                comment.getAuditorId(),
                comment.getCreatedAt()
        );
    }
}
