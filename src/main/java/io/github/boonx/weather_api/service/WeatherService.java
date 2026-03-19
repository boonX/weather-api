package io.github.boonx.weather_api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import io.github.boonx.weather_api.client.VisualCrossingWeatherApiClient;
import io.github.boonx.weather_api.dto.WeatherResponse;
import io.github.boonx.weather_api.entity.Location;
import io.github.boonx.weather_api.entity.Subscription;
import io.github.boonx.weather_api.entity.User;
import io.github.boonx.weather_api.exception.HttpStatusException;
import io.github.boonx.weather_api.repository.LocationRepository;
import io.github.boonx.weather_api.repository.SubscriptionRepository;
import io.github.boonx.weather_api.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeatherService {

  private final VisualCrossingWeatherApiClient visualCrossingWeatherApiClient;
  private final UserRepository userRepository;
  private final LocationRepository locationRepository;
  private final SubscriptionRepository subscriptionRepository;

  public WeatherResponse getCurrentWeather(String location) {
    return WeatherResponse.from(visualCrossingWeatherApiClient.getWeather(location));
  }

  public List<WeatherResponse> getSubscribedCurrentWeather(UUID userId) {
    return getSubscribedLocations(userId).stream()
        .map(this::getCurrentWeather)
        .toList();
  }

  @Transactional
  public void subscribeToLocation(UUID userId, String locationName) {
    validateWeatherLocation(locationName);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new HttpStatusException("User not found", HttpStatus.BAD_REQUEST));

    Location location = locationRepository
        .findByName(locationName)
        .orElseGet(() -> {
          Location newLoc = new Location();
          newLoc.setName(locationName);
          return locationRepository.save(newLoc);
        });

    if (subscriptionRepository.existsByUserAndLocation(user, location)) {
      throw new HttpStatusException("User is already subscribed to location", HttpStatus.BAD_REQUEST);
    }

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setLocation(location);

    user.getSubscriptions().add(subscription);
    location.getSubscriptions().add(subscription);

    subscriptionRepository.save(subscription);
  }

  private void validateWeatherLocation(String locationName) {
    try {
      getCurrentWeather(locationName);
    } catch (HttpStatusException e) {
      throw new HttpStatusException(
          "Could not subscribe to weather location because it was not possible to get weather data", e.getStatusCode());
    }
  }

  public List<String> getSubscribedLocations(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new HttpStatusException("User not found", HttpStatus.BAD_REQUEST));
    return user.getSubscriptions().stream()
        .map(Subscription::getLocation)
        .map(Location::getName)
        .toList();
  }

  @Transactional
  public void deleteSubscription(UUID userId, String locationName) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new HttpStatusException("User not found", HttpStatus.BAD_REQUEST));

    Location location = locationRepository
        .findByName(locationName)
        .orElseThrow(() -> new HttpStatusException("Location not found", HttpStatus.BAD_REQUEST));

    Subscription subscription = subscriptionRepository.findByUserAndLocation(user, location)
        .orElseThrow(() -> new HttpStatusException("Subscription not found", HttpStatus.BAD_REQUEST));
    subscriptionRepository.delete(subscription);
  }
}
