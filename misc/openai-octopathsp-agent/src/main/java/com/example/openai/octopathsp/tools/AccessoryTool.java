package com.example.openai.octopathsp.tools;

import com.example.openai.octopathsp.utils.JsonReader;
import org.springframework.ai.tool.annotation.Tool;

/**
 * @author n039920
 * @date 2025/12/8
 * @description TODO
 */
public class AccessoryTool {


    //获取饰品信息
    @Tool(description = "获取饰品信息")
    String getAccessoryInfo(){
        //获取饰品信息
        return JsonReader.readJsonFileAsString("accessories.json");
    }
}
