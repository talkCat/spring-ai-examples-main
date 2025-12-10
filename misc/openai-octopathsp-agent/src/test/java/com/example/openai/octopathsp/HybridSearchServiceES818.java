package com.example.openai.octopathsp;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.ai.document.Document;
import co.elastic.clients.json.JsonData;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.*;

@SpringBootTest
public class HybridSearchServiceES818 {

    @Autowired
    EmbeddingModel embeddingModel;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Test
    void testHybridSearchWithKnnBool() throws Exception {
        // 准备过滤条件
        Map<String, Object> filters = new HashMap<>();
        filters.put("metadata.title", "巴杰罗");

        // 执行混合搜索
        List<Document> results = hybridSearchWithKnnBool(
                "巴杰罗", filters, 10);

        System.out.println("混合搜索结果数量: " + results.size());
        results.forEach(doc -> {
            System.out.println("ID: " + doc.getId());
            System.out.println("内容: " + doc.getText());
            //System.out.println("元数据: " + doc.getMetadata());
            System.out.println("---");
        });
    }

    /**
     * 方法1：使用 knn 结合 bool 查询进行混合搜索
     * 这是 ES 8.x 中推荐的混合查询方式
     */
    public List<Document> hybridSearchWithKnnBool(String queryText,
                                                  Map<String, Object> textFilters,
                                                  int topK) throws IOException {
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
                .index("custom-character-index-1024-v2")
                .knn(Collections.singletonList(knnSearch))  // 传递 List<KnnSearch>
                .query(textQuery)
                .size(topK)
        );

        return executeSearch(request);
    }


    /**
     * 方式4：使用 combined_fields 查询（文本搜索优化）
     */
    public List<Document> combinedFieldsSearch(String queryText,
                                               List<String> fields,
                                               int k) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index("custom-character-index-1024-v2")
                .query(q -> q
                        .combinedFields(cf -> cf
                                .query(queryText)
                                .fields(fields)
                        )
                )
                .size(k)
        );

        return executeSearch(request);
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


    private List<Document> executeSearch(SearchRequest request) throws IOException {
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
}
