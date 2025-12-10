package com.example.openai.octopathsp.utils;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HybridDocumentRetriever implements DocumentRetriever {

    private final HybridSearchService hybridSearchService;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public HybridDocumentRetriever(HybridSearchService hybridSearchService,
                                   VectorStore vectorStore,
                                   EmbeddingModel embeddingModel) {
        this.hybridSearchService = hybridSearchService;
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }


    /**
     * 扩展方法：带过滤条件的检索
     */
    public List<Document> retrieveWithFilters(String query, Map<String, Object> filters) {
        try {
            return hybridSearchService.hybridSearchWithKnnBool(query, filters, 10);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 扩展方法：带过滤条件和数量的检索
     */
    public List<Document> retrieveWithFilters(String query, Map<String, Object> filters, int topK) {
        try {
            return hybridSearchService.hybridSearchWithKnnBool(query, filters, topK);
        } catch (Exception e) {
            return null;
        }
    }


    @Override
    public List<Document> retrieve(Query query) {
        // 使用混合搜索，默认不添加过滤条件
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("metadata.description", query.text());
            return hybridSearchService.hybridSearchWithKnnBool(query.text(), filters, 20);
        } catch (Exception e) {
            // 如果混合搜索失败，回退到纯向量搜索
            return null;
        }
    }

    @Override
    public List<Document> apply(Query query) {
        return DocumentRetriever.super.apply(query);
    }
}
