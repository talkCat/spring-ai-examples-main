package com.example.openai.octopathsp.configuration;

import com.example.openai.octopathsp.utils.HybridDocumentRetriever;
import com.example.openai.octopathsp.utils.HybridSearchService;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;

/**
 * @author n039920
 * @date 2025/12/9
 * @description TODO
 */
//@Configuration 在 ChatConfig 中完成实例创建
public class HybridSearchConfig {

    @Bean
    public DocumentRetriever hybridDocumentRetriever(
            HybridSearchService hybridSearchService,
            VectorStore vectorStore) {

        return new HybridDocumentRetriever(
                hybridSearchService,
                vectorStore
        );
    }
}
