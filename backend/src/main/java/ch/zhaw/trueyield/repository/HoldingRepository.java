package ch.zhaw.trueyield.repository;

import java.util.List;

import ch.zhaw.trueyield.model.Holding;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HoldingRepository extends MongoRepository<Holding, String> {

    List<Holding> findByPortfolioId(String portfolioId);
}
