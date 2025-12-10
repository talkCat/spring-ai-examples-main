package com.example.openai.octopathsp.agent;

import com.example.openai.octopathsp.bean.StreamResponse;
import com.example.openai.octopathsp.prompt.PromptTemplateCustomize;
import com.example.openai.octopathsp.utils.JsonReader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author n039920
 * @date 2025/12/4
 * @description TODO
 */
@Component
public class AccessoryAgent {

    @Autowired
    private ChatClient chatAccessoryClient;

    public Flux<StreamResponse> getAccessoryInfo(String query){
        /*AtomicReference<String> messageId = new AtomicReference<>(UUID.randomUUID().toString());
        //获取饰品信息
        String accessoryInfo = JsonReader.readJsonFileAsString("accessories.json");
        //获取提示词模板
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(PromptTemplateCustomize.CONTEXT_PROMPT_TEMPLATE_ACCESSORY_RAG)
                .build();
        String prompt = promptTemplate.render(Map.of("context", accessoryInfo, "query", query));
        return chatClient.prompt(prompt)
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
                .onErrorReturn(createErrorResponse(messageId.get()));*/
        AtomicReference<String> messageId = new AtomicReference<>(UUID.randomUUID().toString());
        return chatAccessoryClient.prompt()
                .user(query)
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
