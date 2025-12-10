package com.example.openai.octopathsp;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class AnalyzerTestService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    private final String indexName = "custom-character-index-1024-v2";

    /**
     * 测试1：全文搜索（使用 ik_smart 分词器）
     * 对应：
     * GET /custom-character-index-1024-v2/_search
     * {
     *   "query": {
     *     "match": {
     *       "content": "人工智能 技术"
     *     }
     *   }
     * }
     */
    @Test
    public void fullTextSearch() throws IOException {
        // 方法1：使用 ElasticsearchClient 直接构建请求
        SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q
                        .match(m -> m
                                .field("content")
                                .query(v -> v.stringValue("巴尔杰罗"))
                        )
                )
        );

        SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);

        List<Map<String, Object>> results = processSearchResponse(response);
        System.out.println("查询结果: ");
        results.forEach(System.out::println);
    }

    /**
     * 测试2：多字段搜索
     * 对应：
     * GET /custom-character-index-1024-v2/_search
     * {
     *   "query": {
     *     "multi_match": {
     *       "query": "人工智能",
     *       "fields": ["content", "metadata.title"]
     *     }
     *   }
     * }
     */
    @Test
    public void multiFieldSearch() throws IOException {
        // 方法1：使用 ElasticsearchClient
        SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q
                        .multiMatch(m -> m
                                .query("巴尔杰罗")
                                .fields("metadata.title")
                        )
                )
        );

        SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);
        List<Map<String, Object>> results = processSearchResponse(response);
        System.out.println("查询结果: ");
        results.forEach(System.out::println);
    }

    /**
     * 处理 SearchResponse
     */
    private List<Map<String, Object>> processSearchResponse(SearchResponse<Map> response) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", hit.id());
            result.put("score", hit.score());
            result.put("source", hit.source());
            results.add(result);
        }

        return results;
    }

    /**
     * 测试索引的分词效果
     */
    @Test
    public void testAnalyzer() throws IOException {
        String indexName = "custom-character-index-1024-v2";

        // 测试1: 使用 ik_smart 分词器
        System.out.println("=== 测试 ik_smart 分词器 ===");
        testWithAnalyzer(indexName, "ik_smart", "巴尔杰罗");

        // 测试3: 使用字段的分词器
        System.out.println("\n=== 测试 content 字段分词效果 ===");
        testWithField(indexName, "content", "巴尔杰罗");

        // 测试4: 测试 metadata.title 字段
        System.out.println("\n=== 测试 metadata.title 字段分词效果 ===");
        testWithField(indexName, "metadata.title", "中文标题测试文档");
    }

    /**
     * 使用指定分词器测试
     */
    private void testWithAnalyzer(String indexName, String analyzer, String text) throws IOException {
        AnalyzeRequest request = AnalyzeRequest.of(a -> a
                .index(indexName)
                .analyzer(analyzer)
                .text(text)
        );

        AnalyzeResponse response = elasticsearchClient.indices().analyze(request);

        System.out.println("原文: " + text);
        System.out.print("分词结果: ");
        response.tokens().forEach(token -> System.out.print(token.token() + " | "));
        System.out.println();
    }

    /**
     * 使用字段的分词器测试
     */
    private void testWithField(String indexName, String field, String text) throws IOException {
        AnalyzeRequest request = AnalyzeRequest.of(a -> a
                .index(indexName)
                .field(field)
                .text(text)
        );

        AnalyzeResponse response = elasticsearchClient.indices().analyze(request);

        System.out.println("字段: " + field);
        System.out.println("原文: " + text);
        System.out.print("分词结果: ");
        response.tokens().forEach(token -> System.out.print(token.token() + " | "));
        System.out.println();
    }

    /**
     * 对比不同分词器的效果
     */
    public void compareAnalyzers(String text) throws IOException {
        String indexName = "custom-character-index-1024-v2";

        List<String> analyzers = List.of("ik_smart", "ik_max_word", "standard");

        for (String analyzer : analyzers) {
            AnalyzeRequest request = AnalyzeRequest.of(a -> a
                    .index(indexName)
                    .analyzer(analyzer)
                    .text(text)
            );

            AnalyzeResponse response = elasticsearchClient.indices().analyze(request);

            System.out.println("\n=== " + analyzer + " ===");
            System.out.print("分词结果: ");
            response.tokens().forEach(token -> System.out.print(token.token() + " | "));
        }
        System.out.println();
    }
}