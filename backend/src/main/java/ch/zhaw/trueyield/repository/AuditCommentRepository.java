package ch.zhaw.trueyield.repository;

import java.util.List;

import ch.zhaw.trueyield.model.AuditComment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditCommentRepository extends MongoRepository<AuditComment, String> {

    List<AuditComment> findByAuditReportId(String auditReportId);
}
