package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.AuditReport;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.service.AuditReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ch.zhaw.trueyield.security.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AuditReportServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditReportService auditReportService;

    private AuditReport sampleReport;

    private static final String STATE_CHANGE_BODY =
            "{\"auditReportId\":\"report-001\",\"auditorId\":\"auditor-001\"}";

    @BeforeEach
    void setUp() {
        sampleReport = new AuditReport("portfolio-001", AuditStatus.UNDER_REVIEW);
        sampleReport.setAuditorId("auditor-001");
    }

    // ── GET /api/service/auditreport/{id} ────────────────────────────────────

    @Test
    void getAuditReportById_authenticated_returnsOk() throws Exception {
        when(auditReportService.getAuditReportById("report-001")).thenReturn(sampleReport);

        mockMvc.perform(get("/api/service/auditreport/report-001")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value("portfolio-001"))
                .andExpect(jsonPath("$.auditStatus").value("UNDER_REVIEW"));
    }

    @Test
    void getAuditReportById_serviceThrowsNotFound_returns404() throws Exception {
        when(auditReportService.getAuditReportById("missing"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/service/auditreport/missing")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAuditReportById_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/service/auditreport/report-001"))
                .andExpect(status().isForbidden());
    }

    // Parametrisiert: beide Rollen dürfen GET /{id} aufrufen
    @ParameterizedTest
    @ValueSource(strings = {"fund-manager", "esg-auditor"})
    void getAuditReportById_anyAuthenticatedRole_returnsOk(String role) throws Exception {
        when(auditReportService.getAuditReportById(anyString())).thenReturn(sampleReport);

        mockMvc.perform(get("/api/service/auditreport/report-001")
                        .with(user("testuser").roles(role)))
                .andExpect(status().isOk());
    }

    // ── PUT /api/service/auditreport/assign ──────────────────────────────────

    @Test
    void assignAuditReport_asAuditor_returnsOk() throws Exception {
        AuditReport assigned = new AuditReport("portfolio-001", AuditStatus.UNDER_REVIEW);
        assigned.setAuditorId("auditor-001");
        when(auditReportService.assignAuditReport(any())).thenReturn(assigned);

        mockMvc.perform(put("/api/service/auditreport/assign")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.auditorId").value("auditor-001"));
    }

    @Test
    void assignAuditReport_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/assign")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignAuditReport_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignAuditReport_serviceThrowsBadRequest_returns400() throws Exception {
        when(auditReportService.assignAuditReport(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state"));

        mockMvc.perform(put("/api/service/auditreport/assign")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/service/auditreport/reject ──────────────────────────────────

    @Test
    void rejectAuditReport_asAuditor_returnsOk() throws Exception {
        AuditReport rejected = new AuditReport("portfolio-001", AuditStatus.REJECTED);
        rejected.setAuditorId("auditor-001");
        when(auditReportService.rejectAuditReport(any())).thenReturn(rejected);

        mockMvc.perform(put("/api/service/auditreport/reject")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditStatus").value("REJECTED"));
    }

    @Test
    void rejectAuditReport_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/reject")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectAuditReport_serviceThrowsBadRequest_returns400() throws Exception {
        when(auditReportService.rejectAuditReport(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state"));

        mockMvc.perform(put("/api/service/auditreport/reject")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/service/auditreport/complete ────────────────────────────────

    @Test
    void completeAuditReport_asAuditor_returnsOk() throws Exception {
        AuditReport approved = new AuditReport("portfolio-001", AuditStatus.APPROVED);
        approved.setAuditorId("auditor-001");
        when(auditReportService.completeAuditReport(any())).thenReturn(approved);

        mockMvc.perform(put("/api/service/auditreport/complete")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditStatus").value("APPROVED"));
    }

    @Test
    void completeAuditReport_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/complete")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void completeAuditReport_serviceThrowsBadRequest_returns400() throws Exception {
        when(auditReportService.completeAuditReport(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state"));

        mockMvc.perform(put("/api/service/auditreport/complete")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/service/auditreport/dashboard ───────────────────────────────

    @Test
    void getDashboard_authenticated_returnsOk() throws Exception {
        when(auditReportService.getAuditReportDashboard("portfolio-001"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/service/auditreport/dashboard")
                        .param("portfolioId", "portfolio-001")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getDashboard_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/service/auditreport/dashboard")
                        .param("portfolioId", "portfolio-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboard_serviceThrowsBadRequest_returns400() throws Exception {
        when(auditReportService.getAuditReportDashboard("unknown"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Portfolio not found"));

        mockMvc.perform(get("/api/service/auditreport/dashboard")
                        .param("portfolioId", "unknown")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isBadRequest());
    }
}
