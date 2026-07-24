package ch.zhaw.trueyield.config;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthIndicator.class);

    private final MongoTemplate mongoTemplate;

    public DatabaseHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        try {
            Document result = mongoTemplate.executeCommand(new Document("ping", 1));
            Number ok = result.get("ok", Number.class);
            if (ok != null && ok.doubleValue() == 1.0d) {
                return Health.up().withDetail("service", "MongoDB").build();
            }
            return Health.down()
                    .withDetail("service", "MongoDB")
                    .withDetail("reason", "Database ping was not acknowledged")
                    .build();
        } catch (RuntimeException exception) {
            log.error("MongoDB readiness check failed: {}", exception.getMessage());
            return Health.down()
                    .withDetail("service", "MongoDB")
                    .withException(exception)
                    .build();
        }
    }
}
