package com.example.openai.octopathsp.controller;


import com.example.openai.octopathsp.agent.AccessoryAgent;
import com.example.openai.octopathsp.agent.CharacterAgent;
import com.example.openai.octopathsp.bean.StreamResponse;
import com.example.openai.octopathsp.tools.TimeTool;
import com.example.openai.octopathsp.tools.WeatherTool;
import com.example.openai.octopathsp.utils.AgentManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    OpenAiChatModel chatModel;

    @Autowired
    VectorStore vectorStore;

    @Autowired
    ChatClient chatClient;

    @Autowired
    ChatMemory chatMemory;

    @Autowired
    CharacterAgent characterAgent;

    @Autowired
    AccessoryAgent accessoryAgent;

    @Autowired
    AgentManager agentManager;

    @Autowired
    ChatClient chatStructuredOutputClientAdvisor;

    // chat样例
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        ToolCallback[] customerTools = ToolCallbacks.from(new TimeTool(), new WeatherTool());
        ChatClient chatClient = ChatClient
                .builder(chatModel)
                .defaultOptions(ToolCallingChatOptions.builder()
                        .toolCallbacks(customerTools)
                        .build()).build();
        return chatClient.prompt().user("查询南京当前的时间和天气").stream().content();
    }

    private String buildWeatherPrompt(String city) {
        return String.format("""
            请提供%s的详细天气信息，包括：
            1. 当前天气：温度、体感温度、天气状况、湿度、风速、风向、气压、能见度
            
            请以JSON格式返回，严格遵守Schema定义。
            """, city);
    }


    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamResponse> generateStream(
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        //return accessoryAgent.getAccessoryInfo(message);
        //return characterAgent.getCharacterInfo(message);
        return agentManager.processQuery(message);
    }

    @GetMapping(value = "/chartMemory", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chartMemory(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message, @RequestParam(value = "conversationId", defaultValue = "default") String conversationId) {
        Flux<String> response = chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId))
                .stream()
                .content();

        return response;
    }


    private StreamResponse createCompletionSignal(String msgId) {
        StreamResponse response = new StreamResponse();
        response.setContent("");
        response.setCompleted(true);
        response.setMessageId(msgId);
        return response;
    }

    private StreamResponse createErrorResponse(String msgId) {
        StreamResponse response = new StreamResponse();
        response.setContent("抱歉，AI服务暂时不可用");
        response.setCompleted(true);
        response.setMessageId(msgId);
        return response;
    }
}
