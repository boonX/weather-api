package io.github.boonx.weather_api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
  public ResponseEntity<WeatherResponse> getCurrentWeather(@PathVariable String location) {
    return ResponseEntity.ok(weatherService.getCurrentWeather(location));
  }

  @PostMapping("/{location}/subscribe")
  public ResponseEntity<Void> subscribeToLocation(@PathVariable String location, Authentication authentication) {
    weatherService.subscribeToLocation(UUID.fromString(authentication.getName()), location);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/{location}/subscribe")
  public ResponseEntity<Void> deleteSubscription(@PathVariable String location, Authentication authentication) {
    weatherService.deleteSubscription(UUID.fromString(authentication.getName()), location);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @GetMapping("/locations/me")
  public ResponseEntity<List<String>> getSubscribedLocations(Authentication authentication) {
    return ResponseEntity.ok(weatherService.getSubscribedLocations(UUID.fromString(authentication.getName())));
  }

  @GetMapping("/locations/current")
  public ResponseEntity<List<WeatherResponse>> getSubscribedCurrentWeather(Authentication authentication) {
    return ResponseEntity.ok(weatherService.getSubscribedCurrentWeather(UUID.fromString(authentication.getName())));
  }
}
