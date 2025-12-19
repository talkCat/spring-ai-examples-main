package com.example.openai.streaming;

import com.example.openai.streaming.bean.StreamResponse;
import com.example.openai.streaming.configuration.VectorStoreFactory;
import com.example.openai.streaming.tools.TimeTool;
import com.example.openai.streaming.tools.WeatherTool;
import com.example.openai.streaming.util.AdvancedBookmarkParser;
import com.example.openai.streaming.util.MinioUtils;
import com.knuddels.jtokkit.api.EncodingType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.writer.FileDocumentWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author n039920
 * @date 2025/11/6
 * @description TODO
 */
@SpringBootTest
public class DocumentReadersTest {

    @Autowired
    private MinioUtils minioClient;

    @Autowired
    private VectorStore vectorStore;

    //@Autowired
    private VectorStore index2VectorStore;

    @Autowired
    VectorStoreFactory vectorStoreFactory;

    @Autowired
    OpenAiChatModel chatModel;

    @Autowired
    JdbcChatMemoryRepository chatMemoryRepository;

    @Autowired
    ChatClient chatClient;

    @Autowired
    ChatClient chatClient02;


    private static final Logger logger = LoggerFactory.getLogger(DocumentReadersTest.class);


    //mcp 测试
    @Test
    public void testMcp(){
        System.out.println("\n>>> ASSISTANT: " + chatClient02.prompt("美国的NY天气如何").call().content());
    }


    //返回一个由返回值映射而来的实体
    @Test
    public void testReturnMap() throws Exception {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        StreamResponse actorFilms = chatClient.prompt()
                .user("今天天气不错呦。")
                .call()
                .entity(StreamResponse.class);
        System.out.println(actorFilms.getContent());
    }

    //日志记录
    @Test
    public void testLog() throws Exception {
        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "Custom request: " + request.prompt().getUserMessage(),
                response -> "Custom response: " + response.getResult().getOutput().getText(),
                1
        );

        StreamResponse response = ChatClient.create(chatModel)
                .prompt()
                .advisors(customLogger)
                .user("可以给我讲个笑话吗?")
                .call()
                .entity(StreamResponse.class);
    }

    @Test
    public void test1() throws Exception {
        List<Document> documents = AdvancedBookmarkParser.parsePdfToDocuments("C:\\Users\\Administrator\\Desktop\\临时文件\\20251128\\demo.pdf");

        // 更保守的分割配置
        TokenTextSplitter splitter = new TokenTextSplitter(2000, 350, 5, 3000, true);

        List<Document> splitDocuments = splitter.apply(documents);

        vectorStore.add(splitDocuments);

    }

    //使用TokenCountBatchingStrategy进行批处理
    @Test
    public void test03() throws Exception {
        List<Document> documents = AdvancedBookmarkParser.parsePdfToDocuments("D:\\project\\spring-ai-examples-main\\misc\\openai-streaming-response\\src\\main\\resources\\111.pdf");
        //使用TokenCountBatchingStrategy进行批处理
        BatchingStrategy batchingStrategy = new TokenCountBatchingStrategy(
                EncodingType.CL100K_BASE,  // Specify the encoding type
                8000,                      // Set the maximum input token count
                0.1                        // Set the reserve percentage
        );
        List<List<Document>> batches = batchingStrategy.batch(documents);
    }

    //元数据过滤器
    @Test
    public void test05() throws Exception {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query("密云水库")
                .topK(10)
                .similarityThreshold(0.7)
                .filterExpression("'page' == '103'").build());

        for (Document doc : documents) {
            System.out.println("文档内容摘要: " + doc.getText() + "...");
            System.out.println("文档元数据: " + doc.getMetadata());
            System.out.println("---");
        }
    }

    //元数据过滤器
    @Test
    public void test04() throws Exception {
        String content = chatClient.prompt()
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "'page' == '13'"))
                .user("密云水库在哪")
                .call().content();
        System.out.println(content);
    }

    //CompressionQueryTransformer 问题改写
    //使用QueryTransformer时，建议配置ChatClient。使用低温（例如0.0）构建器，以确保更确定和准确的结果，提高检索质量。
    // 大多数聊天模型的默认温度通常过高，无法进行最佳查询转换，从而导致检索效率降低。
    @Test
    public void test07() throws Exception {
        Query query = Query.builder()
                .text("How about the third one?")
                .history(
                        new UserMessage("Can you list the top 5 largest cities in Germany by population?"),
                        new AssistantMessage("Sure! The top 5 largest cities in Germany by population are: 1. Berlin (3.6 million), 2. Hamburg (1.8 million), 3. Munich (1.5 million), 4. Cologne (1.1 million), 5. Frankfurt (760,000)."),
                        new UserMessage("What's special about the second city you mentioned?"),
                        new AssistantMessage("Hamburg, the second largest city, is known as Germany's 'Gateway to the World' due to its major port. It has more bridges than Venice and Amsterdam combined, and is famous for its Speicherstadt warehouse district, a UNESCO World Heritage site."),
                        new UserMessage("Tell me more about its port history"),
                        new AssistantMessage("Hamburg's port has a rich history dating back to the 9th century. It became a key member of the Hanseatic League in the Middle Ages, facilitating trade across Northern Europe. Today, it's one of Europe's largest ports and features the famous Miniatur Wunderland, the world's largest model railway."),
                        new UserMessage("Compare it with the first city"),
                        new AssistantMessage("While Berlin is the capital and known for politics, culture, and history, Hamburg is a major commercial and trade hub. Berlin is inland with river access, while Hamburg is a coastal port city. Berlin has more museums and historical sites, while Hamburg's identity is closely tied to its maritime economy and Hanseatic heritage.")
                )
                .build();

        QueryTransformer queryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel)
                        .defaultOptions(ChatOptions.builder()
                                .temperature(0.7)
                                .build()))
                .build();

        Query transformedQuery = queryTransformer.transform(query);
        System.out.println(transformedQuery.text());
    }

    // 问题提炼
    @Test
    public void test08() throws Exception {
        Query query = new Query("Hey, so I was talking to my friend yesterday about AI stuff, and we got into this whole discussion about neural networks, but anyway, I'm actually trying to understand what exactly is transformer architecture in machine learning? Like, I know it has something to do with attention but I'm not sure how it works fundamentally.");

        QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel)
                        .defaultOptions(ChatOptions.builder()
                                .temperature(0.0)
                                .build()))
                .build();

        Query transformedQuery = queryTransformer.transform(query);
        System.out.println(transformedQuery.text());
    }

    //MultiQueryExpander
    @Test
    public void test09() throws Exception {
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(chatModel)
                        .defaultOptions(ChatOptions.builder()
                                .temperature(0.0)
                                .build()))
                .numberOfQueries(3)
                .build();
        List<Query> queries = queryExpander.expand(new Query("如何运行Spring Boot应用程序？"));
        for (Query query : queries) {
            System.out.println("Query: " + query.text());
        }
    }

    // 检索
    @Test
    public void test10() throws Exception {

        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.73)
                .topK(5)
                .filterExpression(new FilterExpressionBuilder()
                        .eq("page", "103")
                        .build())
                .build();
        List<Document> documents = retriever.retrieve(new Query("What is the main character of the story?"));
    }

    // 多问题 多数据源检索
    @Test
    public void test11() throws Exception {
        Map<Query, List<List<Document>>> documentsForQuery = new HashMap<>();

        DocumentJoiner documentJoiner = new DocumentJoiner() {
            @Override
            public List<Document> join(Map<Query, List<List<Document>>> documentsForQuery) {
                return documentsForQuery.values().stream()
                        .flatMap(List::stream)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
        };
        List<Document> documents = documentJoiner.join(documentsForQuery);
    }

    //关键词元数据增强器实例
    @Test
    public void test2() throws Exception {
        KeywordMetadataEnricher enricher = KeywordMetadataEnricher.builder(chatModel)
                .keywordCount(5)
                .build();
        List<Document> documents = AdvancedBookmarkParser.parsePdfToDocuments("D:\\project\\spring-ai-examples-main\\misc\\openai-streaming-response\\src\\main\\resources\\111.pdf").subList(0, 10);
        List<Document> splitDocuments = enricher.apply(documents);
        for (Document doc : splitDocuments) {
            String keywords = (String) doc.getMetadata().get(KeywordMetadataEnricher.EXCERPT_KEYWORDS_METADATA_KEY); // 默认键是 "spring_ai_keywords"
            System.out.println("文档内容摘要: " + doc.getText().substring(0, 50) + "...");
            System.out.println("生成的关键词: " + keywords);
            System.out.println("---");
        }
    }

    // 问题增强
    @Test
    public void test12() throws Exception {
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .build();
    }

    //tool call
    @Test
    public void test13() throws Exception {

        ToolCallback[] customerTools = ToolCallbacks.from(new TimeTool(), new WeatherTool());

        ChatClient chatClient = ChatClient
                .builder(chatModel)
                .defaultOptions(ToolCallingChatOptions.builder()
                        .toolCallbacks(customerTools)
                        //.toolContext(Map.of("tenantId", "acme"))
                        .build()).build();
        String content = chatClient.prompt().user("现在几点了？南京的天气如何？").call().content();
        System.out.println(content);
    }

    @Test
    public void test() throws Exception {
        String url = "http://192.168.102.19:9001/1833773232281493504/密云水库工程技术手册.pdf";
        String[] urlParts = url2fileInfo(url);
        String bucketName = urlParts[1];
        String objectName = extractObjectName(url);
        // 从MinIO获取文件
        InputStream fileStream = minioClient.download(bucketName, objectName);

        List<Document> documents = extractPdfContent(fileStream);
        //List<Document> documents = getDocsFromPdfWithCatalog();
        for (Document doc : documents) {
            System.out.println("Chunk: " + doc.getText());
            System.out.println("Metadata: " + doc.getMetadata());
        }
    }

    private String[] url2fileInfo(String url) {
        String[] parts = url.split("/");
        String bucketName = parts[3];
        String fileName = parts[parts.length - 1];
        return new String[]{url, bucketName, fileName};
    }

    private String extractObjectName(String url) {
        String[] parts = url.split("/");
        StringBuilder objectName = new StringBuilder();
        for (int i = 4; i < parts.length; i++) {
            if (objectName.length() > 0) {
                objectName.append("/");
            }
            objectName.append(parts[i]);
        }
        return objectName.toString();
    }

    private List<Document> extractPdfContent(InputStream pdfStream) throws Exception {
        List<Document> documents = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(new File("D:\\project\\spring-ai-examples-main\\misc\\openai-streaming-response\\src\\main\\resources\\111.pdf"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            // 递归遍历目录结构
            traverseBookmarks(outline, 0);
            for (int pageNumber = 0; pageNumber < document.getNumberOfPages(); pageNumber++) {
                stripper.setStartPage(pageNumber + 1);
                stripper.setEndPage(pageNumber + 1);
                String text = stripper.getText(document);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("text", text);
                metadata.put("content_pages_number", pageNumber);
                metadata.put("content_table", new ArrayList<>());
                metadata.put("content_image", new ArrayList<>());
                documents.add(new Document(text, metadata));
            }
        }
        return documents;
    }

    /**
     * 递归遍历书签/目录结构
     *
     * @param node  当前节点
     * @param level 当前层级（用于缩进显示）
     */
    private static void traverseBookmarks(PDOutlineNode node, int level) {
        if (node == null) {
            return;
        }

        PDOutlineItem current = node.getFirstChild();
        while (current != null) {
            // 根据层级缩进显示
            String indent = "  ".repeat(level);
            String title = current.getTitle();

            System.out.println(indent + "📖 " + title);

            // 如果有子书签，递归遍历
            if (current.hasChildren()) {
                traverseBookmarks(current, level + 1);
            }

            current = current.getNextSibling();
        }
    }

    // 写入文件
    public static void writeDocuments(List<Document> documents) {
        FileDocumentWriter writer = new FileDocumentWriter("output.txt", true, MetadataMode.ALL, false);
        writer.accept(documents);
    }

    //PDF段落 按章节目录拆分
    public static List<Document> getDocsFromPdfWithCatalog() {
        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader("classpath:/222.pdf",
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .withPagesPerDocument(1)
                        .build());
        return pdfReader.read();
    }

    //PDF页面
    public static List<Document> getDocsFromPdf() {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader("classpath:/111.pdf",
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .withPagesPerDocument(1)
                        .build());
        return pdfReader.read();
    }

    //Advisors API demo
    @Test
    public void test06() throws Exception {
        VectorStore vectorStore = vectorStoreFactory.createVectorStore("custom-index-1024-6", 1024);
        addVectorStore(vectorStore);
    }

    public List<Document> addVectorStore(VectorStore vectorStore3) {
        // 1. 检查实际使用的模型和维度
        System.out.println("Using VectorStore: " + vectorStore3.getClass().getSimpleName());
        List<Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!"),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future."));
        vectorStore3.add(documents);
        List<Document> results = vectorStore3.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
        return results;
    }
}
