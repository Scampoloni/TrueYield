package ch.zhaw.trueyield.model.dto;

import ch.zhaw.trueyield.model.AuditEvent;
import ch.zhaw.trueyield.model.enums.AuditEventType;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import java.time.LocalDateTime;

public record AuditEventResponseDTO(String id, AuditEventType type, AuditStatus fromStatus,
        AuditStatus toStatus, String actorId, String detail, LocalDateTime createdAt) {
    public static AuditEventResponseDTO fromEntity(AuditEvent event) {
        return new AuditEventResponseDTO(event.getId(), event.getType(), event.getFromStatus(),
                event.getToStatus(), event.getActorId(), event.getDetail(), event.getCreatedAt());
    }
}
