package ch.zhaw.trueyield.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.zhaw.trueyield.repository.PortfolioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @AfterEach
    void cleanup() {
        portfolioRepository.deleteAll();
    }

    @Test
    void testCreatePortfolioReturnsCreated() throws Exception {
        String requestBody = """
                {
                    "name": "ESG Global Fund",
                    "description": "A diversified ESG portfolio"
                }
                """;

        mockMvc.perform(post("/api/portfolio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("ESG Global Fund"))
                .andExpect(jsonPath("$.description").value("A diversified ESG portfolio"))
                .andExpect(jsonPath("$.fundManagerId").value("anonymous"));
    }

    @Test
    void testCreatePortfolioWithBlankNameReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "name": "",
                    "description": "Missing name"
                }
                """;

        mockMvc.perform(post("/api/portfolio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreatePortfolioWithoutNameReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "description": "No name field at all"
                }
                """;

        mockMvc.perform(post("/api/portfolio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllPortfoliosReturnsOkWithList() throws Exception {
        // Arrange – create a portfolio first
        String requestBody = """
                {
                    "name": "ESG Europe Fund",
                    "description": "European ESG assets"
                }
                """;

        mockMvc.perform(post("/api/portfolio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated());

        // Act & Assert – GET should return the portfolio
        mockMvc.perform(get("/api/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("ESG Europe Fund"))
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void testGetAllPortfoliosReturnsEmptyArrayWhenNoneExist() throws Exception {
        mockMvc.perform(get("/api/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

