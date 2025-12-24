package com.example.openai.octopathsp.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CurrentWeather(
        @JsonProperty("temperature") double temperature,
        @JsonProperty("feels_like") double feelsLike,
        @JsonProperty("condition") String condition,
        @JsonProperty("humidity") int humidity,
        @JsonProperty("wind_speed") double windSpeed,
        @JsonProperty("wind_direction") String windDirection,
        @JsonProperty("pressure") int pressure,
        @JsonProperty("visibility") double visibility
) {
}
