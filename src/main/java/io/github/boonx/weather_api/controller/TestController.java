package io.github.boonx.weather_api.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.boonx.weather_api.service.WeatherEmailService;
import lombok.RequiredArgsConstructor;

@Profile("local")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

  private final WeatherEmailService weatherEmailService;

  @PostMapping("/email-subscribers")
  public ResponseEntity<Void> emailSubscribers() {
    weatherEmailService.sendWeatherDigest();
    return ResponseEntity.ok().build();
  }
}
