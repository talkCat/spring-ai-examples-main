package com.example.openai.streaming.embedding;

import com.example.openai.streaming.service.SampleMCPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author n039920
 * @date 2025/11/19
 * @description TODO
 */
@RestController
public class McpChatController {

    @Autowired
    private SampleMCPService sampleMCPService;

    @GetMapping("/ai/mcp/getPersonInfo")
    public String getPersonInfo(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return sampleMCPService.getPersonInfo();
    }
}
