package com.example.openai.octopathsp.utils;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HybridDocumentRetriever implements DocumentRetriever {

    private final HybridSearchService hybridSearchService;
    private final VectorStore vectorStore;

    public HybridDocumentRetriever(HybridSearchService hybridSearchService,
                                   VectorStore vectorStore) {
        this.hybridSearchService = hybridSearchService;
        this.vectorStore = vectorStore;
    }


    /**
     * 扩展方法：带过滤条件的检索
     */
    public List<Document> retrieveWithFilters(String query, Map<String, Object> filters) {
        try {
            return hybridSearchService.hybridSearchWithKnnBool(query, filters, 10, vectorStore);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 扩展方法：带过滤条件和数量的检索
     */
    public List<Document> retrieveWithFilters(String query, Map<String, Object> filters, int topK) {
        try {
            return hybridSearchService.hybridSearchWithKnnBool(query, filters, topK, vectorStore);
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
            return hybridSearchService.hybridSearchWithKnnBool(query.text(), filters, 20, vectorStore);
        } catch (Exception e) {
            // 如果混合搜索失败，回退到纯向量搜索
            return null;
        }
    }

    @Override
    public List<Document> apply(Query query) {
        return DocumentRetriever.super.apply(query);
    }

    public static Builder builder() {
        return new Builder();
    }


    //设置 Builder
    public static final class Builder {
        private HybridSearchService hybridSearchService;
        private VectorStore vectorStore;

        public Builder() {
        }

        public Builder hybridSearchService(HybridSearchService hybridSearchService) {
            this.hybridSearchService = hybridSearchService;
            return this;
        }
        public Builder vectorStore(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
            return this;
        }
        public HybridDocumentRetriever build() {
            return new HybridDocumentRetriever(this.hybridSearchService, this.vectorStore);
        }
    }
}
