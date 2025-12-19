package com.example.openai.octopathsp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author n039920
 * @date 2025/11/6
 * @description TODO
 */
public class TimeTool {
    @Tool(description = "获取当前时间")
    String getCurrentDateTime() {
        return LocalDateTime.now( ZoneId.of("America/Phoenix")).toString();
    }

}
