package com.example.openai.streaming.embedding;

import com.example.openai.streaming.service.SegmentService;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author n039920
 * @date 2025/11/4
 * @description TODO
 */
@RestController
public class EmbeddingController {

    private final EmbeddingModel embeddingModel;

    private final VectorStore vectorStore;

    @Autowired
    SegmentService segmentService;

    @Autowired
    public EmbeddingController(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/ai/embedding")
    public Map embed(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(message));
        System.out.println(embeddingResponse.getResult().getOutput().length);
        return Map.of("embedding", embeddingResponse);
    }

    @GetMapping("/ai/addVectorStore")
    public List<Document> addVector(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return addVectorStore();
    }

    @GetMapping("/ai/getVectorStore")
    public List<Document> getVectorStore(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        List<Document> results = this.vectorStore.similaritySearch(SearchRequest.builder().query(message).build());
        return results;
    }

    @GetMapping("/ai/segment")
    public Map<String, Object> segment(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return segmentService.segment();
    }

    public List<Document> addVectorStore(){
        // 1. 检查实际使用的模型和维度
        System.out.println("Using VectorStore: " + vectorStore.getClass().getSimpleName());
        List <Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!"),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future."));

        vectorStore.add(documents);
        List<Document> results = this.vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
        return results;
    }
}
