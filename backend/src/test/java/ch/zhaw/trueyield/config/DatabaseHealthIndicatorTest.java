package ch.zhaw.trueyield.config;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseHealthIndicatorTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(mongoTemplate);

    @Test
    void health_databaseAcknowledgesPing_returnsUp() {
        when(mongoTemplate.executeCommand(new Document("ping", 1)))
                .thenReturn(new Document("ok", 1.0d));

        assertEquals(Status.UP, indicator.health().getStatus());
    }

    @Test
    void health_databaseRejectsPing_returnsDown() {
        when(mongoTemplate.executeCommand(new Document("ping", 1)))
                .thenReturn(new Document("ok", 0.0d));

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void health_databaseThrows_returnsDown() {
        when(mongoTemplate.executeCommand(new Document("ping", 1)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }
}
