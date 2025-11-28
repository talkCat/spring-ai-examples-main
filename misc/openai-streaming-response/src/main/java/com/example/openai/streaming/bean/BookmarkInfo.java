package com.example.openai.streaming.bean;

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

    public BookmarkInfo(String title, int level, int pageNumber, String content) {
        this.title = title;
        this.level = level;
        this.pageNumber = pageNumber;
        this.content = content;
    }
}
