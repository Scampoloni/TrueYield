package ch.zhaw.trueyield.repository;

import java.util.List;

import ch.zhaw.trueyield.model.Evidence;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EvidenceRepository extends MongoRepository<Evidence, String> {

    List<Evidence> findByHoldingId(String holdingId);

    boolean existsByHoldingIdAndSourceUrl(String holdingId, String sourceUrl);

    boolean existsBySourceUrl(String sourceUrl);

    long countByHoldingId(String holdingId);
}
