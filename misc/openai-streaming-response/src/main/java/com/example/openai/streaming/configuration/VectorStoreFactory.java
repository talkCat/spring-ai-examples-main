package com.example.openai.streaming.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author n039920
 * @date 2025/11/13
 * @description TODO
 */
@Component
public class VectorStoreFactory {

    private final RestClient restClient;
    private final EmbeddingModel embeddingModel;
    private final ElasticsearchClient elasticsearchClient;
    private final Map<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();

    public VectorStoreFactory(RestClient restClient, EmbeddingModel embeddingModel, ElasticsearchClient elasticsearchClient) {
        this.restClient = restClient;
        this.embeddingModel = embeddingModel;
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 获取指定索引的 VectorStore
     */
    public VectorStore getVectorStore(String indexName) {
        return getVectorStore(indexName, 1024, SimilarityFunction.cosine);
    }

    /**
     * 获取指定索引和配置的 VectorStore
     */
    public VectorStore getVectorStore(String indexName, int dimensions, SimilarityFunction similarity) {
        return vectorStoreCache.computeIfAbsent(indexName, key -> {
            ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
            options.setDimensions(dimensions);
            options.setIndexName(indexName);
            options.setSimilarity(similarity);
            return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                    .initializeSchema(true)
                    .options(options)
                    .build();
        });
    }

    /**
     * 动态创建新的 VectorStore
     */
    public VectorStore createVectorStore(String indexName, int dimensions) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setDimensions(dimensions);
        options.setIndexName(indexName);
        if (!indexExists(options)) {
            createIndexMapping(options);
        }
        return ElasticsearchVectorStore.builder(restClient, embeddingModel).options(options).initializeSchema(true).build();
    }

    public boolean indexExists(ElasticsearchVectorStoreOptions options) {
        try {
            return elasticsearchClient.indices().exists(ex -> ex.index(options.getIndexName())).value();
        } catch (IOException e) {
            throw new RuntimeException("Failed to check if index exists: " + options.getIndexName(), e);
        }
    }

    private void createIndexMapping(ElasticsearchVectorStoreOptions options) {
        try {
            this.elasticsearchClient.indices().create(cr ->
                    cr.index(options.getIndexName())
                            .mappings(map ->
                                    map.properties(options.getEmbeddingFieldName(), p ->
                                            p.denseVector(dv ->
                                                    dv.dims(options.getDimensions())
                                            )
                                    )
                            )
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to create index mapping for: " + options.getIndexName(), e);
        }
    }

    /**
     * 移除缓存中的 VectorStore
     */
    public void removeVectorStore(String indexName) {
        vectorStoreCache.remove(indexName);
    }

    /**
     * 获取所有已创建的索引名称
     */
    public Set<String> getAllIndexNames() {
        return vectorStoreCache.keySet();
    }
}
