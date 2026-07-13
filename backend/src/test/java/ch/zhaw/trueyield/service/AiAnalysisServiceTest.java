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

    // ── analyzeRelevance ─────────────────────────────────────────────────────

    @Test
    void analyzeRelevance_returnsOne_whenNotAvailable() {
        AiAnalysisService service = new AiAnalysisService();
        ReflectionTestUtils.setField(service, "chatModel", null);
        ReflectionTestUtils.setField(service, "apiKey", "");

        double result = service.analyzeRelevance("Apple", "Some article text");

        assertEquals(1.0, result);
        verifyNoInteractions(chatModel);
    }

    @ParameterizedTest
    @CsvSource({
        "0.0,  0.0",
        "0.2,  0.2",
        "0.5,  0.5",
        "1.0,  1.0"
    })
    void analyzeRelevance_returnsCorrectScore_forValidValues(String aiOutput, double expected) {
        ChatResponse mockResponse = mockChatResponse(aiOutput.trim());
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeRelevance("Nestlé", "ESG article about Nestlé");

        assertEquals(expected, result, 0.001);
    }

    @Test
    void analyzeRelevance_clampsToOne_whenAiReturnsHigherValue() {
        ChatResponse mockResponse = mockChatResponse("1.5");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeRelevance("Apple", "Some text");

        assertEquals(1.0, result);
    }

    @Test
    void analyzeRelevance_clampsToZero_whenAiReturnsNegativeValue() {
        ChatResponse mockResponse = mockChatResponse("-0.3");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeRelevance("Apple", "Some text");

        assertEquals(0.0, result);
    }

    @Test
    void analyzeRelevance_returnsOne_whenResponseIsNotParseable() {
        ChatResponse mockResponse = mockChatResponse("not a number");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeRelevance("Apple", "Some text");

        assertEquals(1.0, result);
    }

    @Test
    void analyzeRelevance_returnsOne_whenChatModelThrows() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("timeout"));

        double result = aiAnalysisService.analyzeRelevance("Apple", "Some text");

        assertEquals(1.0, result);
    }

    @Test
    void analyzeRelevance_handlesCommaAsDecimalSeparator() {
        ChatResponse mockResponse = mockChatResponse("0,8");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.analyzeRelevance("Apple", "Some text");

        assertEquals(0.8, result, 0.001);
    }

    // ── generatePortfolioRiskScore ───────────────────────────────────────────

    @Test
    void generatePortfolioRiskScore_returnsValidScore_whenModelResponds() {
        ChatResponse mockResponse = mockChatResponse("7\nHigh carbon footprint across fossil fuel holdings.");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        AiAnalysisService.PortfolioRiskResult result = aiAnalysisService.generatePortfolioRiskScore(
                List.of("Shell PLC", "Exxon Mobil"),
                List.of("Shell faces regulatory scrutiny over emissions.", "Exxon lobbied against climate policy."));

        assertEquals(7, result.score());
        assertNotNull(result.rationale());
        assertFalse(result.rationale().isBlank());
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void generatePortfolioRiskScore_fallbackOnError_whenModelThrows() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));

        AiAnalysisService.PortfolioRiskResult result = aiAnalysisService.generatePortfolioRiskScore(
                List.of("Shell PLC"), List.of("Some snippet"));

        assertNull(result.score());
        assertEquals("AI analysis unavailable.", result.rationale());
    }

    @Test
    void generatePortfolioRiskScore_withEmptyLists_doesNotThrow() {
        ChatResponse mockResponse = mockChatResponse("3\nNo significant ESG risks identified.");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        AiAnalysisService.PortfolioRiskResult result = aiAnalysisService.generatePortfolioRiskScore(
                List.of(), List.of());

        assertNotNull(result);
        assertTrue(result.score() >= 0 && result.score() <= 10);
    }

    @Test
    void generatePortfolioRiskScore_returnsUnavailable_whenNotAvailable() {
        AiAnalysisService service = new AiAnalysisService();
        ReflectionTestUtils.setField(service, "chatModel", null);
        ReflectionTestUtils.setField(service, "apiKey", "");

        AiAnalysisService.PortfolioRiskResult result = service.generatePortfolioRiskScore(
                List.of("Apple Inc."), List.of("Snippet"));

        assertNull(result.score());
        assertEquals("AI analysis unavailable.", result.rationale());
        verifyNoInteractions(chatModel);
    }

    @Test
    void generatePortfolioRiskScore_clampsScore_whenModelReturnsOutOfRange() {
        ChatResponse mockResponse = mockChatResponse("15\nExtremely high risk.");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        AiAnalysisService.PortfolioRiskResult result = aiAnalysisService.generatePortfolioRiskScore(
                List.of("BadCorp"), List.of());

        assertEquals(10, result.score());
    }

    // ── isPremiumSource ──────────────────────────────────────────────────────

    @Test
    void isPremiumSource_returnsTrue_whenModelAnswersYes() {
        ChatResponse mockResponse = mockChatResponse("yes");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        assertTrue(aiAnalysisService.isPremiumSource("Reuters"));
    }

    @Test
    void isPremiumSource_returnsFalse_whenModelAnswersNo() {
        ChatResponse mockResponse = mockChatResponse("no");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        assertFalse(aiAnalysisService.isPremiumSource("SomeBlog"));
    }

    @Test
    void isPremiumSource_returnsFalse_whenNotAvailable() {
        AiAnalysisService service = new AiAnalysisService();
        ReflectionTestUtils.setField(service, "chatModel", null);
        ReflectionTestUtils.setField(service, "apiKey", "");

        assertFalse(service.isPremiumSource("Reuters"));
        verifyNoInteractions(chatModel);
    }

    @Test
    void isPremiumSource_returnsFalse_whenSourceIsNull() {
        assertFalse(aiAnalysisService.isPremiumSource(null));
    }

    @Test
    void isPremiumSource_returnsFalse_whenSourceIsBlank() {
        assertFalse(aiAnalysisService.isPremiumSource("   "));
    }

    @Test
    void isPremiumSource_returnsFalse_whenChatModelThrows() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("AI error"));

        assertFalse(aiAnalysisService.isPremiumSource("Bloomberg"));
    }

    // ── generatePortfolioSentiment ───────────────────────────────────────────

    @Test
    void generatePortfolioSentiment_returnsValue_whenModelResponds() {
        ChatResponse mockResponse = mockChatResponse("-0.6");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.generatePortfolioSentiment(List.of("Shell PLC", "Exxon Mobil"));

        assertEquals(-0.6, result, 0.001);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void generatePortfolioSentiment_returnsZero_whenResponseIsNotParseable() {
        ChatResponse mockResponse = mockChatResponse("nicht eine Zahl");
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        double result = aiAnalysisService.generatePortfolioSentiment(List.of("Apple Inc."));

        assertEquals(0.0, result);
    }

    @Test
    void generatePortfolioSentiment_returnsZero_whenNotAvailable() {
        AiAnalysisService service = new AiAnalysisService();
        ReflectionTestUtils.setField(service, "chatModel", null);
        ReflectionTestUtils.setField(service, "apiKey", "");

        double result = service.generatePortfolioSentiment(List.of("Apple Inc."));

        assertEquals(0.0, result);
        verifyNoInteractions(chatModel);
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
