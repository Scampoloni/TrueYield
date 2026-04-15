package ch.zhaw.trueyield.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${spring.ai.anthropic.chat.options.model:claude-haiku-4-5-20251001}")
    private String model;

    @Value("${spring.ai.anthropic.chat.options.max-tokens:512}")
    private int maxTokens;

    @Bean
    @ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
    public AnthropicApi anthropicApi(
            @Value("${spring.ai.anthropic.api-key}") String apiKey) {
        return AnthropicApi.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
    public AnthropicChatModel anthropicChatModel(AnthropicApi anthropicApi) {
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(model)
                .maxTokens(maxTokens)
                .build();
        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
    public ChatClient.Builder chatClientBuilder(AnthropicChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }
}
