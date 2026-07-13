package ch.zhaw.trueyield.repository;

import ch.zhaw.trueyield.model.AuditEvent;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {
    List<AuditEvent> findByAuditReportIdOrderByCreatedAtAsc(String auditReportId);
}
