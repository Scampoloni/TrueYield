package ch.zhaw.trueyield.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String SYSTEM_PROMPT = """
            You are an ESG analyst assistant for TrueYield, a greenwashing audit platform.
            You help users analyse portfolios, holdings, and ESG evidence.
            You can read portfolio and holding data, and - if the user has the fund-manager role - create new portfolios and holdings.
            Always respond concisely and professionally. If a tool call fails, explain why clearly.
            Never invent data. If you don't know something, say so.
            """;

    private final ChatModel chatModel;
    private final EsgChatTools esgChatTools;

    public ChatService(@Autowired(required = false) ChatModel chatModel, EsgChatTools esgChatTools) {
        this.chatModel = chatModel;
        this.esgChatTools = esgChatTools;
    }

    public String chat(String userMessage) {
        if (chatModel == null) {
            return "Chat is currently unavailable. Please verify ANTHROPIC_API_KEY and try again.";
        }
        try {
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .tools(esgChatTools)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                return "No response available.";
            }
            return response;
        } catch (Exception e) {
            log.warn("Chat request failed: {}", e.getMessage());
            return "Chat is currently unavailable. Please verify ANTHROPIC_API_KEY and try again.";
        }
    }
}
