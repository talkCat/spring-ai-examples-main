package com.example.openai.octopathsp.bean;

import lombok.Data;

/**
 * @author n039920
 * @date 2025/11/8
 * @description TODO
 */
@Data
public class BookmarkInfo {
    private String title;
    private int level;
    private int pageNumber;
    private String content;
    private String path; // 新增：层级路径

    public BookmarkInfo(String title, int level, int pageNumber, String content, String path) {
        this.title = title;
        this.level = level;
        this.pageNumber = pageNumber;
        this.content = content;
        this.path = path;
    }
}
