package com.example.openai.streaming.service;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author n039920
 * @date 2025/11/19
 * @description TODO
 */
@Service
@Slf4j
public class SampleMCPService {

    @Autowired
    private ToolCallbackProvider tools;

    // 查询人员信息
    public String getPersonInfo() {

        // List and demonstrate tools
        /*McpSchema.ListToolsResult toolsList = mcpSyncClient.listTools();
        log.info("Available Tools = " + toolsList);
        // 查询人员信息
        McpSchema.CallToolResult personResult = mcpSyncClient.callTool(new McpSchema.CallToolRequest("getPersonInfo", Map.of("name", "张三")));
        log.info("Person Info: " + personResult);*/
        return null;
    }
}
