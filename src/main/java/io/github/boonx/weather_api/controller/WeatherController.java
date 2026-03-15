package io.github.boonx.weather_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.boonx.weather_api.dto.WeatherResponse;
import io.github.boonx.weather_api.service.WeatherService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

  private final WeatherService weatherService;

  @GetMapping("/{location}/current")
  public WeatherResponse getCurrentWeather(@PathVariable String location) {
    return weatherService.getCurrentWeather(location);
  }
}
