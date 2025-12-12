package com.example.openai.octopathsp.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author n039920
 * @date 2025/12/9
 * @description TODO
 */
@Component
public class HybridSearchService {

    @Autowired
    EmbeddingModel embeddingModel;

    /**
     * 方法1：使用 knn 结合 bool 查询进行混合搜索
     * 这是 ES 8.x 中推荐的混合查询方式
     */
    public List<Document> hybridSearchWithKnnBool(String queryText,
                                                  Map<String, Object> textFilters,
                                                  int topK,
                                                  VectorStore vectorStore) throws IOException {
        // 1. 生成查询向量
        float[] queryVectorFloat = embeddingModel.embed(queryText);
        List<Float> queryVector = new ArrayList<>();
        for (float f : queryVectorFloat) {
            queryVector.add(f);
        }

        // 2. 构建 KnnSearch 对象
        KnnSearch knnSearch = KnnSearch.of(k -> k
                .field("embedding")
                .queryVector(queryVector)
                .k(topK * 2)
                .numCandidates(topK * 4)
        );

        // 3. 构建 bool 查询用于文本过滤
        Query textQuery = buildTextQuery(textFilters);

        // 4. 构建完整的搜索请求 - 传递 List<KnnSearch>
        SearchRequest request = SearchRequest.of(s -> s
                .index(getIndexNameByMethodInvocation((ElasticsearchVectorStore)vectorStore))
                .knn(Collections.singletonList(knnSearch))  // 传递 List<KnnSearch>
                .query(textQuery)
                .size(topK)
        );

        return executeSearch(request, vectorStore);
    }



    /**
     * 辅助方法：构建文本查询
     */
    private Query buildTextQuery(Map<String, Object> textFilters) {
        if (textFilters == null || textFilters.isEmpty()) {
            return Query.of(q -> q.matchAll(ma -> ma));
        }

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        for (Map.Entry<String, Object> entry : textFilters.entrySet()) {
            boolBuilder.must(m -> m
                    .match(mt -> mt
                            .field(entry.getKey())
                            .query(entry.getValue().toString())
                    )
            );
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }


    private List<Document> executeSearch(SearchRequest request, VectorStore vectorStore) throws IOException {
        ElasticsearchClient elasticsearchClient = (ElasticsearchClient) vectorStore.getNativeClient().get();
        SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);
        return convertToDocuments(response);
    }

    private List<Document> convertToDocuments(SearchResponse<Map> response) {
        List<Document> documents = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            documents.add(new Document(hit.id(), String.valueOf(source.get("content")), source));
        }
        return documents;
    }

    private String getIndexNameByMethodInvocation(ElasticsearchVectorStore esStore) {
        try {
            // 获取 options 字段
            Field optionsField = ElasticsearchVectorStore.class.getDeclaredField("options");
            optionsField.setAccessible(true);
            Object options = optionsField.get(esStore);

            // 尝试调用 getIndexName 方法
            Method getIndexNameMethod = options.getClass().getMethod("getIndexName");
            return (String) getIndexNameMethod.invoke(options);

        } catch (Exception e) {
            // 如果上述方法失败，使用更直接的方法
            return "custom-character-index-1024-v2";
        }
    }
}
