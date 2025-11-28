package com.example.openai.streaming.bean;

import lombok.Data;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/**
 * @author n039920
 * @date 2025/11/8
 * @description TODO
 */
@Data
public class BookmarkWithLevel {
    private PDOutlineItem bookmark;
    private int level;
    private int pageNumber; // 存储页码

    public BookmarkWithLevel(PDOutlineItem bookmark, int level, int pageNumber) {
        this.bookmark = bookmark;
        this.level = level;
        this.pageNumber = pageNumber;
    }
}
