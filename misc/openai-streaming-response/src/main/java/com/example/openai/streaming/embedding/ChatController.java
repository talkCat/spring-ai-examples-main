package com.example.openai.streaming.embedding;

import com.example.openai.streaming.bean.StreamResponse;
import com.example.openai.streaming.configuration.QueryAugmenterFactory;
import com.example.openai.streaming.prompt.PromptTemplateCustomize;
import com.example.openai.streaming.service.SimpleChatService;
import com.example.openai.streaming.tools.TimeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/ai")
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
    SimpleChatService simpleChatService;

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamResponse> generateStream(
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        logger.info("Received message: " + message);
        logger.debug("Generating response...");
        AtomicReference<String> messageId = new AtomicReference<>(UUID.randomUUID().toString());
        return chatModel.stream(new Prompt(new UserMessage(message)))
                .map(chatResponse -> {
                    StreamResponse response = new StreamResponse();
                    response.setContent(chatResponse.getResult().getOutput().getText());
                    response.setCompleted(false);
                    response.setMessageId(messageId.get());
                    return response;
                })
                .concatWithValues(createCompletionSignal(messageId.get()))
                .onErrorReturn(createErrorResponse(messageId.get()));
    }

    @GetMapping(value = "/chartRag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamResponse> chartRag(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message, @RequestParam(value = "conversationId", defaultValue = "default") String conversationId) {
        /*RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .queryAugmenter(QueryAugmenterFactory.getContextBasedQueryAugmenter())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.7)
                        .topK(5)
                        .build())
                .build();*/
        //ChatClient chatClient = ChatClient.builder(chatModel).build();
        Flux<ChatResponse> responseSpec = chatClient.prompt()
                //.advisors(advisor)
                .advisors(advisorMemory -> advisorMemory.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(message)
                .system(PromptTemplateCustomize.SYSTEM_PROMPT)
                .stream()
                .chatResponse();

        AtomicReference<String> messageId = new AtomicReference<>(UUID.randomUUID().toString());
        return responseSpec.map(chatResponse -> {
                    StreamResponse response = new StreamResponse();
                    response.setContent(chatResponse.getResult().getOutput().getText());
                    response.setCompleted(false);
                    response.setMessageId(messageId.get());
                    return response;
                })
                .concatWithValues(createCompletionSignal(messageId.get()))
                .onErrorReturn(createErrorResponse(messageId.get()));
    }

    @GetMapping(value = "/chartTools", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chartTools(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        return chatClient.prompt().tools(new TimeTool()).user(message).stream().content();
    }

    @GetMapping(value = "/chartLogs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chartLogs(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "Custom request: " + request.prompt().getUserMessage(),
                response -> "Custom response: " + response.getResult().getOutput().getText(),
                1
        );
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        return chatClient.prompt().advisors(customLogger).user(message).stream().content();
    }

    @GetMapping(value = "/chartMemory", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chartMemory(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message, @RequestParam(value = "conversationId", defaultValue = "default") String conversationId) {
        Flux<String> response = chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId))
                .stream()
                .content();

        // 收集所有响应片段并保存记忆
        response.collectList()
                .map(chunks -> String.join("", chunks))
                .doOnSuccess(fullText -> {
                    chatMemory.add(conversationId,new AssistantMessage(fullText));
                });

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
