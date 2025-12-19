package com.example.openai.octopathsp.configuration;

import com.example.openai.octopathsp.tools.StockTools;
import com.example.openai.octopathsp.tools.TimeTool;
import com.example.openai.octopathsp.tools.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author n039920
 * @date 2025/12/18
 * @description TODO
 */
@Configuration
public class AIConfiguration {

    // 1. 定义工具回调
    @Bean
    public ToolCallingChatOptions toolCallingChatOptions() {
        return ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(new TimeTool(), new WeatherTool(), new StockTools()))
                .build();
    }

        /*典型的Advisor顺序
    -2000: SecurityAdvisor       // 最先：安全检查
    -1500: ValidationAdvisor     // 然后：输入验证
    -1000: RateLimitAdvisor      // 限流
    -500:  ContextAdvisor        // 添加上下文
     0:    ToolCallAdvisor       // ★ 工具调用（关键位置）★
     500:   ChatMemoryAdvisor    // 对话记忆
     1000:  RAGAdvisor           // 知识检索
     1500:  FormattingAdvisor    // 输出格式化
     2000:  LoggingAdvisor       // 最后：日志记录（最先看到响应）*/
    @Bean
    public ChatClient chatClientAdvisor(ChatModel chatModel, ToolCallingManager toolCallingManager,ToolCallingChatOptions toolCallingChatOptions, ChatMemory chatMemory) {
        var toolCallAdvisor = ToolCallAdvisor.builder()
                .toolCallingManager(toolCallingManager)
                .advisorOrder(BaseAdvisor.HIGHEST_PRECEDENCE + 300)
                .build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(toolCallAdvisor, MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultOptions(toolCallingChatOptions)
                .build();
    }
}
