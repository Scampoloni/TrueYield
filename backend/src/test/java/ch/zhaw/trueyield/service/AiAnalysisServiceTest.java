package ch.zhaw.trueyield.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private ChatModel chatModel;

    private AiAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        aiAnalysisService = new AiAnalysisService();
        ReflectionTestUtils.setField(aiAnalysisService, "chatModel", chatModel);
        ReflectionTestUtils.setField(aiAnalysisService, "apiKey", "test-api-key");
    }

    // ── isAvailable ──────────────────────────────────────────────────────────

    @Test
    void isAvailable_returnsTrue_whenChatModelAndApiKeyPresent() {
        assertTrue(aiAnalysisService.isAvailable());
    }

    @Test
    void isAvailable_returnsFalse_whenChatModelIsNull() {
        AiAnalysisService service = new AiAnalysisService();
        ReflectionTestUtils.setField(service, "chatModel", null);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        assertFalse(service.isAvailable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void isAvailable_returnsFalse_whenApiKeyIsBlankOrEmpty(String blankKey) {
        AiAnalysisService service = new AiAnalysisService();
        ReflectionTestUtils.setField(service, "chatModel", chatModel);
        ReflectionTestUtils.setField(service, "apiKey", blankKey);
        assertFalse(service.isAvailable());
    }

    // ── generateRiskSummary ──────────────────────────────────────────────────

    @Test
    void generateRiskSummary_returnsUnavailable_whenNotAvailable() {
        AiAnalysisService service = new AiAnalysisService();
        ReflectionTestUtils.setField(service, "chatModel", null);
        ReflectionTestUtils.setField(service, "apiKey", "");

        String result = service.generateRiskSummary(List.of("Apple Inc.", "Tesla"));

        assertEquals("AI analysis unavailable.", result);
        verifyNoInteractions(chatModel);
    }

    @Test
    void generateRiskSummary_callsChatModelAndReturnsSummary() {
        String expected = "High ESG risk detected for Tesla due to labor violations.";
        ChatResponse mockResponse = mockChatResponse(expected);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String result = aiAnalysisService.generateRiskSummary(List.of("Tesla", "Apple Inc."));

        assertEquals(expected, result);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void generateRiskSummary_handlesEmptyHoldingsList() {
        String expected = "No significant ESG risks identified for this portfolio.";
        ChatResponse mockResponse = mockChatResponse(expected);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String result = aiAnalysisService.generateRiskSummary(List.of());

        assertNotNull(result);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void generateRiskSummary_returnsUnavailable_whenChatModelThrows() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));

        String result = aiAnalysisService.generateRiskSummary(List.of("Apple Inc."));

        assertEquals("AI analysis unavailable.", result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Apple Inc.", "Tesla", "Microsoft", "Nestlé AG", "Shell PLC"})
    void generateRiskSummary_invokesModelForAnyHolding(String company) {
        ChatResponse mockResponse = mockChatResponse("Risk summary.");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        String result = aiAnalysisService.generateRiskSummary(List.of(company));

        assertNotNull(result);
        verify(chatModel, atLeastOnce()).call(any(Prompt.class));
    }

    // ── analyzeSentiment ─────────────────────────────────────────────────────

    @Test
    void analyzeSentiment_returnsZero_whenNotAvailable() {
        AiAnalysisService service = new AiAnalysisService();
        ReflectionTestUtils.setField(service, "chatModel", null);
        ReflectionTestUtils.setField(service, "apiKey", "");

        assertEquals(0.0, service.analyzeSentiment("Bad ESG news"));
        verifyNoInteractions(chatModel);
    }

    @ParameterizedTest
    @CsvSource({
        "-1.0, -1.0",
        "-0.9, -0.9",
        " 0.0,  0.0",
        " 0.75, 0.75",
        " 1.0,  1.0"
    })
    void analyzeSentiment_returnsCorrectScore_forValidDecimalValues(String aiOutput, double expected) {
        ChatResponse mockResponse = mockChatResponse(aiOutput.trim());
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeSentiment("Some ESG content");

        assertEquals(expected, result, 0.001);
    }

    @Test
    void analyzeSentiment_clampsToMaximumOne_whenAiReturnsHigherValue() {
        ChatResponse mockResponse = mockChatResponse("1.5");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeSentiment("Extremely positive news");

        assertEquals(1.0, result);
    }

    @Test
    void analyzeSentiment_clampsToMinimumMinusOne_whenAiReturnsLowerValue() {
        ChatResponse mockResponse = mockChatResponse("-1.8");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeSentiment("Extremely negative news");

        assertEquals(-1.0, result);
    }

    @Test
    void analyzeSentiment_handlesCommaAsDecimalSeparator() {
        ChatResponse mockResponse = mockChatResponse("0,75");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeSentiment("Some text");

        assertEquals(0.75, result, 0.001);
    }

    @Test
    void analyzeSentiment_returnsZero_whenResponseIsNotParseable() {
        ChatResponse mockResponse = mockChatResponse("not a number");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeSentiment("Some text");

        assertEquals(0.0, result);
    }

    @Test
    void analyzeSentiment_returnsZero_whenChatModelThrows() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Network error"));

        double result = aiAnalysisService.analyzeSentiment("Some text");

        assertEquals(0.0, result);
    }

    @Test
    void analyzeSentiment_trimsWhitespaceFromResponse() {
        ChatResponse mockResponse = mockChatResponse("  0.5  ");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeSentiment("Some text");

        assertEquals(0.5, result, 0.001);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private ChatResponse mockChatResponse(String text) {
        // AssistantMessage.getText() is final in Spring AI — use a real instance
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(message);
        ChatResponse response = mock(ChatResponse.class);
        when(response.getResult()).thenReturn(generation);
        return response;
    }
}
