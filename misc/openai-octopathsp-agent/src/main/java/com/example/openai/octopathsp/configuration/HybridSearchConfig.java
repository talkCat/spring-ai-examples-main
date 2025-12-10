package com.example.openai.octopathsp.configuration;

import com.example.openai.octopathsp.utils.HybridDocumentRetriever;
import com.example.openai.octopathsp.utils.HybridSearchService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author n039920
 * @date 2025/12/9
 * @description TODO
 */
@Configuration
public class HybridSearchConfig {

    @Bean
    @Primary  // 将这个 DocumentRetriever 设为首选
    public DocumentRetriever hybridDocumentRetriever(
            HybridSearchService hybridSearchService,
            VectorStore vectorStore,
            EmbeddingModel embeddingModel) {

        return new HybridDocumentRetriever(
                hybridSearchService,
                vectorStore,
                embeddingModel
        );
    }
}
