package com.example.openai.octopathsp.utils;



import org.springframework.ai.reader.JsonMetadataGenerator;

import java.util.HashMap;
import java.util.Map;

public class AccessoryMetadataGenerator implements JsonMetadataGenerator {
    @Override
    public Map<String, Object> generate(Map<String, Object> jsonMap) {
        // 从每个 JSON 对象中提取“饰品名称”作为元数据
        Map<String, Object> metadata = new HashMap<>();
        Object name = jsonMap.get("饰品名称");
        if (name != null) {
            metadata.put("name", name.toString());
        }
        // 也可以加入文件名等信息
        metadata.put("source", "accessories.json");
        return metadata;
    }
}
