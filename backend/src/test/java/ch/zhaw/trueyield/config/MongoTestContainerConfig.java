package ch.zhaw.trueyield.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.MongoDBContainer;

@TestConfiguration
public class MongoTestContainerConfig {

    private static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    static {
        mongo.start();
        System.setProperty("spring.mongodb.uri", mongo.getConnectionString());
    }
}
