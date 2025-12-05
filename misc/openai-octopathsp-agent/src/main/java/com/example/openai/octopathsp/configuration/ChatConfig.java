package com.example.openai.octopathsp.configuration;

import com.example.openai.octopathsp.prompt.PromptTemplateCustomize;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ChatConfig {

    @Bean
    public JdbcChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .build();
    }

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                /*.defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )*/
                .defaultOptions(ChatOptions.builder().temperature(0.8).build())
                .build();
    }

    //角色信息
    @Bean
    public ChatClient chatCharacterClient(ChatModel chatModel, ChatMemory chatMemory, VectorStore indexCharacterVectorStore) {
        //检索增强
        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .queryAugmenter(QueryAugmenterFactory.getContextBasedQueryAugmenter())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(indexCharacterVectorStore)
                        .similarityThreshold(0.4)
                        .topK(100)
                        .build())
                .build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        advisor
                        //MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultSystem(PromptTemplateCustomize.SYSTEM_PROMPT)
                // 低温度确保分类稳定
                .defaultOptions(ChatOptions.builder().temperature(0.4).build())
                .build();
    }

}
