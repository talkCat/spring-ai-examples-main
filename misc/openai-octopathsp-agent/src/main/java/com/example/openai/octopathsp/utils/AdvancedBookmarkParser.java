package com.example.openai.octopathsp.utils;

import com.example.openai.octopathsp.bean.BookmarkInfo;
import com.example.openai.octopathsp.bean.BookmarkWithLevel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedBookmarkParser {

    /**
     * 解析PDF文件，返回统一的Document列表
     */
    public static List<Document> parsePdfToDocuments(String filePath) {
        File pdfFile = new File(filePath);
        List<Document> documents = new ArrayList<>();

        try (PDDocument pdfDocument = Loader.loadPDF(pdfFile)) {

            PDDocumentOutline outline = pdfDocument.getDocumentCatalog().getDocumentOutline();

            if (outline != null && hasValidBookmarks(outline)) {
                System.out.println("检测到目录结构，按目录层级解析...");
                List<BookmarkInfo> bookmarks = extractBookmarksWithRobustContent(pdfDocument, outline);
                documents = convertBookmarksToDocuments(bookmarks, pdfFile.getName());
            } else {
                System.out.println("未检测到目录结构，按页解析...");
                documents = parseByPages(pdfDocument, pdfFile.getName());
            }

        } catch (IOException e) {
            System.err.println("解析PDF文件时出错: " + e.getMessage());
            e.printStackTrace();
        }

        return documents;
    }

    private static boolean hasValidBookmarks(PDDocumentOutline outline) {
        if (outline == null) return false;
        PDOutlineItem firstChild = outline.getFirstChild();
        if (firstChild == null) return false;
        return firstChild.getTitle() != null && !firstChild.getTitle().trim().isEmpty();
    }

    /**
     * 提取书签及其内容（使用更健壮的匹配逻辑）
     */
    private static List<BookmarkInfo> extractBookmarksWithRobustContent(PDDocument document, PDDocumentOutline outline) {
        List<BookmarkInfo> bookmarks = new ArrayList<>();
        List<BookmarkWithLevel> allBookmarks = new ArrayList<>();

        // 收集所有书签
        //collectAllBookmarks(document, outline, 0, allBookmarks);
        collectAllBookmarks(document, outline, 0, null, allBookmarks); // 初始父节点为null

        // 提取整个文档的文本内容，按页面组织
        Map<Integer, String> pageContents = extractAllPageContents(document);

        // 为每个书签提取内容
        for (int i = 0; i < allBookmarks.size(); i++) {
            BookmarkWithLevel current = allBookmarks.get(i);
            String currentTitle = current.getBookmark().getTitle();
            int currentPage = current.getPageNumber();

            // 获取下一个书签
            BookmarkWithLevel nextBookmark = (i + 1 < allBookmarks.size()) ? allBookmarks.get(i + 1) : null;

            // 确定内容结束页面
            int endPage = (nextBookmark != null) ? nextBookmark.getPageNumber()  : document.getNumberOfPages();

            // 提取内容
            String content = extractRobustContent(document, currentTitle, currentPage, endPage,
                    pageContents, allBookmarks, i);

            BookmarkInfo bookmark = new BookmarkInfo(
                    currentTitle,
                    current.getLevel(),
                    currentPage,
                    content,
                    current.getPath()
            );
            bookmarks.add(bookmark);
        }

        return bookmarks;
    }

    /**
     * 健壮的内容提取方法
     */
    private static String extractRobustContent(PDDocument document, String currentTitle,
                                               int startPage, int endPage,
                                               Map<Integer, String> pageContents,
                                               List<BookmarkWithLevel> allBookmarks, int currentIndex) {
        try {
            // 如果开始和结束页面相同，处理单页情况
            if (startPage == endPage) {
                String pageContent = pageContents.get(startPage);
                if (pageContent == null) return "";

                return extractContentFromSinglePage(pageContent, currentTitle, allBookmarks, currentIndex);
            } else {
                // 处理多页情况
                return extractContentFromMultiplePages(document, currentTitle, startPage, endPage, allBookmarks, currentIndex);
            }
        } catch (Exception e) {
            System.err.println("提取内容时出错: " + e.getMessage());
            return "";
        }
    }

    /**
     * 从单页提取内容
     */
    private static String extractContentFromSinglePage(String pageContent, String currentTitle,
                                                       List<BookmarkWithLevel> allBookmarks, int currentIndex) {
        // 获取下一个书签
        BookmarkWithLevel nextBookmark = (currentIndex + 1 < allBookmarks.size()) ? allBookmarks.get(currentIndex + 1) : null;

        // 尝试多种匹配策略
        String content = tryMultipleExtractionStrategies(pageContent, currentTitle, nextBookmark);

        return content;
    }

    /**
     * 从多页提取内容
     */
    private static String extractContentFromMultiplePages(PDDocument document, String currentTitle,
                                                          int startPage, int endPage,
                                                          List<BookmarkWithLevel> allBookmarks, int currentIndex) throws IOException {
        // 提取页面范围内的所有文本
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setStartPage(startPage);
        textStripper.setEndPage(endPage);
        String fullContent = textStripper.getText(document);

        // 获取下一个书签
        BookmarkWithLevel nextBookmark = (currentIndex + 1 < allBookmarks.size()) ? allBookmarks.get(currentIndex + 1) : null;

        // 尝试多种匹配策略
        String content = tryMultipleExtractionStrategies(fullContent, currentTitle, nextBookmark);

        return content;
    }

    /**
     * 尝试多种内容提取策略
     */
    private static String tryMultipleExtractionStrategies(String fullContent, String currentTitle, BookmarkWithLevel nextBookmark) {
        // 策略1: 精确匹配
        String content = extractWithExactMatch(fullContent, currentTitle, nextBookmark);
        if (!content.isEmpty()) {
            return content;
        }

        // 策略2: 宽松匹配（移除空格）
        content = extractWithLooseMatch(fullContent, currentTitle, nextBookmark);
        if (!content.isEmpty()) {
            return content;
        }

        // 策略3: 基于数字模式匹配
        content = extractWithPatternMatch(fullContent, currentTitle, nextBookmark);
        if (!content.isEmpty()) {
            return content;
        }

        // 策略4: 提取页面开始部分作为备选
        return extractFallbackContent(fullContent);
    }

    /**
     * 精确匹配提取
     */
    private static String extractWithExactMatch(String fullContent, String currentTitle, BookmarkWithLevel nextBookmark) {
        String[] lines = fullContent.split("\n");
        StringBuilder content = new StringBuilder();
        boolean foundCurrent = false;

        for (String line : lines) {
            line = line.replaceAll("\\s", "");
            if (line.isEmpty()) continue;

            // 检查是否找到当前书签（精确匹配）
            if (!foundCurrent && line.contains(currentTitle)) {
                foundCurrent = true;
                content.append(line).append("\n");
                continue;
            }

            // 如果已经找到当前书签，开始收集内容
            if (foundCurrent) {
                // 检查是否遇到下一个书签的标题
                if (nextBookmark != null && line.contains(nextBookmark.getBookmark().getTitle().replaceAll("\\s", ""))) {
                    break;
                }

                content.append(line).append("\n");
            }
        }

        return content.toString().trim();
    }

    /**
     * 宽松匹配提取（移除空格）
     */
    private static String extractWithLooseMatch(String fullContent, String currentTitle, BookmarkWithLevel nextBookmark) {
        String[] lines = fullContent.split("\n");
        StringBuilder content = new StringBuilder();
        boolean foundCurrent = false;

        // 移除空格后的标题
        String noSpaceTitle = currentTitle.replaceAll("\\s+", "");

        for (String line : lines) {
            String originalLine = line.trim();
            if (originalLine.isEmpty()) continue;

            // 移除空格后的行
            String noSpaceLine = originalLine.replaceAll("\\s+", "");

            // 检查是否找到当前书签（宽松匹配）
            if (!foundCurrent && noSpaceLine.contains(noSpaceTitle)) {
                foundCurrent = true;
                content.append(originalLine).append("\n");
                continue;
            }

            // 如果已经找到当前书签，开始收集内容
            if (foundCurrent) {
                // 检查是否遇到下一个书签的标题
                if (nextBookmark != null) {
                    String nextNoSpaceTitle = nextBookmark.getBookmark().getTitle().replaceAll("\\s+", "");
                    if (noSpaceLine.contains(nextNoSpaceTitle)) {
                        break;
                    }
                }

                content.append(originalLine).append("\n");
            }
        }

        return content.toString().trim();
    }

    /**
     * 基于模式匹配提取
     */
    private static String extractWithPatternMatch(String fullContent, String currentTitle, BookmarkWithLevel nextBookmark) {
        String[] lines = fullContent.split("\n");
        StringBuilder content = new StringBuilder();
        boolean foundCurrent = false;

        // 从标题中提取可能的数字模式
        String numberPattern = extractNumberPattern(currentTitle);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 检查是否找到当前书签（模式匹配）
            if (!foundCurrent && (line.contains(currentTitle) ||
                    (numberPattern != null && line.contains(numberPattern)))) {
                foundCurrent = true;
                content.append(line).append("\n");
                continue;
            }

            // 如果已经找到当前书签，开始收集内容
            if (foundCurrent) {
                // 检查是否遇到下一个书签的标题
                if (nextBookmark != null) {
                    String nextNumberPattern = extractNumberPattern(nextBookmark.getBookmark().getTitle());
                    if (line.contains(nextBookmark.getBookmark().getTitle()) ||
                            (nextNumberPattern != null && line.contains(nextNumberPattern))) {
                        break;
                    }
                }

                content.append(line).append("\n");
            }
        }

        return content.toString().trim();
    }

    /**
     * 从标题中提取数字模式
     */
    private static String extractNumberPattern(String title) {
        // 匹配类似 "1.1", "1.2.1" 等数字模式
        Pattern pattern = Pattern.compile("\\d+(\\.\\d+)*");
        Matcher matcher = pattern.matcher(title);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * 备选内容提取（当精确匹配失败时使用）
     */
    private static String extractFallbackContent(String fullContent) {
        // 返回内容的前几段
        String[] paragraphs = fullContent.split("\n\n");

        StringBuilder content = new StringBuilder();
        int maxParagraphs = Math.min(3, paragraphs.length);
        for (int i = 0; i < maxParagraphs; i++) {
            if (paragraphs[i].trim().length() > 0) {
                content.append(paragraphs[i].trim()).append("\n\n");
            }
        }

        return content.toString().trim();
    }

    /**
     * 提取所有页面的内容
     */
    private static Map<Integer, String> extractAllPageContents(PDDocument document) {
        Map<Integer, String> pageContents = new HashMap<>();
        int totalPages = document.getNumberOfPages();

        try {
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                PDFTextStripper textStripper = new PDFTextStripper();
                textStripper.setStartPage(pageNum);
                textStripper.setEndPage(pageNum);
                String content = textStripper.getText(document).trim();
                pageContents.put(pageNum, content);
            }
        } catch (IOException e) {
            System.err.println("提取页面内容时出错: " + e.getMessage());
        }

        return pageContents;
    }

    /**
     * 递归收集所有书签
     */
    /*private static void collectAllBookmarks(PDDocument document, PDOutlineNode node, int level, List<BookmarkWithLevel> bookmarks) {
        PDOutlineItem current = node.getFirstChild();

        while (current != null) {
            int pageNumber = getPageNumber(current, document);
            if (pageNumber > 0) {
                bookmarks.add(new BookmarkWithLevel(current, level, pageNumber));
            }

            if (current.hasChildren()) {
                collectAllBookmarks(document, current, level + 1, bookmarks);
            }

            current = current.getNextSibling();
        }
    }*/

    /**
     * 递归收集所有书签（带层级路径）
     */
    private static void collectAllBookmarks(PDDocument document, PDOutlineNode node,
                                            int level, BookmarkWithLevel parent,
                                            List<BookmarkWithLevel> bookmarks) {
        PDOutlineItem current = node.getFirstChild();

        while (current != null) {
            int pageNumber = getPageNumber(current, document);
            if (pageNumber > 0) {
                String title = current.getTitle();

                // 构建层级路径
                String path;
                if (parent == null) {
                    path = title; // 根节点
                } else {
                    // 对于非根节点，添加父级路径
                    path = parent.getPath() + "-" + title;
                }

                BookmarkWithLevel bookmarkWithLevel = new BookmarkWithLevel(
                        current, level, pageNumber, path, parent
                );
                bookmarks.add(bookmarkWithLevel);

                if (current.hasChildren()) {
                    collectAllBookmarks(document, current, level + 1, bookmarkWithLevel, bookmarks);
                }
            }

            current = current.getNextSibling();
        }
    }

    /**
     * 获取书签对应的页码
     */
    private static int getPageNumber(PDOutlineItem bookmark, PDDocument document) {
        try {
            PDPage page = bookmark.findDestinationPage(document);
            if (page != null) {
                return document.getPages().indexOf(page) + 1;
            }
        } catch (IOException e) {
            System.err.println("获取书签页码时出错: " + e.getMessage());
        }
        return -1;
    }

    /**
     * 将书签信息转换为Spring AI Document
     */
    /*private static List<Document> convertBookmarksToDocuments(List<BookmarkInfo> bookmarks, String fileName) {
        List<Document> documents = new ArrayList<>();

        for (BookmarkInfo bookmark : bookmarks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", fileName);
            metadata.put("title", bookmark.getTitle());
            metadata.put("level", bookmark.getLevel());
            metadata.put("page", bookmark.getPageNumber());
            metadata.put("type", "bookmark");

            Document doc = new Document(bookmark.getContent(), metadata);
            documents.add(doc);
        }

        return documents;
    }*/

    /**
     * 将书签信息转换为Spring AI Document
     */
    private static List<Document> convertBookmarksToDocuments(List<BookmarkInfo> bookmarks, String fileName) {
        List<Document> documents = new ArrayList<>();

        for (BookmarkInfo bookmark : bookmarks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", fileName);
            metadata.put("title", bookmark.getTitle());
            metadata.put("level", bookmark.getLevel());
            metadata.put("page", bookmark.getPageNumber());
            metadata.put("type", "bookmark");
            metadata.put("path", bookmark.getPath()); // 保存路径到元数据

            // 构建带路径前缀的内容
            String enhancedContent;
            if (bookmark.getPath() != null && !bookmark.getPath().isEmpty()) {
                enhancedContent = bookmark.getPath() + "\n" + bookmark.getContent();
            } else {
                enhancedContent = bookmark.getContent();
            }
            enhancedContent = DirectoryCleaner.cleanDirectoryNumbersPrecise(enhancedContent);
            Document doc = new Document(enhancedContent, metadata);
            documents.add(doc);
        }

        return documents;
    }

    /**
     * 按页解析PDF
     */
    private static List<Document> parseByPages(PDDocument document, String fileName) {
        List<Document> documents = new ArrayList<>();
        int totalPages = document.getNumberOfPages();

        try {
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                PDFTextStripper textStripper = new PDFTextStripper();
                textStripper.setStartPage(pageNum);
                textStripper.setEndPage(pageNum);
                String content = textStripper.getText(document).trim();

                if (content == null || content.trim().isEmpty()) {
                    continue;
                }

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", fileName);
                metadata.put("title", "Page " + pageNum);
                metadata.put("level", 0);
                metadata.put("page", pageNum);
                metadata.put("type", "page");

                Document doc = new Document(content, metadata);
                documents.add(doc);
            }
        } catch (IOException e) {
            System.err.println("按页解析PDF时出错: " + e.getMessage());
        }

        return documents;
    }

    public static void main(String[] args) {
        String pdfPath = "D:\\project\\spring-ai-examples-main\\misc\\openai-streaming-response\\src\\main\\resources\\111.pdf";

        List<Document> documents = parsePdfToDocuments(pdfPath);

        System.out.println("=== 解析结果 ===");
        System.out.println("共生成 " + documents.size() + " 个Document");

        for (int i = 0; i < Math.min(documents.size(), 10); i++) {
            Document doc = documents.get(i);
            System.out.println("\n--- Document " + (i + 1) + " ---");
            System.out.println("标题: " + doc.getMetadata().get("title"));
            System.out.println("层级: " + doc.getMetadata().get("level"));
            System.out.println("页码: " + doc.getMetadata().get("page"));
            System.out.println("类型: " + doc.getMetadata().get("type"));
            String content = doc.getText();
            System.out.println("内容预览: " +
                    (content.length() > 100 ?
                            content.substring(0, 100).replaceAll("\\s+", " ") + "..." :
                            content.replaceAll("\\s+", " ")));
            System.out.println("内容长度: " + content.length());
        }
    }



}