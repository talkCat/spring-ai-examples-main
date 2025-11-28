package com.example.openai.streaming.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;

/**
 * @author n039920
 * @date 2025/11/6
 * @description TODO
 */
public class TimeTool {
    @Tool(description = "获取当前时间")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    @Tool(description = "获取天气信息")
    String getWeather(@ToolParam(description = "用户的地址信息") String city,
                      @ToolParam(description = "当前时间") String time) {
        return city + ", " + time + ",晴，40度";
    }
}
