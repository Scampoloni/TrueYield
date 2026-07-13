package ch.zhaw.trueyield.model;

import ch.zhaw.trueyield.model.enums.AuditEventType;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "auditEvent")
public class AuditEvent {
    @Id private String id;
    @Indexed private String auditReportId;
    private AuditEventType type;
    private AuditStatus fromStatus;
    private AuditStatus toStatus;
    private String actorId;
    private String detail;
    private LocalDateTime createdAt;

    public AuditEvent(String auditReportId, AuditEventType type, AuditStatus fromStatus,
                      AuditStatus toStatus, String actorId, String detail) {
        this.auditReportId = auditReportId;
        this.type = type;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }
}
