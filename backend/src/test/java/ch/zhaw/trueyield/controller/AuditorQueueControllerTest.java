package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.dto.AuditReportResponseDTO;
import ch.zhaw.trueyield.model.enums.AuditStatus;
import ch.zhaw.trueyield.security.UserService;
import ch.zhaw.trueyield.service.AuditReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditorQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditReportService auditReportService;

    @MockitoBean
    private UserService userService;

    @Test
    void auditor_canSeeQueue_returns200() throws Exception {
        when(userService.getCurrentUserId()).thenReturn("auditor-001");
        AuditReportResponseDTO dto = new AuditReportResponseDTO(
                "report-001", "portfolio-001", "Global ESG Leaders Fund", AuditStatus.PENDING_REVIEW, null, null, null);
        when(auditReportService.getAuditorQueue(anyString())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/service/auditreport/auditor-queue")
                        .with(user("auditor").roles("auditor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].auditStatus").value("PENDING_REVIEW"));
    }

    @Test
    void fundManager_cannotAccessAuditorQueue_returns403() throws Exception {
        mockMvc.perform(get("/api/service/auditreport/auditor-queue")
                        .with(user("manager").roles("fund-manager")))
                .andExpect(status().isForbidden());
    }
}
