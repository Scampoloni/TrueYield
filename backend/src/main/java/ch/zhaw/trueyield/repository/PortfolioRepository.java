package ch.zhaw.trueyield.repository;

import java.util.List;

import ch.zhaw.trueyield.model.Portfolio;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PortfolioRepository extends MongoRepository<Portfolio, String> {

    List<Portfolio> findByFundManagerId(String fundManagerId);
}
