package io.github.boonx.weather_api.dto;

public record WeatherResponse(float temp, float feelslike) {

  public static WeatherResponse from(VisualCrossingWeatherResponse visualCrossingWeatherResponse) {
    return new WeatherResponse(visualCrossingWeatherResponse.currentConditions().temp(),
        visualCrossingWeatherResponse.currentConditions().feelslike());
  }
}
