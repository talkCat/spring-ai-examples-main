package com.example.openai.octopathsp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * @author n039920
 * @date 2025/12/8
 * @description TODO
 */
public class WeatherTool {

    @Tool(description = "获取天气信息")
    String getWeather(@ToolParam(description = "用户的地址信息") String city,
                      @ToolParam(description = "当前时间") String time) {
        return city + ", " + time + ",晴，40度";
    }
}
