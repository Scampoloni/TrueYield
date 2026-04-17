package ch.zhaw.trueyield.repository;

import ch.zhaw.trueyield.model.Portfolio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Slice-Integration-Test für PortfolioRepository gegen eine echte MongoDB.
 * Läuft mit Testcontainers (Docker) oder einer lokalen MongoDB auf 27017.
 */
@DataMongoTest
class PortfolioRepositoryTest {

    private static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");
    private static final boolean DOCKER_AVAILABLE = DockerClientFactory.instance().isDockerAvailable();

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            if (!mongo.isRunning()) mongo.start();
            registry.add("spring.data.mongodb.uri",
                    () -> mongo.getConnectionString() + "/trueyield-repo-test");
        } else {
            registry.add("spring.data.mongodb.uri",
                    () -> "mongodb://localhost:27017/trueyield-repo-test");
        }
    }

    @BeforeAll
    static void requireMongo() {
        assumeTrue(
                DOCKER_AVAILABLE || isLocalMongoReachable(),
                "Skipping PortfolioRepositoryTest: neither Docker/Testcontainers nor local MongoDB on 27017 is available."
        );
    }

    private static boolean isLocalMongoReachable() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", 27017), 500);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    @Autowired
    private PortfolioRepository portfolioRepository;

    @BeforeEach
    void cleanUp() {
        portfolioRepository.deleteAll();
    }

    @Test
    void save_andFindById_returnsStoredPortfolio() {
        Portfolio p = new Portfolio("Test Fund", "manager-001");
        p.setDescription("A test portfolio");
        Portfolio saved = portfolioRepository.save(p);

        Optional<Portfolio> found = portfolioRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Fund", found.get().getName());
        assertEquals("manager-001", found.get().getFundManagerId());
        assertEquals("A test portfolio", found.get().getDescription());
    }

    @Test
    void findById_returnsEmpty_forNonExistentId() {
        Optional<Portfolio> result = portfolioRepository.findById("nonexistent-id");
        assertTrue(result.isEmpty());
    }

    @Test
    void findByFundManagerId_returnsOnlyOwnPortfolios() {
        portfolioRepository.save(new Portfolio("Fund A", "manager-001"));
        portfolioRepository.save(new Portfolio("Fund B", "manager-002"));

        List<Portfolio> results = portfolioRepository.findByFundManagerId("manager-001");
        assertEquals(1, results.size());
        assertEquals("manager-001", results.get(0).getFundManagerId());
    }

    @Test
    void findByFundManagerId_returnsEmptyList_forUnknownManager() {
        portfolioRepository.save(new Portfolio("Fund A", "manager-001"));

        List<Portfolio> results = portfolioRepository.findByFundManagerId("unknown-manager");
        assertTrue(results.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"manager-001", "manager-002", "manager-003"})
    void findByFundManagerId_isolatesPerManager(String managerId) {
        portfolioRepository.save(new Portfolio("Own Fund", managerId));
        portfolioRepository.save(new Portfolio("Other Fund", "other-manager"));

        List<Portfolio> results = portfolioRepository.findByFundManagerId(managerId);
        assertEquals(1, results.size());
        assertEquals(managerId, results.get(0).getFundManagerId());
    }
}
