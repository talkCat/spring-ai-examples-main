package com.example.openai.octopathsp.utils;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Component;

/**
 * @author n039920
 * @date 2025/12/16
 * @description TODO
 */
@Component
public class RateLimitAdvisor implements Advisor {

    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
