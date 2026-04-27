package ch.zhaw.trueyield.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private EsgChatTools esgChatTools;

    // ── chatModel is null ─────────────────────────────────────────────────────

    @Test
    void chat_withNullChatModel_returnsUnavailableMessage() {
        ChatService service = new ChatService(null, esgChatTools);
        String result = service.chat("Hello");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    // ── chatModel throws during call ──────────────────────────────────────────

    @Test
    void chat_whenChatModelThrows_returnsUnavailableMessage() {
        ChatModel chatModel = mock(ChatModel.class, RETURNS_DEEP_STUBS);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API error"));

        ChatService service = new ChatService(chatModel, esgChatTools);
        String result = service.chat("Hello");
        assertNotNull(result);
        assertFalse(result.isBlank());
        // must return the unavailable message, not throw
        assertTrue(result.contains("unavailable") || result.contains("API"));
    }

    // ── chatModel returns response with null content ───────────────────────────

    @Test
    void chat_whenChatModelReturnsNullContent_returnsNoResponseAvailable() {
        ChatModel chatModel = mock(ChatModel.class, RETURNS_DEEP_STUBS);
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn(null);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        ChatService service = new ChatService(chatModel, esgChatTools);
        String result = service.chat("Hello");
        assertEquals("No response available.", result);
    }

    // ── chatModel returns blank content ───────────────────────────────────────

    @Test
    void chat_whenChatModelReturnsBlankContent_returnsNoResponseAvailable() {
        ChatModel chatModel = mock(ChatModel.class, RETURNS_DEEP_STUBS);
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("   ");
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        ChatService service = new ChatService(chatModel, esgChatTools);
        String result = service.chat("Tell me about ESG");
        assertEquals("No response available.", result);
    }

    // ── chatModel returns valid response ──────────────────────────────────────

    @Test
    void chat_whenChatModelReturnsValidContent_returnsThatContent() {
        ChatModel chatModel = mock(ChatModel.class, RETURNS_DEEP_STUBS);
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn("Here is your ESG analysis.");
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        ChatService service = new ChatService(chatModel, esgChatTools);
        String result = service.chat("Analyze my portfolio");
        assertEquals("Here is your ESG analysis.", result);
    }
}
