package com.example.openai.octopathsp.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * @author n039920
 * @date 2025/12/18
 * @description TODO
 */
public class StockTools {

    // 获取实时股票数据工具
    @Tool(description = "获取实时股票数据")
    public String getStockData(String stockCode) {
        // 写死返回固定股票数据
        return "股票代码：SH600519，当前价格：1800.50";
    }

    // 获取新闻分析工具
    @Tool(description = "获取新闻分析")
    public String getNewsAnalysis(String news) {
        // 写死返回固定新闻分析结果
        return "新闻：央行宣布降准0.5个百分点，分析结果: 利好股市，流动性增强，预计短期市场情绪提振。";
    }

    // 获取经济指标工具
    @Tool(description = "获取经济指标")
    public String getEconomicIndicators(String indicator) {
        // 写死返回固定经济指标数据
        return "经济指标：CPI（居民消费价格指数），当前值：2.3%";
    }
}
