package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.security.TestSecurityConfig;
import ch.zhaw.trueyield.security.UserService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PortfolioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static String createdId;
    private static final String FUND_MANAGER_ID = "integration-test-manager";
    private static final String PORTFOLIO_NAME = "Integration Test Portfolio";
    private static final String PORTFOLIO_DESC = "Created by integration test";

    // ── Test 1: POST → create portfolio ──────────────────────────────────────

    @Test
    @Order(1)
    void createPortfolio_returnsCreated_andSavesId() throws Exception {
        when(userService.getCurrentUserId()).thenReturn(FUND_MANAGER_ID);

        String requestBody = """
                {
                    "name": "%s",
                    "description": "%s"
                }
                """.formatted(PORTFOLIO_NAME, PORTFOLIO_DESC);

        MvcResult result = mockMvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(PORTFOLIO_NAME))
                .andExpect(jsonPath("$.fundManagerId").value(FUND_MANAGER_ID))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"id\":\"([^\"]+)\"").matcher(responseBody);
        if (matcher.find()) {
            createdId = matcher.group(1);
        }
    }

    // ── Test 2: GET /{id} → verify portfolio exists ───────────────────────────

    @Test
    @Order(2)
    void getPortfolioById_returnsOk_andCorrectFields() throws Exception {
        when(userService.getCurrentUserId()).thenReturn(FUND_MANAGER_ID);

        mockMvc.perform(get("/api/portfolio/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(PORTFOLIO_NAME))
                .andExpect(jsonPath("$.description").value(PORTFOLIO_DESC))
                .andExpect(jsonPath("$.fundManagerId").value(FUND_MANAGER_ID));
    }

    // ── Test 3: DELETE /{id} → delete portfolio ───────────────────────────────

    @Test
    @Order(3)
    void deletePortfolio_returnsNoContent() throws Exception {
        when(userService.getCurrentUserId()).thenReturn(FUND_MANAGER_ID);

        mockMvc.perform(delete("/api/portfolio/" + createdId))
                .andExpect(status().isNoContent());
    }

    // ── Test 4: GET /{id} → verify portfolio is gone ─────────────────────────

    @Test
    @Order(4)
    void getPortfolioById_afterDelete_returnsNotFound() throws Exception {
        when(userService.getCurrentUserId()).thenReturn(FUND_MANAGER_ID);

        mockMvc.perform(get("/api/portfolio/" + createdId))
                .andExpect(status().isNotFound());
    }
}
