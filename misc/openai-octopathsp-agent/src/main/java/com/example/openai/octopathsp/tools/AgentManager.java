package com.example.openai.octopathsp.tools;

import com.example.openai.octopathsp.agent.AccessoryAgent;
import com.example.openai.octopathsp.agent.CharacterAgent;
import com.example.openai.octopathsp.bean.AgentRoute;
import com.example.openai.octopathsp.bean.StreamResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author n039920
 * @date 2025/12/4
 * @description TODO
 */
@Slf4j
@Component
public class AgentManager {

    @Autowired
    AgentRouter agentRouter;

    @Autowired
    CharacterAgent characterAgent;

    @Autowired
    AccessoryAgent accessoryAgent;

    /**
     * 处理用户问题
     */
    public Flux<StreamResponse> processQuery(String userQuery) {
        // 1. 路由到合适的Agent
        AgentRoute route = agentRouter.route(userQuery);

        log.info("路由决策: {} -> {} (置信度: {})",
                userQuery, route.getAgentType(), route.getConfidence());

        // 2. 执行对应的Agent
        return executeAgent(route);
    }

    private Flux<StreamResponse> executeAgent(AgentRoute route) {
        return switch (route.getAgentType()) {
            case CHARACTER_SKILL -> characterAgent.getCharacterInfo(route.getOriginalQuery());
            case ACCESSORY -> accessoryAgent.getAccessoryInfo(route.getOriginalQuery());
            case PET -> null;
            case GENERAL -> null;
            case UNKNOWN -> Flux.just(new StreamResponse("抱歉，我暂时无法处理这个问题。请尝试问关于角色技能、饰品装备或支炎兽的问题。"));
        };
    }

    /**
     * 获取路由统计信息
     */
    public Map<String, Object> getRoutingStats() {
        // 可以添加统计逻辑
        return Map.of(
                "totalAgents", 4,
                "routingMethod", "关键词+AI混合路由"
        );
    }
}
