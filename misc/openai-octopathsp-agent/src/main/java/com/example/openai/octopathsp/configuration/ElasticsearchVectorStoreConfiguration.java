package com.example.openai.octopathsp.configuration;

import com.knuddels.jtokkit.api.EncodingType;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchVectorStoreConfiguration {

    private final RestClient restClient;

    public ElasticsearchVectorStoreConfiguration(RestClient restClient) {
        this.restClient = restClient;
    }

    @Bean
    public VectorStore elasticsearchVectorStore(RestClient restClient, EmbeddingModel embeddingModel) {
        return ElasticsearchVectorStore.builder(restClient, embeddingModel).build();
    }

    @Bean
    public VectorStore index2VectorStore(RestClient restClient, EmbeddingModel embeddingModel) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setDimensions(1024);
        options.setIndexName("custom-index-1024-2");
        return ElasticsearchVectorStore.builder(restClient, embeddingModel).initializeSchema(true).options(options).build();
    }

    @Bean
    public VectorStore index3VectorStore(RestClient restClient, EmbeddingModel embeddingModel) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setDimensions(1024);
        options.setIndexName("custom-index-1024-3");
        return ElasticsearchVectorStore.builder(restClient, embeddingModel).initializeSchema(true).options(options).build();
    }

    @Bean
    public VectorStore indexCharacterVectorStore(RestClient restClient, EmbeddingModel embeddingModel) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setDimensions(1024);
        options.setIndexName("custom-character-index-1024-v2");
        return ElasticsearchVectorStore.builder(restClient, embeddingModel).initializeSchema(true).options(options).build();
    }

    @Bean
    public VectorStore indexAccessoryVectorStore(RestClient restClient, EmbeddingModel embeddingModel) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setDimensions(1024);
        options.setIndexName("custom-accessory-index-1024");
        return ElasticsearchVectorStore.builder(restClient, embeddingModel).initializeSchema(true).options(options).build();
    }

    @Bean
    public BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy(
                EncodingType.CL100K_BASE,
                132900,  // Artificially high limit
                0.1      // 10% reserve
        );
    }
}
