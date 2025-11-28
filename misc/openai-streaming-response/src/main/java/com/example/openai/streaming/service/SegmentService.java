package com.example.openai.streaming.service;

import com.example.openai.streaming.bean.SegmentReq;
import com.example.openai.streaming.util.MinioUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.COSParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author n039920
 * @date 2025/11/5
 * @description TODO
 */
@Slf4j
@Service
public class SegmentService {

    @Autowired
    private MinioUtils minioClient;

    @Autowired
    private VectorStore vectorStore;

    private final TokenTextSplitter textSplitter;

    public SegmentService() {
        this.textSplitter = new TokenTextSplitter();
    }

    public Map<String, Object> segment() {
        SegmentReq config = new SegmentReq();
        config.setUrl("http://192.168.102.19:9001/1833773232281493504/密云水库工程技术手册.pdf");
        config.setSplitStrategy("titlesplit");
        config.setTitleLevel(4);
        config.setFileId("1910598684589146112");
        config.setChunkSize(5);
        config.setOverlap(1);
        config.setSeparators("\n");
        config.setTenantId("1");
        log.info("入参: {}", config);

        String url = config.getUrl();

        // 检查文件类型
        String[] supportedTypes = {".docx", ".md", ".txt", ".pptx", ".pdf"};
        boolean isSupported = false;
        for (String ext : supportedTypes) {
            if (url.endsWith(ext)) {
                isSupported = true;
                break;
            }
        }

        if (!isSupported) {
            return Map.of(
                    "code", "9",
                    "data", Map.of(),
                    "msg", String.format("感谢您上传。目前，我们只处理特定格式的文档，包括%s。请上传这些格式的文档。",
                            String.join(", ", supportedTypes))
            );
        }

        if (url.endsWith(".pdf")) {
            try {
                return processPdf(config);
            } catch (Exception e) {
                log.error("PDF处理错误", e);
                return Map.of(
                        "code", "9",
                        "data", Map.of(),
                        "msg", e.getMessage()
                );
            }
        } else {
            return Map.of(
                    "code", "8",
                    "data", Map.of(),
                    "msg", "不存在桶或其相应的对象或URL格式不符合预期，请提供正确的URL。"
            );
        }
    }

    private Map<String, Object> processPdf(SegmentReq request) {
        boolean useTable = false;
        boolean useImage = false;
        log.info("是否使用表格：{}", useTable);
        log.info("是否使用图片：{}", useImage);

        String url = request.getUrl();
        String[] urlParts = url2fileInfo(url);
        String bucketName = urlParts[1];
        String fileName = urlParts[2];

        log.info("处理PDF...");

        String userFileName = request.getUserFileName();
        String objectName = extractObjectName(url);
        String fileId = request.getFileId();

        try {
            // 从MinIO获取文件
            InputStream fileStream = minioClient.download(bucketName,objectName);

            List<Document> docs = extractPdfContent(fileStream);
            log.debug("解析完成后的页数：{}", docs.size());

            // 切片处理
            log.debug("开始切片！");
            log.debug("split_strategy: {} -- level: {}",
                    request.getSplitStrategy(), request.getTitleLevel());

            if ("chunksplit".equals(request.getSplitStrategy())) {
                docs = splitDocumentsByChunk(docs);
            } else if ("titlesplit".equals(request.getSplitStrategy())) {
                docs = splitDocumentsByTitle(docs, fileStream, request.getTitleLevel(),
                        bucketName, useTable, useImage);
            }

            log.debug("最终片数：{}", docs.size());

            if (docs.isEmpty()) {
                log.error("切片未获得任何内容，片数必须大于0！");
                return Map.of("code", "121", "msg", "上传的文档疑似为不含任何字符,解析失败");
            }

            // 更新元数据
            String finalFileName = userFileName != null ? userFileName : fileName;
            List<String> sharedTenantIds = new ArrayList<>();
            if (request.getSharedTenantIdList() != null) {
                sharedTenantIds.addAll(request.getSharedTenantIdList());
            }
            if (request.getTenantId() != null) {
                sharedTenantIds.add(request.getTenantId());
            }
            List<Document> updatedDocs = new ArrayList<>();
            for (int segmentId = 0; segmentId < docs.size(); segmentId++) {
                Document originalDoc = docs.get(segmentId);
                Map<String, Object> metadata = new HashMap<>(originalDoc.getMetadata());
                metadata.put("file_name", finalFileName);
                metadata.put("file_id", fileId);
                metadata.put("segment_id", segmentId + 1);
                metadata.put("state", true);
                metadata.put("tenant_id", request.getTenantId());
                metadata.put("shared_tenant_id_list", sharedTenantIds);

                // 创建新的Document对象，因为Document是不可变的
                Document updatedDoc = new Document(originalDoc.getText(), metadata);
                updatedDocs.add(updatedDoc);
            }

            // 存储到向量数据库
            log.debug("开始上传向量存储");
            vectorStore.add(updatedDocs);
            log.info("数据上传成功");

            return Map.of(
                    "code", "0",
                    "msg", finalFileName + " 切片成功",
                    "data", Map.of("file_id", fileId)
            );

        } catch (Exception e) {
            log.error("PDF处理错误", e);
            return Map.of(
                    "code", "9",
                    "data", Map.of(),
                    "msg", "PDF处理失败: " + e.getMessage()
            );
        }
    }

    private List<Document> extractPdfContent(InputStream pdfStream) throws Exception {
        List<Document> documents = new ArrayList<>();
        RandomAccessRead randomAccessRead = new RandomAccessReadBuffer(pdfStream);
        COSParser cosParser = new COSParser(randomAccessRead);
        COSDocument cosDoc = new COSDocument(cosParser);
        try (PDDocument document = new PDDocument(cosDoc)) {
            PDFTextStripper stripper = new PDFTextStripper();
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

    private List<Document> splitDocumentsByChunk(List<Document> docs) {
        List<Document> result = new ArrayList<>();

        for (Document doc : docs) {
            List<Document> splitDocs = textSplitter.apply(List.of(doc));
            result.addAll(splitDocs);
        }

        return result;
    }

    private List<Document> splitDocumentsByTitle(List<Document> docs, InputStream pdfStream,
                                                 Integer titleLevel, String bucketName, boolean useTable, boolean useImage) {
        // 这里需要实现基于标题的分割逻辑
        // 由于Java中没有直接的fitz等价物，可以使用Apache PDFBox或其他库
        // 这里简化实现，返回原始文档
        log.warn("标题分割功能在Java中需要额外实现，当前返回原始文档");
        return docs;
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

    // 编辑距离计算（用于标题匹配）
    private int levenshteinDistance(String s1, String s2) {
        if (s1.length() < s2.length()) {
            return levenshteinDistance(s2, s1);
        }

        if (s2.isEmpty()) {
            return s1.length();
        }

        int[] previousRow = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) {
            previousRow[j] = j;
        }

        for (int i = 0; i < s1.length(); i++) {
            int[] currentRow = new int[s2.length() + 1];
            currentRow[0] = i + 1;

            for (int j = 0; j < s2.length(); j++) {
                int insertions = previousRow[j + 1] + 1;
                int deletions = currentRow[j] + 1;
                int substitutions = previousRow[j] + (s1.charAt(i) != s2.charAt(j) ? 1 : 0);
                currentRow[j + 1] = Math.min(Math.min(insertions, deletions), substitutions);
            }

            previousRow = currentRow;
        }

        return previousRow[s2.length()];
    }
}
