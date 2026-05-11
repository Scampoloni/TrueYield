package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.repository.AuditReportRepository;
import ch.zhaw.trueyield.security.UserService;
import ch.zhaw.trueyield.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioService portfolioService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuditReportRepository auditReportRepository;

    private Portfolio samplePortfolio;

    @BeforeEach
    void setUp() {
        when(userService.getCurrentUserId()).thenReturn("user-123");
        when(portfolioService.getLatestAuditStatusByPortfolioIds(anyList())).thenReturn(new HashMap<>());

        samplePortfolio = new Portfolio("ESG Global Fund", "user-123");
        samplePortfolio.setDescription("A diversified ESG portfolio");
    }

    // ── POST /api/portfolio ──────────────────────────────────────────────────

    @Test
    void createPortfolio_asFundManager_returnsCreated() throws Exception {
        when(portfolioService.createPortfolio(any(PortfolioCreateDTO.class), anyString()))
                .thenReturn(samplePortfolio);

        mockMvc.perform(post("/api/portfolio")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ESG Global Fund","description":"A diversified ESG portfolio"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ESG Global Fund"))
                .andExpect(jsonPath("$.fundManagerId").value("user-123"))
                .andExpect(jsonPath("$.description").value("A diversified ESG portfolio"));
    }

    @Test
    void createPortfolio_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/portfolio")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Should be forbidden"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPortfolio_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Should be forbidden"}
                                """))
                                .andExpect(status().isUnauthorized());
    }

    // Parametrisierter Test: verschiedene leere Namen → 400 Bad Request
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void createPortfolio_withBlankName_returnsBadRequest(String blankName) throws Exception {
        mockMvc.perform(post("/api/portfolio")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + blankName + "\",\"description\":\"test\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/portfolio ───────────────────────────────────────────────────

    @Test
    void getAllPortfolios_authenticated_returnsOkWithList() throws Exception {
        when(portfolioService.getAllPortfoliosByFundManager("user-123"))
                .thenReturn(List.of(samplePortfolio));

        mockMvc.perform(get("/api/portfolio")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("ESG Global Fund"))
                .andExpect(jsonPath("$[0].fundManagerId").value("user-123"));
    }

    @Test
    void getAllPortfolios_authenticated_returnsEmptyList() throws Exception {
        when(portfolioService.getAllPortfoliosByFundManager(anyString()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/portfolio")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllPortfolios_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/portfolio"))
                                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/portfolio/{id} ──────────────────────────────────────────────

    @Test
    void getPortfolioById_ownedByUser_returnsOk() throws Exception {
        when(portfolioService.getPortfolioById("abc-123", "user-123"))
                .thenReturn(samplePortfolio);

        mockMvc.perform(get("/api/portfolio/abc-123")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ESG Global Fund"));
    }

    @Test
    void getPortfolioById_asAuditor_returnsOk() throws Exception {
        when(userService.userHasRole("auditor")).thenReturn(true);
        when(portfolioService.getPortfolioByIdForAuditor("abc-123")).thenReturn(samplePortfolio);

        mockMvc.perform(get("/api/portfolio/abc-123")
                        .with(user("auditor").roles("auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ESG Global Fund"));
    }

    // Parametrisierter Test mit CsvSource: 404 und 403 als Fehlerszenarien
    @ParameterizedTest
    @CsvSource({
        "abc-other, FORBIDDEN,  Access denied",
        "not-found, NOT_FOUND,  Portfolio not found"
    })
    void getPortfolioById_errorScenarios_returnsCorrectStatus(
            String portfolioId, String httpStatus, String message) throws Exception {
        HttpStatus status = HttpStatus.valueOf(httpStatus.trim());
        when(portfolioService.getPortfolioById(eq(portfolioId), anyString()))
                .thenThrow(new ResponseStatusException(status, message.trim()));

        mockMvc.perform(get("/api/portfolio/" + portfolioId)
                        .with(user("manager").roles("fund-manager")))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertEquals(
                        status.value(), result.getResponse().getStatus()));
    }

    // ── PUT /api/portfolio/{id} ──────────────────────────────────────────────

    @Test
    void updatePortfolio_asFundManager_returnsOk() throws Exception {
        Portfolio updated = new Portfolio("Updated Fund", "user-123");
        updated.setDescription("Updated description");
        when(portfolioService.updatePortfolio(eq("abc-123"), any(), anyString()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/portfolio/abc-123")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Fund","description":"Updated description"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Fund"));
    }

    @Test
    void updatePortfolio_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/portfolio/abc-123")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Should be forbidden"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePortfolio_serviceThrowsNotFound_returns404() throws Exception {
        when(portfolioService.updatePortfolio(eq("not-found"), any(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));

        mockMvc.perform(put("/api/portfolio/not-found")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Fund"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/portfolio/{id} ───────────────────────────────────────────

    @Test
    void deletePortfolio_asFundManager_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/portfolio/abc-123")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePortfolio_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/portfolio/abc-123")
                        .with(user("auditor").roles("auditor")))
                .andExpect(status().isForbidden());
    }
}
