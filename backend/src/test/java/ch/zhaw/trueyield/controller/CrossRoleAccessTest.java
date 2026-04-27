package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.security.UserService;
import ch.zhaw.trueyield.service.AuditCommentService;
import ch.zhaw.trueyield.service.AuditReportService;
import ch.zhaw.trueyield.service.HoldingService;
import ch.zhaw.trueyield.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-role integration tests: verifies that each role is blocked from
 * endpoints it must not access, and that shared read endpoints work for both.
 *
 * Note on 403 vs 401 for unauthenticated requests:
 * The test-SecurityConfig permits all requests at the HTTP level and relies
 * solely on @PreAuthorize. Spring Security's default AccessDeniedException
 * for anonymous users results in 403, not 401, in this setup.
 *
 * Note on role names:
 * Controllers use hasRole('auditor') — Spring prepends ROLE_ internally, so
 * tests must pass .roles("auditor"), not .roles("esg-auditor").
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrossRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditReportService auditReportService;

    @MockitoBean
    private AuditCommentService auditCommentService;

    @MockitoBean
    private PortfolioService portfolioService;

    @MockitoBean
    private HoldingService holdingService;

    @MockitoBean
    private UserService userService;

    private static final String STATE_CHANGE_BODY =
            "{\"auditReportId\":\"report-001\",\"auditorId\":\"auditor-001\"}";

    private static final String PORTFOLIO_CREATE_BODY =
            "{\"name\":\"My Fund\",\"description\":\"desc\"}";

    private static final String AUDIT_COMMENT_BODY =
            "{\"auditReportId\":\"report-001\",\"comment\":\"looks good\"}";

    // ── Fund Manager blocked from auditor-only audit endpoints ──────────────

    @Test
    void assign_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/assign")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void complete_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/complete")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void reject_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/reject")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAuditComment_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/service/auditcomment")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AUDIT_COMMENT_BODY))
                .andExpect(status().isForbidden());
    }

    // ── ESG Auditor blocked from fund-manager-only portfolio endpoints ───────

    @Test
    void createPortfolio_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/portfolio")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PORTFOLIO_CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePortfolio_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/portfolio/portfolio-001")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePortfolio_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/portfolio/portfolio-001")
                        .with(user("auditor").roles("auditor")))
                .andExpect(status().isForbidden());
    }

    // ── Dashboard endpoint: fund-manager and compliance-officer only ─────────

    @ParameterizedTest
    @ValueSource(strings = {"fund-manager", "compliance-officer"})
    void getDashboard_allowedRole_returnsOk(String role) throws Exception {
        when(auditReportService.getAuditReportDashboard(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/service/auditreport/dashboard")
                        .param("portfolioId", "portfolio-001")
                        .with(user("testuser").roles(role)))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboard_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/service/auditreport/dashboard")
                        .param("portfolioId", "portfolio-001")
                        .with(user("testuser").roles("auditor")))
                .andExpect(status().isForbidden());
    }

        // ── Unauthenticated requests are rejected with 401 ───────────────────────
        // (resource server returns 401 for missing authentication)

    @Test
    void assign_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                                .andExpect(status().isUnauthorized());
    }

    @Test
    void complete_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                                .andExpect(status().isUnauthorized());
    }

    @Test
    void reject_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/service/auditreport/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATE_CHANGE_BODY))
                                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAuditComment_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/service/auditcomment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AUDIT_COMMENT_BODY))
                                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPortfolio_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PORTFOLIO_CREATE_BODY))
                                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletePortfolio_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/portfolio/portfolio-001"))
                                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDashboard_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/service/auditreport/dashboard")
                        .param("portfolioId", "portfolio-001"))
                                .andExpect(status().isUnauthorized());
    }
}
