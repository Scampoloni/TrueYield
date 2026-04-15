package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.Evidence;
import ch.zhaw.trueyield.model.dto.EvidenceCreateDTO;
import ch.zhaw.trueyield.service.EvidenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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
class EvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvidenceService evidenceService;

    private Evidence sampleEvidence;

    @BeforeEach
    void setUp() {
        sampleEvidence = new Evidence("holding-001");
        sampleEvidence.setId("evidence-001");
        sampleEvidence.setSourceUrl("https://example.com/esg-news");
        sampleEvidence.setContentSnippet("ESG evidence snippet longer than 80 chars for headline truncation test in the controller.");
        sampleEvidence.setAiSentimentScore(0.7);
        sampleEvidence.setPublishedAt(LocalDate.of(2024, 4, 1));
    }

    // ── GET /api/evidence?holdingId=... ──────────────────────────────────────

    @Test
    void getEvidence_authenticated_returnsOkWithList() throws Exception {
        when(evidenceService.getEvidenceByHoldingId("holding-001"))
                .thenReturn(List.of(sampleEvidence));

        mockMvc.perform(get("/api/evidence")
                        .param("holdingId", "holding-001")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sentiment").value("POSITIVE"))
                .andExpect(jsonPath("$[0].riskScore").isNumber());
    }

    @Test
    void getEvidence_authenticated_returnsEmptyList() throws Exception {
        when(evidenceService.getEvidenceByHoldingId(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/evidence")
                        .param("holdingId", "holding-empty")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getEvidence_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/evidence")
                        .param("holdingId", "holding-001"))
                .andExpect(status().isForbidden());
    }

    // Parametrisiert: fund-manager und auditor dürfen beide lesen (isAuthenticated)
    @ParameterizedTest
    @ValueSource(strings = {"fund-manager", "esg-auditor"})
    void getEvidence_anyAuthenticatedRole_returnsOk(String role) throws Exception {
        when(evidenceService.getEvidenceByHoldingId(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/evidence")
                        .param("holdingId", "holding-001")
                        .with(user("testuser").roles(role)))
                .andExpect(status().isOk());
    }

    // ── GET /api/evidence/{id} ───────────────────────────────────────────────

    @Test
    void getEvidenceById_authenticated_returnsOk() throws Exception {
        when(evidenceService.getEvidenceById("evidence-001")).thenReturn(sampleEvidence);

        mockMvc.perform(get("/api/evidence/evidence-001")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentiment").value("POSITIVE"))
                .andExpect(jsonPath("$.riskScore").isNumber());
    }

    @Test
    void getEvidenceById_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/evidence/evidence-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEvidenceById_notFound_returns404() throws Exception {
        when(evidenceService.getEvidenceById("unknown"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found: unknown"));

        mockMvc.perform(get("/api/evidence/unknown")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/evidence ───────────────────────────────────────────────────

    @Test
    void createEvidence_asFundManager_returnsCreated() throws Exception {
        when(evidenceService.createEvidence(any(EvidenceCreateDTO.class))).thenReturn(sampleEvidence);

        mockMvc.perform(post("/api/evidence")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"holdingId":"holding-001","sourceUrl":"https://example.com","contentSnippet":"This is a valid ESG news snippet."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sentiment").value("POSITIVE"))
                .andExpect(jsonPath("$.riskScore").isNumber());
    }

    @Test
    void createEvidence_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/evidence")
                        .with(user("auditor").roles("esg-auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"holdingId":"holding-001","contentSnippet":"Some snippet."}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEvidence_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"holdingId":"holding-001","contentSnippet":"Some snippet."}
                                """))
                .andExpect(status().isForbidden());
    }

    // Parametrisiert: Pflichtfelder blank → 400 Bad Request
    @ParameterizedTest
    @CsvSource({
        "'',           'Valid snippet text'",
        "'   ',        'Valid snippet text'",
        "'holding-001', ''",
        "'holding-001', '   '"
    })
    void createEvidence_withBlankRequiredFields_returnsBadRequest(
            String holdingId, String contentSnippet) throws Exception {
        mockMvc.perform(post("/api/evidence")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"holdingId\":\"" + holdingId + "\",\"contentSnippet\":\"" + contentSnippet + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/evidence/{id} ────────────────────────────────────────────

    @Test
    void deleteEvidence_asFundManager_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/evidence/evidence-001")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEvidence_asAuditor_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/evidence/evidence-001")
                        .with(user("auditor").roles("esg-auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEvidence_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/evidence/evidence-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEvidence_serviceThrowsNotFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"))
                .when(evidenceService).deleteEvidence("not-found");

        mockMvc.perform(delete("/api/evidence/not-found")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isNotFound());
    }
}
