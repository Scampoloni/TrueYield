package ch.zhaw.trueyield.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import ch.zhaw.trueyield.model.Portfolio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PortfolioRepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @AfterEach
    void cleanup() {
        portfolioRepository.deleteAll();
    }

    @Test
    void testSaveAndFindByFundManagerId() {
        // Arrange – Lombok @RequiredArgsConstructor erzeugt Konstruktor für
        // @NonNull-Felder
        Portfolio portfolio = new Portfolio("Test Fund", "manager-001");
        portfolio = portfolioRepository.save(portfolio);

        // Assert – ID wird von MongoDB vergeben
        assertNotNull(portfolio.getId());
        assertEquals("Test Fund", portfolio.getName());
        assertEquals("manager-001", portfolio.getFundManagerId());

        // Act – Derived Query testen
        List<Portfolio> results = portfolioRepository.findByFundManagerId("manager-001");

        // Assert
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals("Test Fund", results.get(0).getName());
    }

    @Test
    void testFindByFundManagerIdReturnsEmptyForUnknownId() {
        // Act
        List<Portfolio> results = portfolioRepository.findByFundManagerId("unknown-id");

        // Assert
        assertEquals(0, results.size());
    }
}
