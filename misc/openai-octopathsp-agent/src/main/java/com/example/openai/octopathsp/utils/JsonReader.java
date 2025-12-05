package com.example.openai.octopathsp.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class JsonReader {

    public static String readJsonFileAsString(String fileName) {
        // 1. 获取类加载器
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // 2. 获取资源文件的输入流
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // 3. 检查文件是否存在
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found in resources: " + fileName);
        }

        // 4. 将输入流转换为字符串
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
