package io.github.boonx.weather_api.dto;

public record VisualCrossingWeatherResponse(CurrentConditions currentConditions) {

  record CurrentConditions(float temp, float feelslike) {
  }
}
