package io.github.boonx.weather_api.dto;

public record WeatherResponse(String location, float temp, float feelslike) {

  public static WeatherResponse from(String location, VisualCrossingWeatherResponse visualCrossingWeatherResponse) {
    return new WeatherResponse(location, visualCrossingWeatherResponse.currentConditions().temp(),
        visualCrossingWeatherResponse.currentConditions().feelslike());
  }
}
