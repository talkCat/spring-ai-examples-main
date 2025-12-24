package com.example.openai.octopathsp.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherResponse(
        @JsonProperty("city") String city,
        @JsonProperty("country") String country,
        @JsonProperty("current") CurrentWeather current,
        @JsonProperty("timestamp") String timestamp
) {
}
