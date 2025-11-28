package com.example.openai.streaming.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ChatConfig {

    /*@Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }*/

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public JdbcChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        //检索增强
        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .queryAugmenter(QueryAugmenterFactory.getContextBasedQueryAugmenter())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.7)
                        .topK(5)
                        .build())
                .build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        advisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    //测试 mcp
    @Bean
    public ChatClient chatClient02(ChatModel chatModel, ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(tools)
                .build();
    }
}
