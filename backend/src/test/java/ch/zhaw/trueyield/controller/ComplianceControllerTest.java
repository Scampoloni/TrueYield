package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.ComplianceOverviewDTO;
import ch.zhaw.trueyield.model.dto.EsgEvidenceSignalDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.model.enums.EsgEvidenceSignal;
import ch.zhaw.trueyield.service.ComplianceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplianceService complianceService;

    private ComplianceOverviewDTO sampleOverview() {
        return new ComplianceOverviewDTO(3L, 12L, 5L, Map.of(
                "APPROVED", 2L, "PENDING_REVIEW", 1L, "UNDER_REVIEW", 1L,
                "REJECTED", 0L, "AI_ANALYZING", 1L
        ));
    }

    private Portfolio samplePortfolio() {
        Portfolio p = new Portfolio();
        p.setId("p1");
        p.setName("Green Alpha Fund");
        p.setFundManagerId("mgr-001");
        return p;
    }

    private AuditReport sampleReport() {
        AuditReport r = new AuditReport("p1", AuditStatus.PENDING_REVIEW);
        r.setId("r1");
        return r;
    }

    // ── /esg-signals ─────────────────────────────────────────────────────────

    @Test
    void getEsgSignals_asComplianceOfficer_returnsOk() throws Exception {
        when(complianceService.getEsgEvidenceSignals()).thenReturn(
                List.of(new EsgEvidenceSignalDTO("p1", "Green Alpha Fund", EsgEvidenceSignal.FAVOURABLE, 0.5, 3)));

        mockMvc.perform(get("/api/compliance/esg-signals")
                        .with(user("officer").roles("compliance-officer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].portfolioId").value("p1"))
                .andExpect(jsonPath("$[0].signal").value("FAVOURABLE"));
    }

    @Test
    void getEsgSignals_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/compliance/esg-signals")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEsgSignals_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/compliance/esg-signals")
                        .with(user("auditor").roles("auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEsgSignals_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/compliance/esg-signals"))
                .andExpect(status().isUnauthorized());
    }

    // ── /overview ────────────────────────────────────────────────────────────

    @Test
    void getOverview_asComplianceOfficer_returnsOk() throws Exception {
        when(complianceService.getOverview()).thenReturn(sampleOverview());

        mockMvc.perform(get("/api/compliance/overview")
                        .with(user("officer").roles("compliance-officer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPortfolios").value(3))
                .andExpect(jsonPath("$.totalHoldings").value(12))
                .andExpect(jsonPath("$.totalAuditReports").value(5));
    }

    @Test
    void getOverview_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/compliance/overview")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOverview_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/compliance/overview")
                        .with(user("auditor").roles("auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOverview_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/compliance/overview"))
                .andExpect(status().isUnauthorized());
    }

    // ── /portfolios ──────────────────────────────────────────────────────────

    @Test
    void getPortfolios_asComplianceOfficer_returnsOkWithList() throws Exception {
        when(complianceService.getAllPortfolios()).thenReturn(List.of(samplePortfolio()));

        mockMvc.perform(get("/api/compliance/portfolios")
                        .with(user("officer").roles("compliance-officer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Green Alpha Fund"))
                .andExpect(jsonPath("$[0].fundManagerId").value("mgr-001"));
    }

    @Test
    void getPortfolios_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/compliance/portfolios")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPortfolios_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/compliance/portfolios")
                        .with(user("auditor").roles("auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPortfolios_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/compliance/portfolios"))
                .andExpect(status().isUnauthorized());
    }

    // ── /reports ─────────────────────────────────────────────────────────────

    @Test
    void getReports_asComplianceOfficer_returnsOkWithList() throws Exception {
        when(complianceService.getAllReports()).thenReturn(List.of(sampleReport()));

        mockMvc.perform(get("/api/compliance/reports")
                        .with(user("officer").roles("compliance-officer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].portfolioId").value("p1"))
                .andExpect(jsonPath("$[0].auditStatus").value("PENDING_REVIEW"));
    }

    @Test
    void getReports_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/compliance/reports")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReports_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/compliance/reports")
                        .with(user("auditor").roles("auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReports_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/compliance/reports"))
                .andExpect(status().isUnauthorized());
    }
}
