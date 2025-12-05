package com.example.openai.octopathsp.bean;

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
    private int pageNumber;
    private String path; // 新增：完整层级路径
    private BookmarkWithLevel parent; // 新增：父节点引用

    public BookmarkWithLevel(PDOutlineItem bookmark, int level, int pageNumber, String path, BookmarkWithLevel parent) {
        this.bookmark = bookmark;
        this.level = level;
        this.pageNumber = pageNumber;
        this.path = path;
        this.parent = parent;
    }
}
