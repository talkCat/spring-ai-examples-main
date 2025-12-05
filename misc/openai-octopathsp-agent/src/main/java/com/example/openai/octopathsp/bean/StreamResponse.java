package com.example.openai.octopathsp.bean;

import lombok.Data;

/**
 * @author n039920
 * @date 2025/11/4
 * @description TODO
 */
@Data
public class StreamResponse {
    private String content;
    private boolean completed;
    private String messageId;

    public StreamResponse() {
    }

    public StreamResponse(String content) {
        this.content = content;
    }
}
