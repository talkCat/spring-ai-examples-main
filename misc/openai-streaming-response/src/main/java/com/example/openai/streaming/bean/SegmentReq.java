package com.example.openai.streaming.bean;

import lombok.Data;

import java.util.List;

/**
 * @author n039920
 * @date 2025/11/5
 * @description TODO
 */
@Data
public class SegmentReq {

    private String url;
    private String userFileName;
    private String splitStrategy;
    private Integer titleLevel = 0;
    private Integer chunkSize;
    private Integer overlap;
    private String separators;
    private String fileId;
    private String oldFileId;
    private String tenantId;
    private List<String> sharedTenantIdList;
}
