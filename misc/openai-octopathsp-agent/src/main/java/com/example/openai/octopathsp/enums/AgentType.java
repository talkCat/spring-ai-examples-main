package com.example.openai.octopathsp.enums;

import lombok.Data;

/**
 * @author n039920
 * @date 2025/12/4
 * @description TODO
 */
public enum AgentType {

    CHARACTER_SKILL("角色技能", "处理角色技能、职业、天赋相关问题"),
    ACCESSORY("饰品装备", "处理饰品、装备、道具相关问题"),
    PET("支炎兽", "处理宠物、坐骑、伙伴相关问题"),
    GENERAL("通用助手", "处理通用游戏问题"),
    UNKNOWN("未知", "无法识别的问题类型");

    private final String name;
    private final String description;

    AgentType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
