package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.dto.ComplianceOverviewDTO;
import ch.zhaw.trueyield.service.ComplianceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
}
