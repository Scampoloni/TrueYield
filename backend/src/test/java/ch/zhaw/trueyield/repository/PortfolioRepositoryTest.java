package ch.zhaw.trueyield.repository;

import ch.zhaw.trueyield.model.Portfolio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für PortfolioRepository.
 * Testet die Schnittstelle und erwartetes Verhalten der Repository-Methoden.
 * Integration gegen echte MongoDB: manuell mit MONGODB_URI ausführen.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioRepositoryTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    private Portfolio samplePortfolio;

    @BeforeEach
    void setUp() {
        samplePortfolio = new Portfolio("Test Fund", "manager-001");
        samplePortfolio.setDescription("A test portfolio");
    }

    @Test
    void findByFundManagerId_returnsPortfolios_whenManagerHasPortfolios() {
        when(portfolioRepository.findByFundManagerId("manager-001"))
                .thenReturn(List.of(samplePortfolio));

        List<Portfolio> results = portfolioRepository.findByFundManagerId("manager-001");

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals("Test Fund", results.get(0).getName());
        assertEquals("manager-001", results.get(0).getFundManagerId());
    }

    @Test
    void findByFundManagerId_returnsEmptyList_forUnknownManager() {
        when(portfolioRepository.findByFundManagerId("unknown-id"))
                .thenReturn(List.of());

        List<Portfolio> results = portfolioRepository.findByFundManagerId("unknown-id");

        assertTrue(results.isEmpty());
        assertEquals(0, results.size());
    }

    // Parametrisierter Test: verschiedene Manager-IDs → jeder bekommt nur eigene Portfolios
    @ParameterizedTest
    @ValueSource(strings = {"manager-001", "manager-002", "manager-003"})
    void findByFundManagerId_returnsOnlyOwnPortfolios(String managerId) {
        Portfolio ownPortfolio = new Portfolio("Fund for " + managerId, managerId);
        when(portfolioRepository.findByFundManagerId(managerId))
                .thenReturn(List.of(ownPortfolio));

        List<Portfolio> results = portfolioRepository.findByFundManagerId(managerId);

        assertEquals(1, results.size());
        assertEquals(managerId, results.get(0).getFundManagerId());
    }

    @Test
    void findById_returnsEmpty_forNonExistentId() {
        when(portfolioRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        Optional<Portfolio> result = portfolioRepository.findById("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void save_returnsPortfolioWithSetFields() {
        Portfolio toSave = new Portfolio("New Fund", "manager-001");
        when(portfolioRepository.save(toSave)).thenReturn(toSave);

        Portfolio saved = portfolioRepository.save(toSave);

        assertNotNull(saved);
        assertEquals("New Fund", saved.getName());
        assertEquals("manager-001", saved.getFundManagerId());
        verify(portfolioRepository, times(1)).save(toSave);
    }
}
