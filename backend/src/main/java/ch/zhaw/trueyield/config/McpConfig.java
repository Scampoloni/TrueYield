package ch.zhaw.trueyield.config;

import ch.zhaw.trueyield.mcp.EsgMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider esgToolCallbackProvider(EsgMcpTools esgMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(esgMcpTools)
                .build();
    }
}
