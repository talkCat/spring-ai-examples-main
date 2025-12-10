package com.example.openai.octopathsp.controller;


import com.example.openai.octopathsp.agent.AccessoryAgent;
import com.example.openai.octopathsp.agent.CharacterAgent;
import com.example.openai.octopathsp.bean.StreamResponse;
import com.example.openai.octopathsp.utils.AgentManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
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
