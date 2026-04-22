package ch.zhaw.trueyield.chat;

import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.service.EvidenceService;
import ch.zhaw.trueyield.service.HoldingService;
import ch.zhaw.trueyield.service.PortfolioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsgChatToolsTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private HoldingService holdingService;

    @Mock
    private EvidenceService evidenceService;

    @InjectMocks
    private EsgChatTools esgChatTools;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllPortfolios_returnsFormattedList() {
        setAuthentication("auditor-user", "ROLE_auditor");

        Portfolio p1 = new Portfolio("Green Alpha", "fm-1");
        p1.setId("p-1");
        Portfolio p2 = new Portfolio("Impact Core", "fm-2");
        p2.setId("p-2");

        when(portfolioService.getAllPortfolios()).thenReturn(List.of(p1, p2));

        String result = esgChatTools.getAllPortfolios();

        assertTrue(result.startsWith("Portfolios:"));
        assertTrue(result.contains("Green Alpha (id: p-1)"));
        assertTrue(result.contains("Impact Core (id: p-2)"));
    }

    @Test
    void getAllPortfolios_noPortfolios_returnsNoResults() {
        setAuthentication("auditor-user", "ROLE_auditor");
        when(portfolioService.getAllPortfolios()).thenReturn(List.of());

        String result = esgChatTools.getAllPortfolios();

        assertEquals("No results found", result);
    }

    @Test
    void createPortfolio_asFundManager_succeeds() {
        setAuthentication("fund-manager-user", "ROLE_fund-manager");

        Portfolio created = new Portfolio("Green Alpha", "fund-manager-user");
        created.setId("p-123");
        when(portfolioService.createPortfolio(any(PortfolioCreateDTO.class), anyString())).thenReturn(created);

        String result = esgChatTools.createPortfolio(" Green Alpha ", " Core strategy ");

        ArgumentCaptor<PortfolioCreateDTO> dtoCaptor = ArgumentCaptor.forClass(PortfolioCreateDTO.class);
        verify(portfolioService).createPortfolio(dtoCaptor.capture(), anyString());
        assertEquals("Green Alpha", dtoCaptor.getValue().getName());
        assertEquals("Core strategy", dtoCaptor.getValue().getDescription());
        assertEquals("Created portfolio 'Green Alpha' (id: p-123)", result);
    }

    @Test
    void createPortfolio_asAuditor_throwsAccessDenied() {
        setAuthentication("auditor-user", "ROLE_auditor");

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> esgChatTools.createPortfolio("Any", "Any")
        );

        assertEquals("Only fund-managers can create portfolios", exception.getMessage());
    }

    @Test
    void createHolding_withUnknownPortfolio_returnsNotFound() {
        setAuthentication("fund-manager-user", "ROLE_fund-manager");
        when(portfolioService.getAllPortfolios()).thenReturn(List.of());

        String result = esgChatTools.createHolding("Unknown", "New Holding", "abc");

        assertEquals("Portfolio 'Unknown' not found", result);
    }

    private void setAuthentication(String username, String role) {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(username, "n/a", role);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
