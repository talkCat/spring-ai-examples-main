package com.example.openai.streaming.tools;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.writer.FileDocumentWriter;

import java.util.List;

/**
 * @author n039920
 * @date 2025/11/6
 * @description TODO
 */
public class DocumentReadersUtils {

    public static void main(String[] args) {
        /*Document doc1 = new Document("第 1 章 密云水库概况\n" +
                "密云水库坐落在燕山南麓密云区境内，距北京市区中心约 90 公里，总库容\n" +
                "43.75 亿立方米，为华北地区最大的水库。工程于 1958 年 9 月 1 日动工兴建，\n" +
                "1959 年汛期拦洪，1960 年 9 月 1 日基本建成，是一座以防洪、供水为主要功能\n" +
                "的综合利用、多年调节的大型水利枢纽，目前是首都北京最重要的地表饮用水\n" +
                "源地。 \n" +
                "密云水库由潮白河上游潮河、白河两大支流汇流而成。水库控制流域面积\n" +
                "15788 平方公里，占潮白河总流域面积 18000 平方公里的 88%。其中，潮河发\n" +
                "源于河北省承德市丰宁县，水库以上流域面积 6716 平方公里，白河发源于河北\n" +
                "省张家口市沽源县，水库以上流域面积 9072 平方公里。两河在密云区城南河槽\n" +
                "村汇合而成潮白河，在天津北塘与永定新河汇流入渤海。\n" +
                "水库工程按千年一遇洪水设计，万年一遇洪水校核，坝顶高程 160.0 米，\n" +
                "设计水位 157.5 米，校核水位 158.5 米，正常蓄水位 157.5 米，汛期限制水位\n" +
                "152.0 米，死水位 126.0 米。水库总库容 43.75 亿立方米，其中防洪库容 9.27 亿\n" +
                "立方米，调洪库容 11.08 亿立方米，兴利库容 35.45 亿立方米，死库容 4.19 亿立\n" +
                "方米。\n" +
                "1.1 自然地理、地质和气象情况\n" +
                "水库控制流域地理位置处于东经 115°25'至 117°30'，北纬 40°20'至 41°45'之\n",
                Map.of("source", "example.txt"));
        Document doc2 = new Document("Another document with content that will be split based on token count.",
                Map.of("source", "example2.txt"));

        TokenTextSplitter splitter = new TokenTextSplitter(200, 100, 5, 500, true);
        List<Document> splitDocuments = splitter.apply(List.of(doc1, doc2));*/
        List<Document> documents = getDocsFromPdfWithCatalog();
        for (Document doc : documents) {
            System.out.println("Chunk: " + doc.getText());
            System.out.println("Metadata: " + doc.getMetadata());
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
}
