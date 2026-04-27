package ch.zhaw.trueyield.chat;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.HoldingCreateDTO;
import ch.zhaw.trueyield.security.AccessControlService;
import ch.zhaw.trueyield.service.EvidenceService;
import ch.zhaw.trueyield.service.HoldingService;
import ch.zhaw.trueyield.service.PortfolioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsgChatToolsExtendedTest {

    @Mock private PortfolioService portfolioService;
    @Mock private HoldingService holdingService;
    @Mock private EvidenceService evidenceService;
    @Mock private AccessControlService accessControlService;

    @InjectMocks
    private EsgChatTools esgChatTools;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String username, String role) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(username, "n/a", role);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Portfolio portfolio(String id, String name, String fundManagerId) {
        Portfolio p = new Portfolio(name, fundManagerId);
        p.setId(id);
        return p;
    }

    private Holding holding(String id, String portfolioId, String symbol, String name) {
        Holding h = new Holding(portfolioId, symbol);
        h.setId(id);
        h.setName(name);
        return h;
    }

    private Evidence evidence(String id, String holdingId, String url, Double sentiment) {
        Evidence e = new Evidence(holdingId);
        e.setId(id);
        e.setSourceUrl(url);
        e.setAiSentimentScore(sentiment);
        return e;
    }

    // ── getHoldingsByPortfolioName ────────────────────────────────────────────

    @Test
    void getHoldingsByPortfolioName_emptyInput_returnsNoResults() {
        setAuthentication("auditor-user", "ROLE_auditor");
        assertEquals("No results found", esgChatTools.getHoldingsByPortfolioName("  "));
    }

    @Test
    void getHoldingsByPortfolioName_portfolioNotFound_returnsNotFound() {
        setAuthentication("auditor-user", "ROLE_auditor");
        when(portfolioService.getAllPortfolios()).thenReturn(List.of());

        String result = esgChatTools.getHoldingsByPortfolioName("Unknown Fund");
        assertEquals("Portfolio 'Unknown Fund' not found", result);
    }

    @Test
    void getHoldingsByPortfolioName_portfolioFoundNoHoldings_returnsNoResults() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Green Alpha", "fm-1");
        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));
        when(holdingService.getHoldingsByPortfolioId("p-1")).thenReturn(List.of());

        String result = esgChatTools.getHoldingsByPortfolioName("Green Alpha");
        assertEquals("No results found", result);
    }

    @Test
    void getHoldingsByPortfolioName_portfolioFoundWithHoldings_returnsFormattedList() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Green Alpha", "fm-1");
        Holding h1 = holding("h-1", "p-1", "AAPL", "Apple");
        Holding h2 = holding("h-2", "p-1", "TSLA", "Tesla");

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));
        when(holdingService.getHoldingsByPortfolioId("p-1")).thenReturn(List.of(h1, h2));

        String result = esgChatTools.getHoldingsByPortfolioName("Green Alpha");
        assertTrue(result.startsWith("Holdings for 'Green Alpha':"));
        assertTrue(result.contains("Apple (symbol: AAPL"));
        assertTrue(result.contains("Tesla (symbol: TSLA"));
    }

    @Test
    void getHoldingsByPortfolioName_holdingWithNullName_usesSymbol() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Green Alpha", "fm-1");
        Holding h = holding("h-1", "p-1", "MSFT", null);

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));
        when(holdingService.getHoldingsByPortfolioId("p-1")).thenReturn(List.of(h));

        String result = esgChatTools.getHoldingsByPortfolioName("Green Alpha");
        assertTrue(result.contains("MSFT"));
    }

    // ── getEvidenceByHoldingName ──────────────────────────────────────────────

    @Test
    void getEvidenceByHoldingName_emptyInput_returnsNoResults() {
        setAuthentication("auditor-user", "ROLE_auditor");
        assertEquals("No results found", esgChatTools.getEvidenceByHoldingName(""));
    }

    @Test
    void getEvidenceByHoldingName_holdingNotFound_returnsNoResults() {
        setAuthentication("auditor-user", "ROLE_auditor");
        when(portfolioService.getAllPortfolios()).thenReturn(List.of());

        assertEquals("No results found", esgChatTools.getEvidenceByHoldingName("Unknown Holding"));
    }

    @Test
    void getEvidenceByHoldingName_holdingFoundNoEvidence_returnsNoResults() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Green Alpha", "fm-1");
        Holding h = holding("h-1", "p-1", "AAPL", "Apple");

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));
        when(holdingService.getHoldingsByPortfolioId("p-1")).thenReturn(List.of(h));
        when(evidenceService.getEvidenceByHoldingId("h-1")).thenReturn(List.of());

        assertEquals("No results found", esgChatTools.getEvidenceByHoldingName("Apple"));
    }

    @Test
    void getEvidenceByHoldingName_holdingFoundWithEvidence_returnsFormattedList() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Green Alpha", "fm-1");
        Holding h = holding("h-1", "p-1", "AAPL", "Apple");
        Evidence e1 = evidence("e-1", "h-1", "https://news.example.com/1", 0.85);
        Evidence e2 = evidence("e-2", "h-1", "https://news.example.com/2", null);

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));
        when(holdingService.getHoldingsByPortfolioId("p-1")).thenReturn(List.of(h));
        when(evidenceService.getEvidenceByHoldingId("h-1")).thenReturn(List.of(e1, e2));

        String result = esgChatTools.getEvidenceByHoldingName("Apple");
        assertTrue(result.startsWith("Evidence for 'Apple':"));
        assertTrue(result.contains("0.85"));
        assertTrue(result.contains("n/a"));
        assertTrue(result.contains("https://news.example.com/1"));
    }

    @Test
    void getEvidenceByHoldingName_foundBySymbolWhenNameIsNull_returnsEvidence() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Green Alpha", "fm-1");
        Holding h = holding("h-1", "p-1", "msft", null);
        Evidence e = evidence("e-1", "h-1", "https://source.com", 0.5);

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));
        when(holdingService.getHoldingsByPortfolioId("p-1")).thenReturn(List.of(h));
        when(evidenceService.getEvidenceByHoldingId("h-1")).thenReturn(List.of(e));

        String result = esgChatTools.getEvidenceByHoldingName("msft");
        assertTrue(result.contains("msft"));
    }

    // ── createPortfolio ───────────────────────────────────────────────────────

    @Test
    void createPortfolio_blankName_returnsNoResults() {
        setAuthentication("fm-user", "ROLE_fund-manager");
        assertEquals("No results found", esgChatTools.createPortfolio("   ", "desc"));
    }

    @Test
    void createPortfolio_nullDescriptionIsHandled() {
        setAuthentication("fm-user", "ROLE_fund-manager");
        Portfolio created = portfolio("p-new", "My Portfolio", "fm-user");
        when(portfolioService.createPortfolio(any(), anyString())).thenReturn(created);

        String result = esgChatTools.createPortfolio("My Portfolio", null);
        assertTrue(result.startsWith("Created portfolio"));
        assertTrue(result.contains("My Portfolio"));
    }

    @Test
    void createPortfolio_blankDescription_treatedAsNull() {
        setAuthentication("fm-user", "ROLE_fund-manager");
        Portfolio created = portfolio("p-new", "My Portfolio", "fm-user");
        when(portfolioService.createPortfolio(any(), anyString())).thenReturn(created);

        String result = esgChatTools.createPortfolio("My Portfolio", "   ");
        assertTrue(result.contains("My Portfolio"));
    }

    // ── createHolding ─────────────────────────────────────────────────────────

    @Test
    void createHolding_emptyHoldingName_returnsNoResults() {
        setAuthentication("fm-user", "ROLE_fund-manager");
        String result = esgChatTools.createHolding("Some Portfolio", "  ", "AAPL");
        assertEquals("No results found", result);
    }

    @Test
    void createHolding_emptyTicker_returnsNoResults() {
        setAuthentication("fm-user", "ROLE_fund-manager");
        String result = esgChatTools.createHolding("Some Portfolio", "Apple", "  ");
        assertEquals("No results found", result);
    }

    @Test
    void createHolding_emptyPortfolioName_returnsNoResults() {
        setAuthentication("fm-user", "ROLE_fund-manager");
        String result = esgChatTools.createHolding("  ", "Apple", "AAPL");
        assertEquals("No results found", result);
    }

    @Test
    void createHolding_success_returnsCreatedMessage() {
        setAuthentication("fm-user", "ROLE_fund-manager");
        Portfolio p = portfolio("p-1", "Green Alpha", "fm-user");
        Holding created = holding("h-new", "p-1", "AAPL", "Apple");

        when(portfolioService.getAllPortfoliosByFundManager("fm-user")).thenReturn(List.of(p));
        when(holdingService.createHolding(any(HoldingCreateDTO.class))).thenReturn(created);

        String result = esgChatTools.createHolding("Green Alpha", "Apple", "aapl");
        assertTrue(result.startsWith("Created holding 'Apple'"));
        assertTrue(result.contains("AAPL"));
        assertTrue(result.contains("Green Alpha"));
    }

    @Test
    void createHolding_asAuditor_throwsAccessDenied() {
        setAuthentication("auditor-user", "ROLE_auditor");
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> esgChatTools.createHolding("P", "H", "T"));
    }

    // ── getVisiblePortfolios (auditor path) ───────────────────────────────────

    @Test
    void getVisiblePortfolios_asAuditor_callsGetAllPortfolios() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Fund A", "fm-1");
        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));

        String result = esgChatTools.getAllPortfolios();
        assertTrue(result.contains("Fund A"));
    }

    @Test
    void getVisiblePortfolios_asComplianceOfficer_callsGetAllPortfolios() {
        setAuthentication("co-user", "ROLE_compliance-officer");
        Portfolio p = portfolio("p-1", "Fund B", "fm-1");
        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));

        String result = esgChatTools.getAllPortfolios();
        assertTrue(result.contains("Fund B"));
    }

    // ── hasRole with no authorities (warn branch) ─────────────────────────────

    @Test
    void getAllPortfolios_userWithNoAuthorities_usesGetAllPortfolios() {
        // User is authenticated but has no roles — triggers the warn branch in hasRole()
        TestingAuthenticationToken auth = new TestingAuthenticationToken("no-roles-user", "n/a");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(portfolioService.getAllPortfolios()).thenReturn(List.of());
        assertEquals("No results found", esgChatTools.getAllPortfolios());
    }

    // ── currentUsername fallback ──────────────────────────────────────────────

    @Test
    void getAllPortfolios_withNullAuthentication_usesAnonymousUsername() {
        // No authentication set — SecurityContextHolder has null auth
        SecurityContextHolder.clearContext();
        when(portfolioService.getAllPortfolios()).thenReturn(List.of());

        // Should not throw — logs "anonymous" as username
        assertEquals("No results found", esgChatTools.getAllPortfolios());
    }

    // ── portfolio with null name in getHoldingsByPortfolioName ─────────────────

    @Test
    void getHoldingsByPortfolioName_portfolioWithNullName_handledGracefully() {
        setAuthentication("auditor-user", "ROLE_auditor");
        // Portfolio with null name — normalize(null) returns ""
        Portfolio p = new Portfolio("Real Name", "fm-1");
        p.setId("p-1");
        p.setName(null);  // force null name after construction

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));

        // querying "some name" won't match null — returns "not found"
        String result = esgChatTools.getHoldingsByPortfolioName("some name");
        assertTrue(result.contains("not found") || result.equals("No results found"));
    }

    // ── formatSentiment via evidence output ───────────────────────────────────

    @Test
    void getEvidenceByHoldingName_sentimentNullFormatsAsNa() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Fund", "fm-1");
        Holding h = holding("h-1", "p-1", "X", "XCorp");
        Evidence e = evidence("e-1", "h-1", "https://url.com", null);

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));
        when(holdingService.getHoldingsByPortfolioId("p-1")).thenReturn(List.of(h));
        when(evidenceService.getEvidenceByHoldingId("h-1")).thenReturn(List.of(e));

        String result = esgChatTools.getEvidenceByHoldingName("xcorp");
        assertTrue(result.contains("n/a"));
    }

    @Test
    void getEvidenceByHoldingName_sentimentFormattedToTwoDecimals() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Fund", "fm-1");
        Holding h = holding("h-1", "p-1", "X", "XCorp");
        Evidence e = evidence("e-1", "h-1", "https://url.com", 0.12345);

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));
        when(holdingService.getHoldingsByPortfolioId("p-1")).thenReturn(List.of(h));
        when(evidenceService.getEvidenceByHoldingId("h-1")).thenReturn(List.of(e));

        String result = esgChatTools.getEvidenceByHoldingName("xcorp");
        assertTrue(result.contains("0.12"));
    }

    // ── findHoldingByName — portfolio has no matching holding ─────────────────

    @Test
    void getEvidenceByHoldingName_portfolioHasHoldingsButNoneMatch_returnsNoResults() {
        setAuthentication("auditor-user", "ROLE_auditor");
        Portfolio p = portfolio("p-1", "Fund", "fm-1");
        Holding h = holding("h-1", "p-1", "TSLA", "Tesla");

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p));
        when(holdingService.getHoldingsByPortfolioId("p-1")).thenReturn(List.of(h));

        assertEquals("No results found", esgChatTools.getEvidenceByHoldingName("Apple"));
    }
}
