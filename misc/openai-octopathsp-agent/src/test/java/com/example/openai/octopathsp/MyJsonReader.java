package com.example.openai.octopathsp;

import com.example.openai.octopathsp.tools.TimeTool;
import com.example.openai.octopathsp.tools.WeatherTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.JsonMetadataGenerator;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.URI;
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

    @Autowired
    ChatClient chatStructuredOutputClientAdvisor;

    @Autowired
    OpenAiChatModel multiChatModel;

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

    @Test
    public void test15() throws Exception {

        String prompt = buildWeatherPrompt("上海");
        // 添加系统指令，明确要求返回JSON
        String content = chatStructuredOutputClientAdvisor
                .prompt()
                .system("返回指定城市的天气信息JSON。")
                .user(prompt)
                .call()
                .content();
        System.out.println(content);
    }

    private String buildWeatherPrompt(String city) {
        return String.format("""
            请提供%s的详细天气信息，包括：
            1. 当前天气：温度、体感温度、天气状况、湿度、风速、风向、气压、能见度
            
            请以JSON格式返回，严格遵守Schema定义。
            """, city);
    }

    //多模态测试
    @Test
    public void test16() throws Exception {
        String response = ChatClient.create(multiChatModel)
                .prompt()
                .user(u -> u.text("请描述图片内容。")
                        .media(MediaType.IMAGE_PNG, new ClassPathResource("/test002.webp")))
                .call()
                .content();
        System.out.println(response);
    }

    //多模态测试 mp3
    @Test
    public void test17() throws Exception {
        String response = ChatClient.create(multiChatModel)
                .prompt()
                .user(u -> u.text("请描述音频内容。")
                        .media(new Media(MimeTypeUtils.parseMimeType("audio/mp3"), new ClassPathResource("/test.mp3"))))
                .call()
                .content();
        System.out.println(response);
    }
}
