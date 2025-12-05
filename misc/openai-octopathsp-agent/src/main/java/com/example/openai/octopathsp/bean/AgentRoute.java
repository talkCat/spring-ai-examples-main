package com.example.openai.octopathsp.bean;

import com.example.openai.octopathsp.enums.AgentType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author n039920
 * @date 2025/12/4
 * @description TODO
 */
@Data
@AllArgsConstructor
public class AgentRoute {
    private AgentType agentType;
    private String originalQuery;
    private double confidence;
    private String reason;

    public AgentRoute(AgentType agentType, String originalQuery) {
        this.agentType = agentType;
        this.originalQuery = originalQuery;
        this.confidence = 0.8;
        this.reason = "基于关键词匹配";
    }
}
