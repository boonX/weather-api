package io.github.boonx.weather_api.service;

import org.springframework.stereotype.Service;

import io.github.boonx.weather_api.client.VisualCrossingWeatherApiClient;
import io.github.boonx.weather_api.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeatherService {

  private final VisualCrossingWeatherApiClient visualCrossingWeatherApiClient;

  public WeatherResponse getCurrentWeather(String location) {
    return WeatherResponse.from(visualCrossingWeatherApiClient.getWeather(location));
  }
}
