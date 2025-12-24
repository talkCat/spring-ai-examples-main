package com.example.openai.octopathsp.configuration;


import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;



/**
 * @author n039920
 * @date 2025/12/24
 * @description TODO
 */
@Configuration
public class MultiModelConfig {


    @Bean
    public ObservationRegistry observationRegistry() {
        // 创建一个简单的观察注册表
        return ObservationRegistry.create();
    }

    @Bean
    public OpenAiChatModel multiChatModel(
            ToolCallingManager toolCallingManager,
            RetryTemplate retryTemplate,
            ObservationRegistry observationRegistry,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey) {

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions opts = OpenAiChatOptions.builder()
                .model("qwenvl")
                .streamUsage(true)
                .build();

        return new OpenAiChatModel(api, opts, toolCallingManager, retryTemplate, observationRegistry);
    }

}
