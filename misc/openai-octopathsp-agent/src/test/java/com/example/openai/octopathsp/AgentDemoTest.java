package com.example.openai.octopathsp;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.openai.octopathsp.agent.CharacterAgent;
import com.example.openai.octopathsp.bean.AgentRoute;
import com.example.openai.octopathsp.prompt.PromptTemplateCustomize;
import com.example.openai.octopathsp.tools.AccessoryTool;
import com.example.openai.octopathsp.tools.TimeTool;
import com.example.openai.octopathsp.tools.WeatherTool;
import com.example.openai.octopathsp.utils.AdvancedBookmarkParser;
import com.example.openai.octopathsp.utils.AgentRouter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author n039920
 * @date 2025/11/28
 * @description TODO
 */
@SpringBootTest
public class AgentDemoTest {

    @Autowired
    ChatClient chatClient;

    @Autowired
    VectorStore indexCharacterVectorStore;

    @Autowired
    CharacterAgent characterAgent;

    @Autowired
    AgentRouter agentRouter;

    @Autowired
    ChatModel chatModel;

    @Autowired
    ElasticsearchClient elasticsearchClient;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void test5() throws Exception {
        List<Document> documents = AdvancedBookmarkParser.parsePdfToDocuments("C:\\Users\\Administrator\\Desktop\\临时文件\\20251128\\demo.pdf");
        // 更保守的分割配置
        TokenTextSplitter splitter = new TokenTextSplitter(2000, 350, 5, 3000, true);
        List<Document> splitDocuments = splitter.apply(documents);
        indexCharacterVectorStore.add(splitDocuments);
    }


    @Test
    public void test() {
        String userQuery = "有哪技能可以使伤害上限增加？";
        AgentRoute agentRoute = agentRouter.route(userQuery);
        System.out.println(agentRoute);
    }

    @Test
    public void test05() throws Exception {
        List<Document> documents = indexCharacterVectorStore.similaritySearch(SearchRequest.builder()
                .query("有哪角色可以使伤害上限增加？")
                .topK(10)
                //.similarityThreshold(0.51).build());
                .build());

        for (Document doc : documents) {
            System.out.println("文档内容摘要: " + doc.getText() + "...");
            System.out.println("文档元数据: " + doc.getMetadata());
            System.out.println("---");
        }
    }


    @Test
    public void test2() {

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .template("""
            分析用户关于《歧路旅人：大陆的霸者》游戏的问题意图，返回JSON格式：
                    {
                        "intent": "STORY|CHARACTER|TEAM|PET|MECHANISM|COMPREHENSIVE",
                        "confidence": 0.0-1.0,
                        "sub_intent": "具体的子意图",
                        "entities": {
                            "character_names": ["角色名列表"],
                            "skill_types": ["技能类型"],
                            "story_parts": ["剧情章节"]
                        }
                    }
                    
                    意图类型说明：
                    - STORY: 剧情相关问题（主线、支线、任务等）
                    - CHARACTER: 角色相关问题（技能、属性、觉醒等）
                    - TEAM: 队伍配置问题（阵容搭配、角色组合等）
                    - PET: 宠物相关问题
                    - MECHANISM: 游戏机制问题
                    - COMPREHENSIVE: 综合问题
                    
                    用户问题：<query>
            """)
                .build();

        String prompt = promptTemplate.render(Map.of("query", "欧菲莉亚的技能是什么"));

        String intentJson = chatClient.prompt()
                .user(prompt)
                .options(ChatOptions.builder()
                        .temperature(0.0) // 低温度确保分类稳定
                        .build())
                .call()
                .content();
                System.out.println(intentJson);
    }

    @Test
    public void test3() {
        String response = chatClient.prompt()
                .system(PromptTemplateCustomize.SYSTEM_PROMPT)
                .user("""
                        上下文信息：
                        角色名：巴尔杰罗
                        主动技能
                        1、打算恶作剧
                        消耗精力24
                        对敌方单体施以2次短剑物理攻击（威力75）
                        并赋予短剑耐性降低10%的效果（2回合）
                                                
                        角色名：左翼
                        主动技能
                        1、四连乱风冲
                        消耗精力50
                        对随机目标施以4次风属性攻击（威力45）。
                        【BP加成效果】威力55/65/90 \s
                                                
                        请你根据上下文信息回答：
                        有哪些角色技能可以降低短剑耐性
                        """)
                .call()
                .content();
        System.out.println(response);
    }

    public static String readJsonFileAsString(String fileName) {
        // 1. 获取类加载器
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // 2. 获取资源文件的输入流
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // 3. 检查文件是否存在
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found in resources: " + fileName);
        }

        // 4. 将输入流转换为字符串
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    @Test
    public void test4() {
        try {
            String jsonContent = readJsonFileAsString("accessories.json");
            //System.out.println("JSON content loaded successfully!");
            System.out.println(jsonContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test13() throws Exception {

        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "Custom request: " + request.prompt().getUserMessage(),
                response -> "Custom response: " + response.getResult().getOutput().getText(),
                1
        );

        ToolCallback[] customerTools = ToolCallbacks.from(new TimeTool(), new WeatherTool(), new AccessoryTool());

        ChatClient chatClient = ChatClient
                .builder(chatModel)
                .defaultAdvisors(customLogger)
                .defaultOptions(ToolCallingChatOptions.builder()
                        .toolCallbacks(customerTools)
                        .build()).build();
        String content = chatClient.prompt().user("休吉普饰品效果？").call().content();
        System.out.println(content);
    }
}
