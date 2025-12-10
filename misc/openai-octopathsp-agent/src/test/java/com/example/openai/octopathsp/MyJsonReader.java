package com.example.openai.octopathsp;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonMetadataGenerator;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author n039920
 * @date 2025/12/10
 * @description TODO
 */
@SpringBootTest
public class MyJsonReader {


    @Autowired
    VectorStore indexAccessoryVectorStore;

    private final Resource resource;

    MyJsonReader(@Value("classpath:accessories.json") Resource resource) {
        this.resource = resource;
    }

    List<Document> loadJsonAsDocuments() {
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("饰品名称", "accessories.json");
        JsonReader jsonReader = new JsonReader(resource, new JsonMetadataGenerator() {
            @Override
            public Map<String, Object> generate(Map<String, Object> jsonMap) {
                // 从每个 JSON 对象中提取“饰品名称”作为元数据
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("name", jsonMap.get("饰品名称"));
                metadata.put("method", jsonMap.get("入手方法"));
                metadata.put("level", jsonMap.get("饰品等级"));
                metadata.put("panel", jsonMap.get("饰品面板"));
                metadata.put("description", jsonMap.get("饰品描述"));
                return metadata;
            }
        },"饰品名称","入手方法","饰品等级","饰品面板","饰品描述");
        indexAccessoryVectorStore.add(jsonReader.get());
        return jsonReader.get();
    }

    @Test
    void testLoadJsonAsDocuments() {
        List<Document> documents = loadJsonAsDocuments();
        /*for (Document document : documents) {
            System.out.println("ID: " + document.getId());
            System.out.println("content: " + document.getText());
            System.out.println("metadata: " + document.getMetadata());
            System.out.println("---");
        }*/
    }
}
