package com.example.openai.octopathsp.utils;


import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class DirectoryCleaner {

    /**
     * 清理字符串中的目录编号
     * @param input 原始字符串
     * @return 处理后的字符串
     */
    public static String cleanDirectoryNumbers(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 按行分割字符串
        String[] lines = input.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 处理每行中的目录编号
            // 正则表达式匹配：数字+点+空格的组合，可能出现在字符串开头或连字符后
            String cleanedLine = line;

            // 方法1: 使用正则表达式替换所有目录编号
            // 匹配模式：以数字开头，后跟0个或多个".数字"，然后可能跟一个空格
            Pattern pattern = Pattern.compile("\\b\\d+(?:\\.\\d+)*\\s*");
            Matcher matcher = pattern.matcher(cleanedLine);

            // 替换所有匹配的目录编号
            cleanedLine = matcher.replaceAll("");

            // 方法2: 另一种更精确的匹配方式（处理连字符后的目录编号）
            // 匹配：数字+点组合，后面可能跟连字符或空格
            cleanedLine = cleanedLine.replaceAll("(?:^|[-\\s])\\d+(?:\\.\\d+)*[-\\s]*", "");

            // 方法3: 专门处理"数字-数字"这种模式（如"1-1"）
            cleanedLine = cleanedLine.replaceAll("\\b\\d+\\s*-\\s*", "");

            // 如果处理后的行以连字符开头，去掉开头的连字符
            cleanedLine = cleanedLine.replaceAll("^[-\\s]+", "");

            // 如果处理后的行有多个连续的连字符，替换为单个连字符
            cleanedLine = cleanedLine.replaceAll("-{2,}", "-");

            // 如果处理后的行开头或结尾有多余的连字符，去掉它们
            cleanedLine = cleanedLine.replaceAll("^-|-$", "");

            // 添加处理后的行到结果
            result.append(cleanedLine);

            // 如果不是最后一行，添加换行符
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 更简洁的版本 - 只处理目录编号
     * @param input 原始字符串
     * @return 处理后的字符串
     */
    public static String cleanDirectoryNumbersSimple(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 按行处理
        String[] lines = input.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 主要逻辑：删除所有数字+点的组合，以及它们后面的空格
            // 匹配任何以数字开头，后跟0个或多个".数字"的模式
            String cleanedLine = line.replaceAll("\\b\\d+(?:\\.\\d+)*\\s*", "");

            // 清理可能出现的多余连字符
            cleanedLine = cleanedLine.replaceAll("^-+", ""); // 去掉开头的连字符
            cleanedLine = cleanedLine.replaceAll("-+", "-"); // 将多个连字符合并为一个

            result.append(cleanedLine);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 更精确的版本 - 针对示例中的格式优化
     * @param input 原始字符串
     * @return 处理后的字符串
     */
    public static String cleanDirectoryNumbersPrecise(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] lines = input.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 处理三种情况：
            // 1. 行开头的目录编号（如 "1 "）
            // 2. 连字符后的目录编号（如 "-1.1"）
            // 3. 空格后的目录编号
            String cleanedLine = line
                    .replaceAll("^\\d+(?:\\.\\d+)*\\s+", "") // 行开头的目录编号
                    .replaceAll("(-|\\s)\\d+(?:\\.\\d+)*", "$1") // 连字符或空格后的目录编号
                    .replaceAll("^-\\s*", ""); // 清理开头的连字符

            result.append(cleanedLine);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    // 测试方法
    public static void main(String[] args) {
        String testInput =
                """
                        提供一个java方法，将字符串中的目录如掉：
                        1 角色-1.1巴尔杰罗-1.1.2被动技能-1.1.2.2破防中时BP回复
                        1.1.2.2破防中时BP回复
                        存在处于破防状态的敌人时
                        【BP加成效果】	威力90/110/150
                        """;

        System.out.println("原始输入:");
        System.out.println(testInput);
        System.out.println("\n处理后:");
        System.out.println(cleanDirectoryNumbersPrecise(testInput));

        // 测试其他方法
        System.out.println("\n使用简单版本:");
        System.out.println(cleanDirectoryNumbersSimple(testInput));
    }
}