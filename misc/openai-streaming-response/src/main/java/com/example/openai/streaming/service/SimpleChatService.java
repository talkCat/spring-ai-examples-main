package com.example.openai.streaming.service;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author n039920
 * @date 2025/11/11
 * @description TODO
 */
@Service
public class SimpleChatService {

    @Autowired
    ChatMemory chatMemory;

    //将对话添加到记忆
    @Async
    public void addMessageToMemory(Flux<String> response, String conversationId, String userMsg) {
        String assistantResponse = response.collectList().block().stream().collect(Collectors.joining());
        // 将对话添加到记忆
        UserMessage userMessage = new UserMessage(userMsg);
        SystemMessage systemMessage = new SystemMessage(assistantResponse);
        chatMemory.add(conversationId, userMessage);
        chatMemory.add(conversationId, systemMessage);
    }
}
