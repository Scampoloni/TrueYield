package ch.zhaw.trueyield.repository;

import java.util.List;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.dto.AuditReportAggregationDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditReportRepository extends MongoRepository<AuditReport, String> {

    List<AuditReport> findByPortfolioId(String portfolioId);

    List<AuditReport> findByAuditStatus(AuditStatus status);

    @Aggregation(pipeline = {
        "{ $match: { portfolioId: ?0 } }",
        "{ $group: { _id: '$auditStatus', count: { $sum: 1 }, itemIds: { $push: '$_id' } } }"
    })
    List<AuditReportAggregationDTO> aggregateByPortfolioId(String portfolioId);
}
