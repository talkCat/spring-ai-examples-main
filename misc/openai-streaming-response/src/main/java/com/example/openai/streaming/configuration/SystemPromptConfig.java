package com.example.openai.streaming.configuration;

import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @author n039920
 * @date 2025/11/12
 * @description TODO
 */
//@Configuration
public class SystemPromptConfig {

    @Value("classpath:/prompts/qa-prompt.st.st")
    private Resource systemResource;

    @Bean
    public SystemPromptTemplate systemPromptTemplate() {
        return new SystemPromptTemplate(systemResource);
    }
}
