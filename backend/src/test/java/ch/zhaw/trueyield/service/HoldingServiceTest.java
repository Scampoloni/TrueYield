package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.HoldingCreateDTO;
import ch.zhaw.trueyield.repository.HoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private HoldingService holdingService;

    private Holding sampleHolding;
    private HoldingCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        sampleHolding = new Holding("portfolio-001", "AAPL");
        sampleHolding.setIsin("US0378331005");
        sampleHolding.setName("Apple Inc.");
        sampleHolding.setWeightPercent(10.0);

        // DTO hat keine Setter (Lombok @Getter only) → via Mockito simulieren
        // lenient: nicht jeder Test braucht das DTO
        createDTO = mock(HoldingCreateDTO.class);
        lenient().when(createDTO.getPortfolioId()).thenReturn("portfolio-001");
        lenient().when(createDTO.getSymbol()).thenReturn("AAPL");
        lenient().when(createDTO.getIsin()).thenReturn("US0378331005");
        lenient().when(createDTO.getName()).thenReturn("Apple Inc.");
        lenient().when(createDTO.getWeightPercent()).thenReturn(10.0);
    }

    // ── getHoldingsByPortfolioId ─────────────────────────────────────────────

    @Test
    void getHoldingsByPortfolioId_returnsHoldings_whenHoldingsExist() {
        when(holdingRepository.findByPortfolioId("portfolio-001"))
                .thenReturn(List.of(sampleHolding));

        List<Holding> result = holdingService.getHoldingsByPortfolioId("portfolio-001");

        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
        assertEquals("portfolio-001", result.get(0).getPortfolioId());
    }

    @Test
    void getHoldingsByPortfolioId_returnsEmptyList_whenNoHoldings() {
        when(holdingRepository.findByPortfolioId("portfolio-empty")).thenReturn(List.of());

        List<Holding> result = holdingService.getHoldingsByPortfolioId("portfolio-empty");

        assertTrue(result.isEmpty());
    }

    // Parametrisiert: verschiedene Portfolio-IDs → jedes Portfolio bekommt nur eigene Holdings
    @ParameterizedTest
    @ValueSource(strings = {"portfolio-001", "portfolio-002", "portfolio-003"})
    void getHoldingsByPortfolioId_isolatesPerPortfolio(String portfolioId) {
        Holding ownHolding = new Holding(portfolioId, "MSFT");
        when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(ownHolding));

        List<Holding> result = holdingService.getHoldingsByPortfolioId(portfolioId);

        assertEquals(1, result.size());
        assertEquals(portfolioId, result.get(0).getPortfolioId());
    }

    // ── deleteHolding ────────────────────────────────────────────────────────

    @Test
    void deleteHolding_deletesSuccessfully_whenHoldingExists() {
        when(holdingRepository.existsById("holding-1")).thenReturn(true);

        assertDoesNotThrow(() -> holdingService.deleteHolding("holding-1"));

        verify(holdingRepository, times(1)).deleteById("holding-1");
    }

    @Test
    void deleteHolding_throwsNotFound_neverCallsDelete() {
        when(holdingRepository.existsById("nonexistent")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> holdingService.deleteHolding("nonexistent"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(holdingRepository, never()).deleteById(anyString());
    }

    // ── createHolding ────────────────────────────────────────────────────────

    @Test
    void createHolding_savesHolding_whenOwnerAndPortfolioExist() {
        ch.zhaw.trueyield.model.Portfolio portfolio =
                new ch.zhaw.trueyield.model.Portfolio("ESG Fund", "manager-001");
        when(portfolioService.getPortfolioById("portfolio-001", "manager-001")).thenReturn(portfolio);
        when(holdingRepository.save(any(Holding.class))).thenReturn(sampleHolding);

        Holding result = holdingService.createHolding(createDTO, "manager-001");

        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        assertEquals("portfolio-001", result.getPortfolioId());
        assertEquals("Apple Inc.", result.getName());
        assertEquals(10.0, result.getWeightPercent());
        verify(holdingRepository, times(1)).save(any(Holding.class));
    }

    @Test
    void createHolding_throwsNotFound_whenPortfolioMissing() {
        when(portfolioService.getPortfolioById("portfolio-001", "manager-001"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> holdingService.createHolding(createDTO, "manager-001"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(holdingRepository, never()).save(any());
    }

    @Test
    void createHolding_throwsForbidden_whenRequesterIsNotOwner() {
        when(portfolioService.getPortfolioById("portfolio-001", "other-manager"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> holdingService.createHolding(createDTO, "other-manager"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(holdingRepository, never()).save(any());
    }
}
