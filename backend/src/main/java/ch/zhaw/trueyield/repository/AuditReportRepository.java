package ch.zhaw.trueyield.repository;

import java.util.List;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditReportRepository extends MongoRepository<AuditReport, String> {

    List<AuditReport> findByPortfolioId(String portfolioId);

    List<AuditReport> findByAuditStatus(AuditStatus status);
}
