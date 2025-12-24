package com.example.openai.octopathsp.configuration;

import com.example.openai.octopathsp.bean.WeatherResponse;
import com.example.openai.octopathsp.tools.CityWeatherTools;
import com.example.openai.octopathsp.tools.StockTools;
import com.example.openai.octopathsp.tools.TimeTool;
import com.example.openai.octopathsp.tools.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
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
 * @date 2025/12/22
 * @description TODO
 */
@Configuration
public class ValidationConfig {

    // 1. 定义工具回调
    /*@Bean
    public ToolCallingChatOptions toolCallingChatOptions() {
        return ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(new CityWeatherTools()))
                .build();
    }*/

    @Bean
    public StructuredOutputValidationAdvisor simpleWeatherValidationAdvisor() {
        // 直接使用 Class 类型（不需要 TypeRef）
        return StructuredOutputValidationAdvisor.builder()
                .outputType(WeatherResponse.class)
                .maxRepeatAttempts(2)
                .advisorOrder(1100)
                .build();
    }

    @Bean
    public ChatClient chatStructuredOutputClientAdvisor(ChatModel chatModel, ToolCallingManager toolCallingManager, ToolCallingChatOptions toolCallingChatOptions, StructuredOutputValidationAdvisor simpleWeatherValidationAdvisor) {
        var toolCallAdvisor = ToolCallAdvisor.builder()
                .toolCallingManager(toolCallingManager)
                .advisorOrder(BaseAdvisor.HIGHEST_PRECEDENCE + 300)
                .build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(toolCallAdvisor, simpleWeatherValidationAdvisor)
                .defaultOptions(toolCallingChatOptions)
                .build();
    }
}
