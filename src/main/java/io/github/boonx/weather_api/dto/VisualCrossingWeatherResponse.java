package io.github.boonx.weather_api.dto;

public record VisualCrossingWeatherResponse(CurrentConditions currentConditions) {

  public record CurrentConditions(float temp, float feelslike) {
  }
}
