package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.HoldingCreateDTO;
import ch.zhaw.trueyield.service.HoldingService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HoldingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HoldingService holdingService;

    private Holding sampleHolding;

    @BeforeEach
    void setUp() {
        sampleHolding = new Holding("portfolio-001", "AAPL");
        sampleHolding.setIsin("US0378331005");
        sampleHolding.setName("Apple Inc.");
        sampleHolding.setWeightPercent(10.0);
    }

    // ── GET /api/holding?portfolioId=... ─────────────────────────────────────

    @Test
    void getHoldings_authenticated_returnsOkWithList() throws Exception {
        when(holdingService.getHoldingsByPortfolioId("portfolio-001"))
                .thenReturn(List.of(sampleHolding));

        mockMvc.perform(get("/api/holding")
                        .param("portfolioId", "portfolio-001")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].portfolioId").value("portfolio-001"));
    }

    @Test
    void getHoldings_authenticated_returnsEmptyList() throws Exception {
        when(holdingService.getHoldingsByPortfolioId(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/holding")
                        .param("portfolioId", "portfolio-empty")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getHoldings_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/holding")
                        .param("portfolioId", "portfolio-001"))
                .andExpect(status().isForbidden());
    }

    // Parametrisiert: Auditor und Fund-Manager dürfen beide lesen
    @ParameterizedTest
    @ValueSource(strings = {"fund-manager", "esg-auditor"})
    void getHoldings_anyAuthenticatedRole_returnsOk(String role) throws Exception {
        when(holdingService.getHoldingsByPortfolioId(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/holding")
                        .param("portfolioId", "portfolio-001")
                        .with(user("testuser").roles(role)))
                .andExpect(status().isOk());
    }

    // ── POST /api/holding ────────────────────────────────────────────────────

    @Test
    void createHolding_asFundManager_returnsCreated() throws Exception {
        when(holdingService.createHolding(any(HoldingCreateDTO.class))).thenReturn(sampleHolding);

        mockMvc.perform(post("/api/holding")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"portfolioId":"portfolio-001","symbol":"AAPL","isin":"US0378331005","name":"Apple Inc.","weightPercent":10.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.portfolioId").value("portfolio-001"));
    }

    @Test
    void createHolding_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/holding")
                        .with(user("auditor").roles("esg-auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"portfolioId":"portfolio-001","symbol":"AAPL"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createHolding_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/holding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"portfolioId":"portfolio-001","symbol":"AAPL"}
                                """))
                .andExpect(status().isForbidden());
    }

    // Parametrisiert: Pflichtfelder blank → 400 Bad Request
    @ParameterizedTest
    @CsvSource({
        "'',    'AAPL'",
        "'   ', 'AAPL'",
        "'portfolio-001', ''",
        "'portfolio-001', '   '"
    })
    void createHolding_withBlankRequiredFields_returnsBadRequest(
            String portfolioId, String symbol) throws Exception {
        mockMvc.perform(post("/api/holding")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"portfolioId\":\"" + portfolioId + "\",\"symbol\":\"" + symbol + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/holding/{id} ─────────────────────────────────────────────

    @Test
    void deleteHolding_asFundManager_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/holding/holding-1")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteHolding_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/holding/holding-1")
                        .with(user("auditor").roles("esg-auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteHolding_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/holding/holding-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteHolding_serviceThrowsNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found"))
                .when(holdingService).deleteHolding("not-found");

        mockMvc.perform(delete("/api/holding/not-found")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isNotFound());
    }
}
