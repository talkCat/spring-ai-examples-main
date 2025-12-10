package com.example.openai.octopathsp.utils;

import com.google.gson.*;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HtmlTableToJsonWithChineseKeys {

    public static void main(String[] args) {
        try {
            // 读取HTML文件内容
            String htmlContent = readHtmlFile("D:\\project\\demo\\spring-ai-examples-main\\misc\\openai-octopathsp-agent\\src\\main\\resources\\10.html"); // 替换为你的HTML文件路径

            // 解析HTML并转换为JSON
            String jsonResult = parseHtmlTableToJson(htmlContent);

            // 输出JSON结果
            System.out.println(jsonResult);

            // 也可以保存到文件
            saveJsonToFile(jsonResult, "output.json");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析HTML表格并转换为JSON字符串
     */
    public static String parseHtmlTableToJson(String htmlContent) {
        // 使用Jsoup解析HTML
        Document doc = Jsoup.parse(htmlContent);

        // 获取表格的thead部分，提取列名
        List<String> columnNames = new ArrayList<>();
        Elements headerCells = doc.select("thead th");
        for (Element headerCell : headerCells) {
            // 提取表头文本，去除空白字符
            String headerText = headerCell.text().trim();
            if (!headerText.isEmpty()) {
                columnNames.add(headerText);
            }
        }

        // 获取表格的tbody部分，提取数据行
        Elements rows = doc.select("tbody tr");
        JsonArray jsonArray = new JsonArray();

        for (Element row : rows) {
            JsonObject jsonObject = new JsonObject();
            Elements cells = row.select("td");

            // 为每个单元格添加数据
            for (int i = 0; i < Math.min(cells.size(), columnNames.size()); i++) {
                Element cell = cells.get(i);
                String columnName = columnNames.get(i);
                String cellValue;

                // 特殊处理第一列（图标名字）和最后一列（入手方式）
                if (i == 0) { // 第一列：图标名字
                    // 提取装备名称
                    Element nameLink = cell.select("a").get(1); // 第二个链接是装备名称
                    String equipmentName = nameLink.text().trim();
                    jsonObject.addProperty(columnName, equipmentName);

                    // 也可以提取图标URL
                    Element iconImg = cell.select("img").first();
                    if (iconImg != null) {
                        String iconUrl = iconImg.attr("src");
                        jsonObject.addProperty("图标URL", iconUrl);
                    }

                    // 提取链接
                    String link = nameLink.attr("href");
                    jsonObject.addProperty("链接", link);

                    continue;

                } else if (i == columnNames.size() - 1) { // 最后一列：入手方式
                    // 提取图片信息
                    Element img = cell.select("img").first();
                    if (img != null) {
                        String imgAlt = img.attr("alt");
                        // 去除"icon-"前缀和".png"后缀
                        String obtainMethod = imgAlt.replace("icon-", "").replace(".png", "");
                        jsonObject.addProperty(columnName, obtainMethod);

                        String imgUrl = img.attr("src");
                        jsonObject.addProperty("入手方式图标URL", imgUrl);
                    }
                    continue;
                }

                // 普通列的处理
                cellValue = cell.text().trim();

                // 尝试将数字字符串转换为整数（如果可能）
                try {
                    // 检查是否是数字（可能包含负号）
                    if (cellValue.matches("-?\\d+")) {
                        jsonObject.addProperty(columnName, Integer.parseInt(cellValue));
                    } else {
                        jsonObject.addProperty(columnName, cellValue);
                    }
                } catch (NumberFormatException e) {
                    jsonObject.addProperty(columnName, cellValue);
                }
            }

            // 添加tr元素的data属性（如果存在）
            String zbzlx = row.attr("data-paramzbzlx");
            if (!zbzlx.isEmpty()) {
                jsonObject.addProperty("装备子类型", zbzlx);
            }

            String zblx = row.attr("data-paramzblx");
            if (!zblx.isEmpty()) {
                jsonObject.addProperty("装备类型", zblx);
            }

            String rsff = row.attr("data-paramrsff");
            if (!rsff.isEmpty()) {
                jsonObject.addProperty("入手方法分类", rsff);
            }

            jsonArray.add(jsonObject);
        }

        // 创建最终的JSON对象
        JsonObject result = new JsonObject();
        result.addProperty("数据来源", "HTML表格解析");
        result.addProperty("总记录数", jsonArray.size());
        result.add("装备列表", jsonArray);

        //将jsonArray 调整为：  {
        //    "饰品名称": "商人猫厚厚的账簿",
        //    "饰品等级": "Lv. 1 ",
        //    "入手方法": "将角色 利克四觉",
        //    "饰品描述": "消费BP3并使用能力时\n自身HP恢复（恢复量最大HP的10%），\nSP恢复（恢复量最大SP的10%）\n战斗开始时:恢复自己的BP 5\n物攻：50  |  属攻：50  |  SP：40"
        //  }
        List<JsonElement> jsonArrayList02 = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject02 = new JsonObject();
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            jsonArray.set(i, jsonObject);
            jsonObject02.addProperty("饰品名称", jsonObject.get("图标名字").getAsString());
            jsonObject02.addProperty("饰品等级", "Lv. 1 ");
            jsonObject02.addProperty("入手方法", jsonObject.get("入手方式").getAsString());
            jsonObject02.addProperty("饰品描述", jsonObject.get("装备特性").getAsString());
            StringBuilder stringBuilder = new StringBuilder();
            if( null != jsonObject.get("物攻") && !jsonObject.get("物攻").getAsString().equals("0")){
                stringBuilder.append("物攻：").append(jsonObject.get("物攻").getAsString()).append("  |  ");
            }
            if( null != jsonObject.get("属攻") && !jsonObject.get("属攻").getAsString().equals("0")){
                stringBuilder.append("属攻：").append(jsonObject.get("属攻").getAsString()).append("  |  ");
            }
            if( null != jsonObject.get("物防") && !jsonObject.get("物防").getAsString().equals("0")){
                stringBuilder.append("物防：").append(jsonObject.get("物防").getAsString()).append("  |  ");
            }
            if( null != jsonObject.get("属防") && !jsonObject.get("属防").getAsString().equals("0")){
                stringBuilder.append("属防：").append(jsonObject.get("属防").getAsString()).append("  |  ");
            }
            if( null != jsonObject.get("暴击") && !jsonObject.get("暴击").getAsString().equals("0")){
                stringBuilder.append("暴击：").append(jsonObject.get("暴击").getAsString()).append("  |  ");
            }
            if( null != jsonObject.get("速度") && !jsonObject.get("速度").getAsString().equals("0")){
                stringBuilder.append("速度：").append(jsonObject.get("速度").getAsString()).append("  |  ");
            }
            if( null != jsonObject.get("HP") && !jsonObject.get("HP").getAsString().equals("0")){
                stringBuilder.append("HP：").append(jsonObject.get("HP").getAsString()).append("  |  ");
            }
            if( null != jsonObject.get("SP") && !jsonObject.get("SP").getAsString().equals("0")){
                stringBuilder.append("SP：").append(jsonObject.get("SP").getAsString()).append("  |  ");
            }
            jsonObject02.addProperty("饰品面板",  StringUtil.isBlank(stringBuilder.toString()) ? "无" : stringBuilder.toString());
            jsonArrayList02.add(jsonObject02);
        }

        // 使用Gson格式化JSON输出
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(jsonArrayList02);
    }

    /**
     * 读取HTML文件内容
     */
    private static String readHtmlFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * 将JSON保存到文件
     */
    private static void saveJsonToFile(String jsonContent, String outputPath) throws IOException {
        Files.write(Paths.get(outputPath), jsonContent.getBytes());
        System.out.println("JSON已保存到: " + outputPath);
    }

    /**
     * 如果不想使用Gson，可以使用Java原生方式创建JSON字符串
     */
    public static String parseHtmlTableToJsonSimple(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        Elements rows = doc.select("tbody tr");

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[\n");

        boolean firstRow = true;
        for (Element row : rows) {
            if (!firstRow) {
                jsonBuilder.append(",\n");
            }
            firstRow = false;

            Elements cells = row.select("td");

            jsonBuilder.append("  {\n");

            // 解析每个单元格
            List<String> properties = new ArrayList<>();

            // 第一列：装备名称
            if (cells.size() > 0) {
                Element firstCell = cells.get(0);
                Element nameLink = firstCell.select("a").get(1);
                String name = nameLink.text().trim().replace("\"", "\\\"");
                properties.add(String.format("    \"图标名字\": \"%s\"", name));
            }

            // 其他列
            String[] columnKeys = {"类型", "物攻", "属攻", "物防", "属防", "暴击", "速度", "HP", "SP", "装备特性", "入手方式"};

            for (int i = 1; i < cells.size() && i - 1 < columnKeys.length; i++) {
                Element cell = cells.get(i);
                String value = cell.text().trim().replace("\"", "\\\"");

                // 特殊处理最后一列
                if (i == cells.size() - 1 && i == columnKeys.length) {
                    Element img = cell.select("img").first();
                    if (img != null) {
                        String imgAlt = img.attr("alt");
                        value = imgAlt.replace("icon-", "").replace(".png", "");
                    }
                }

                properties.add(String.format("    \"%s\": \"%s\"", columnKeys[i-1], value));
            }

            jsonBuilder.append(String.join(",\n", properties));
            jsonBuilder.append("\n  }");
        }

        jsonBuilder.append("\n]");
        return jsonBuilder.toString();
    }
}