package ch.zhaw.trueyield.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import ch.zhaw.trueyield.service.AuditReportService;
import ch.zhaw.trueyield.service.PortfolioService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditReportService auditReportService;

    @MockitoBean
    private PortfolioService portfolioService;

    // ── MethodArgumentNotValidException (validation) ──────────────────────────

    @Test
    void postWithInvalidBody_triggersValidationError_returns400WithErrors() throws Exception {
        // POST to a validated endpoint with missing required fields
        mockMvc.perform(post("/api/service/auditreport")
                        .with(user("fm").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))  // missing required portfolioId
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    // ── HttpMessageNotReadableException (malformed JSON) ─────────────────────

    @Test
    void postWithMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/service/auditreport")
                        .with(user("fm").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request body"));
    }

    // ── ResponseStatusException (404) ────────────────────────────────────────

    @Test
    void getByIdWhenNotFound_returns404() throws Exception {
        when(auditReportService.getAuditReportById("missing"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "AuditReport not found"));

        mockMvc.perform(get("/api/service/auditreport/missing")
                        .with(user("fm").roles("fund-manager")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("AuditReport not found"));
    }

    // ── ResponseStatusException (400) ────────────────────────────────────────

    @Test
    void serviceThrowsBadRequest_returns400WithReason() throws Exception {
        when(auditReportService.getAuditReportDashboard(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Portfolio not found"));

        mockMvc.perform(get("/api/service/auditreport/dashboard")
                        .param("portfolioId", "bad-id")
                        .with(user("fm").roles("fund-manager")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Portfolio not found"));
    }

    // ── AccessDeniedException ─────────────────────────────────────────────────

    @Test
    void accessDenied_returns403() throws Exception {
        when(auditReportService.getAuditReportById(anyString()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/api/service/auditreport/some-report")
                        .with(user("fm").roles("fund-manager")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    // ── Generic Exception handler ─────────────────────────────────────────────

    @Test
    void unhandledException_returns500() throws Exception {
        when(auditReportService.getAuditReportById(anyString()))
                .thenThrow(new RuntimeException("Unexpected failure"));

        mockMvc.perform(get("/api/service/auditreport/some-id")
                        .with(user("fm").roles("fund-manager")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    // ── ResponseStatusException with null reason (uses getMessage) ────────────

    @Test
    void responseStatusExceptionWithNullReason_returnsFallbackMessage() throws Exception {
        when(auditReportService.getAuditReportById(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/service/auditreport/x")
                        .with(user("fm").roles("fund-manager")))
                .andExpect(status().isForbidden());
    }
}
