package com.example.openai.octopathsp.tools;

import com.example.openai.octopathsp.bean.CurrentWeather;
import com.example.openai.octopathsp.bean.WeatherResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author n039920
 * @date 2025/12/22
 * @description TODO
 */
public class CityWeatherTools {

    //根据城市返回固定的天气数据
    @Tool(description = "根据城市返回固定的天气数据")
    public String getWeather(String city) {
        try {
        // 根据城市返回固定的天气数据
        WeatherResponse response;
        switch (city) {
            case "北京":
                response = new WeatherResponse(
                        "北京",
                        "中国",
                        new CurrentWeather(15.5, 16.0, "晴朗", 45, 12.3, "东北", 1015, 10.0),
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
                break;
            case "上海":
                response = new WeatherResponse(
                        "上海",
                        "中国",
                        new CurrentWeather(18.0, 19.5, "多云", 65, 8.5, "东南", 1012, 8.0),
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
                break;
            case "广州":
                response = new WeatherResponse(
                        "广州",
                        "中国",
                        new CurrentWeather(25.0, 26.5, "晴天", 70, 10.2, "南", 1008, 12.0),
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
                break;
            default:
                throw new IllegalArgumentException("暂不支持该城市: " + city);
        }
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(response);
        return json;
        } catch (Exception e) {
            return "获取天气数据失败";
        }
    }
}
