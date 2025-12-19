package com.example.openai.octopathsp;

import com.example.openai.octopathsp.tools.TimeTool;
import com.example.openai.octopathsp.tools.WeatherTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.reader.JsonMetadataGenerator;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    OpenAiChatModel chatModel;

    @Autowired
    ChatClient chatClientAdvisor;

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

    @Test
    public void test13() throws Exception {
        ToolCallback[] customerTools = ToolCallbacks.from(new TimeTool(), new WeatherTool());
        ChatClient chatClient = ChatClient
                .builder(chatModel)
                .defaultOptions(ToolCallingChatOptions.builder()
                        .toolCallbacks(customerTools)
                        .build()).build();
        String content = chatClient.prompt().user("查询南京当前的时间和天气").call().content();
        System.out.println(content);
    }

    @Test
    public void test14() throws Exception {
        String content = chatClientAdvisor.prompt().user("结合CPI指数，和 央行新闻 分析一下当前股票：SH600519 的情况，给我投资建议").call().content();
        System.out.println(content);
    }
}
