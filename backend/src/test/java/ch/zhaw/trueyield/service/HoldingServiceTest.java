package ch.zhaw.trueyield.service;

import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.HoldingCreateDTO;
import ch.zhaw.trueyield.repository.HoldingRepository;
import ch.zhaw.trueyield.security.AccessControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private AccessControlService accessControlService;

    @Mock
    private NewsIngestionService newsIngestionService;

    @InjectMocks
    private HoldingService holdingService;

    private Holding sampleHolding;
    private HoldingCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        sampleHolding = new Holding("portfolio-001", "AAPL");
        sampleHolding.setId("holding-001");
        sampleHolding.setIsin("US0378331005");
        sampleHolding.setName("Apple Inc.");
        sampleHolding.setWeightPercent(10.0);

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
        doNothing().when(accessControlService).requirePortfolioAccess("portfolio-001");
        when(holdingRepository.findByPortfolioId("portfolio-001"))
                .thenReturn(List.of(sampleHolding));

        List<Holding> result = holdingService.getHoldingsByPortfolioId("portfolio-001");

        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
        assertEquals("portfolio-001", result.get(0).getPortfolioId());
    }

    @Test
    void getHoldingsByPortfolioId_returnsEmptyList_whenNoHoldings() {
        doNothing().when(accessControlService).requirePortfolioAccess("portfolio-empty");
        when(holdingRepository.findByPortfolioId("portfolio-empty")).thenReturn(List.of());

        List<Holding> result = holdingService.getHoldingsByPortfolioId("portfolio-empty");

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"portfolio-001", "portfolio-002", "portfolio-003"})
    void getHoldingsByPortfolioId_isolatesPerPortfolio(String portfolioId) {
        doNothing().when(accessControlService).requirePortfolioAccess(portfolioId);
        Holding ownHolding = new Holding(portfolioId, "MSFT");
        when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(ownHolding));

        List<Holding> result = holdingService.getHoldingsByPortfolioId(portfolioId);

        assertEquals(1, result.size());
        assertEquals(portfolioId, result.get(0).getPortfolioId());
    }

    // ── deleteHolding ────────────────────────────────────────────────────────

    @Test
    void deleteHolding_deletesSuccessfully_whenHoldingExists() {
        Holding holding = new Holding("portfolio-001", "AAPL");
        holding.setId("holding-1");
        when(accessControlService.requireHoldingAccess("holding-1")).thenReturn(holding);

        assertDoesNotThrow(() -> holdingService.deleteHolding("holding-1"));

        verify(holdingRepository, times(1)).deleteById("holding-1");
    }

    @Test
    void deleteHolding_throwsNotFound_neverCallsDelete() {
        when(accessControlService.requireHoldingAccess("nonexistent"))
            .thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND));

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> holdingService.deleteHolding("nonexistent"));

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(holdingRepository, never()).deleteById(anyString());
    }

    // ── createHolding ────────────────────────────────────────────────────────

    @Test
    void createHolding_savesHolding_whenPortfolioExists() {
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        when(holdingRepository.save(any(Holding.class))).thenReturn(sampleHolding);

        Holding result = holdingService.createHolding(createDTO);

        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        assertEquals("portfolio-001", result.getPortfolioId());
        assertEquals("Apple Inc.", result.getName());
        assertEquals(10.0, result.getWeightPercent());
        verify(holdingRepository, times(1)).save(any(Holding.class));
    }

    @Test
    void createHolding_triggersNewsIngestion_withCompanyName() {
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        when(holdingRepository.save(any(Holding.class))).thenReturn(sampleHolding);

        holdingService.createHolding(createDTO);

        verify(newsIngestionService, times(1)).ingestNewsForHolding("holding-001", "Apple Inc.", "AAPL");
    }

    @Test
    void createHolding_usesSymbolAsCompanyName_whenNameIsNull() {
        HoldingCreateDTO dtoNoName = mock(HoldingCreateDTO.class);
        when(dtoNoName.getPortfolioId()).thenReturn("portfolio-001");
        when(dtoNoName.getSymbol()).thenReturn("MSFT");
        when(dtoNoName.getName()).thenReturn(null);
        Holding savedHolding = new Holding("portfolio-001", "MSFT");
        savedHolding.setId("holding-002");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        when(holdingRepository.save(any(Holding.class))).thenReturn(savedHolding);

        holdingService.createHolding(dtoNoName);

        verify(newsIngestionService, times(1)).ingestNewsForHolding("holding-002", "MSFT", "MSFT");
    }

    @Test
    void createHolding_usesSymbolAsCompanyName_whenNameIsBlank() {
        HoldingCreateDTO dtoBlankName = mock(HoldingCreateDTO.class);
        when(dtoBlankName.getPortfolioId()).thenReturn("portfolio-001");
        when(dtoBlankName.getSymbol()).thenReturn("GOOGL");
        when(dtoBlankName.getName()).thenReturn("   ");
        Holding savedHolding = new Holding("portfolio-001", "GOOGL");
        savedHolding.setId("holding-003");
        doNothing().when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");
        when(holdingRepository.save(any(Holding.class))).thenReturn(savedHolding);

        holdingService.createHolding(dtoBlankName);

        verify(newsIngestionService, times(1)).ingestNewsForHolding("holding-003", "GOOGL", "GOOGL");
    }

    @Test
    void createHolding_throwsBadRequest_whenPortfolioNotFound() {
        doThrow(new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST))
            .when(accessControlService).requireFundManagerPortfolioAccess("portfolio-001");

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> holdingService.createHolding(createDTO));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(holdingRepository, never()).save(any());
    }
}
