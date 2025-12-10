package com.example.openai.octopathsp.utils;

import com.example.openai.octopathsp.bean.AgentRoute;
import com.example.openai.octopathsp.enums.AgentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author n039920
 * @date 2025/12/4
 * @description TODO
 */
@Slf4j
@Component
public class AgentRouter {

    @Autowired
    private ChatClient chatClient;

    // 路由关键词配置
    private final Map<AgentType, List<String>> agentKeywords = Map.of(
            AgentType.CHARACTER_SKILL, List.of(
                    "技能", "天赋", "职业", "攻击", "防御", "法术", "战斗", "升级",
                    "转职", "加点", "属性", "战斗力", "职业选择", "技能搭配"
            ),
            AgentType.ACCESSORY, List.of(
                    "饰品", "装备", "武器", "防具", "戒指", "项链", "耳环", "宝石", "附魔",
                    "强化", "锻造", "合成", "装备属性", "套装", "装备升级"
            ),
            AgentType.PET, List.of(
                    "支炎兽", "宠物", "坐骑", "伙伴", "孵化", "喂养", "进化", "宠物技能",
                    "宠物升级", "宠物合成", "宠物装备", "宠物战斗", "捕捉"
            )
    );

    /**
     * 路由用户问题到合适的Agent
     */
    public AgentRoute route(String userQuery) {
        // 1. 首先尝试基于关键词的简单路由
        AgentType keywordType = routeByKeywords(userQuery);
        if (keywordType != AgentType.UNKNOWN) {
            log.info("关键词路由结果: {} -> {}", userQuery, keywordType);
            return new AgentRoute(keywordType, userQuery);
        }

        // 2. 如果关键词无法确定，使用AI路由
        return routeByAI(userQuery);
    }

    /**
     * 基于关键词的路由
     */
    private AgentType routeByKeywords(String query) {
        String lowerQuery = query.toLowerCase();

        // 统计关键词出现次数
        Map<AgentType, Integer> scoreMap = new HashMap<>();

        for (Map.Entry<AgentType, List<String>> entry : agentKeywords.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (lowerQuery.contains(keyword.toLowerCase())) {
                    score++;
                }
            }
            if (score > 0) {
                scoreMap.put(entry.getKey(), score);
            }
        }

        if (!scoreMap.isEmpty()) {
            // 返回得分最高的Agent类型
            return scoreMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(AgentType.UNKNOWN);
        }

        return AgentType.UNKNOWN;
    }

    /**
     * 基于AI的智能路由
     */
    private AgentRoute routeByAI(String query) {
        String prompt = String.format("""
            请分析以下用户问题属于哪个游戏助手模块：
            
            可用模块：
            1. 角色技能模块 - 处理角色技能、职业、天赋、战斗相关的问题
            2. 饰品装备模块 - 处理饰品、装备、武器、防具、强化相关的问题
            3. 支炎兽模块 - 处理宠物、坐骑、伙伴、喂养、进化相关的问题
            4. 通用模块 - 其他游戏相关问题
            
            用户问题: "%s"
            
            请严格按照以下JSON格式响应：
            {
                "agent": "模块名称（角色技能/饰品装备/支炎兽/通用）",
                "confidence": "置信度分数0-1",
                "reason": "路由原因"
            }
            """, query);

        String response = chatClient.prompt(prompt).call().content();

        try {
            return parseAIRouteResponse(response, query);
        } catch (Exception e) {
            log.error("AI路由解析失败: {}", response, e);
            return new AgentRoute(AgentType.GENERAL, query);
        }
    }

    private AgentRoute parseAIRouteResponse(String response, String originalQuery) {
        // 解析JSON响应
        // 这里可以使用Jackson或Gson，简化起见使用字符串处理
        if (response.contains("\"agent\": \"角色技能\"")) {
            return new AgentRoute(AgentType.CHARACTER_SKILL, originalQuery);
        } else if (response.contains("\"agent\": \"饰品装备\"")) {
            return new AgentRoute(AgentType.ACCESSORY, originalQuery);
        } else if (response.contains("\"agent\": \"支炎兽\"")) {
            return new AgentRoute(AgentType.PET, originalQuery);
        } else {
            return new AgentRoute(AgentType.GENERAL, originalQuery);
        }
    }
}
