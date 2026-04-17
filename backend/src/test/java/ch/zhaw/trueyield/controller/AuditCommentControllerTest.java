package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.AuditComment;
import ch.zhaw.trueyield.security.UserService;
import ch.zhaw.trueyield.service.AuditCommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditCommentService auditCommentService;

    @MockitoBean
    private UserService userService;

    private AuditComment sampleComment;

    private static final String VALID_BODY =
            "{\"auditReportId\":\"report-001\",\"comment\":\"Looks compliant.\"}";

    @BeforeEach
    void setUp() {
        when(userService.getCurrentUserId()).thenReturn("auditor-001");
        sampleComment = new AuditComment("report-001", "Looks compliant.", "auditor-001");
    }

    // ── GET /api/service/auditcomment?auditReportId=... ──────────────────────

    @Test
    void getComments_authenticated_returnsOkWithList() throws Exception {
        when(auditCommentService.getCommentsByReportId("report-001"))
                .thenReturn(List.of(sampleComment));

        mockMvc.perform(get("/api/service/auditcomment")
                        .param("auditReportId", "report-001")
                        .with(user("auditor").roles("auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].comment").value("Looks compliant."))
                .andExpect(jsonPath("$[0].auditorId").value("auditor-001"));
    }

    @Test
    void getComments_authenticated_returnsEmptyList() throws Exception {
        when(auditCommentService.getCommentsByReportId(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/service/auditcomment")
                        .param("auditReportId", "report-empty")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getComments_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/service/auditcomment")
                        .param("auditReportId", "report-001"))
                                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/service/auditcomment ───────────────────────────────────────

    @Test
    void createComment_asAuditor_returnsCreated() throws Exception {
        when(auditCommentService.createComment(any(), anyString())).thenReturn(sampleComment);

        mockMvc.perform(post("/api/service/auditcomment")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value("Looks compliant."))
                .andExpect(jsonPath("$.auditReportId").value("report-001"));
    }

    @Test
    void createComment_asFundManager_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/service/auditcomment")
                        .with(user("manager").roles("fund-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createComment_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/service/auditcomment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                                .andExpect(status().isUnauthorized());
    }

    @Test
    void createComment_serviceThrowsBadRequest_returns400() throws Exception {
        when(auditCommentService.createComment(any(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "AuditReport not found"));

        mockMvc.perform(post("/api/service/auditcomment")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest());
    }

    // Parametrisiert: Pflichtfelder blank → 400 Bad Request
    @ParameterizedTest
    @CsvSource({
        "'',            'Looks compliant.'",
        "'   ',         'Looks compliant.'",
        "'report-001',  ''",
        "'report-001',  '   '"
    })
    void createComment_withBlankRequiredFields_returnsBadRequest(
            String auditReportId, String comment) throws Exception {
        mockMvc.perform(post("/api/service/auditcomment")
                        .with(user("auditor").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditReportId\":\"" + auditReportId + "\",\"comment\":\"" + comment + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
