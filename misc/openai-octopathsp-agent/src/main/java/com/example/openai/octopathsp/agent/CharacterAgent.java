package com.example.openai.octopathsp.agent;

import com.example.openai.octopathsp.bean.StreamResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author n039920
 * @date 2025/11/28
 * @description TODO
 */
@Component
public class CharacterAgent {

    @Autowired
    ChatClient chatCharacterClient;

    public Flux<StreamResponse> getCharacterInfo(String detailType) {
        AtomicReference<String> messageId = new AtomicReference<>(UUID.randomUUID().toString());
        return chatCharacterClient.prompt()
                .user(detailType)
                .stream()
                .content()
                .map(chatResponse -> {
                    StreamResponse response = new StreamResponse();
                    response.setContent(chatResponse);
                    response.setCompleted(false);
                    response.setMessageId(messageId.get());
                    return response;
                })
                .concatWithValues(createCompletionSignal(messageId.get()))
                .onErrorReturn(createErrorResponse(messageId.get()));
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
