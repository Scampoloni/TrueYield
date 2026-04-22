package ch.zhaw.trueyield.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void chatAsAuditor_returnsAnswer() throws Exception {
        when(chatService.chat("Summarize ESG risk")).thenReturn("Risk is moderate.");

        mockMvc.perform(post("/api/chat")
                        .with(user("auditor-1").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Summarize ESG risk"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Risk is moderate."));
    }

    @Test
    void chatWithBlankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .with(user("auditor-1").roles("auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatUnauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hello"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
