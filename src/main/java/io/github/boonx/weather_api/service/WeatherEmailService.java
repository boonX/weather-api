package io.github.boonx.weather_api.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.github.boonx.weather_api.dto.WeatherResponse;
import io.github.boonx.weather_api.entity.Subscription;
import io.github.boonx.weather_api.entity.User;
import io.github.boonx.weather_api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WeatherEmailService {

  private final UserRepository userRepository;
  private final WeatherService weatherService;
  private final JavaMailSender mailSender;
  private final String from;

  public WeatherEmailService(
      UserRepository userRepository,
      WeatherService weatherService,
      JavaMailSender mailSender,
      @Value("${mail.weather-digest.from}") String from) {
    this.userRepository = userRepository;
    this.weatherService = weatherService;
    this.mailSender = mailSender;
    this.from = from;
  }

  @Scheduled(cron = "${mail.weather-digest.cron}")
  public void sendWeatherDigest() {
    List<User> users = userRepository.findAllWithSubscriptions();
    for (User user : users) {
      sendDigestToUser(user);
    }
  }

  private void sendDigestToUser(User user) {
    List<WeatherResponse> weatherResponses = getWeatherResponses(user);
    if (weatherResponses.isEmpty()) {
      log.warn("Skipping email for '{}' — no weather data could be fetched", user.getEmail());
      return;
    }

    String dateStr = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH));

    StringBuilder body = new StringBuilder("Your weather digest for ")
        .append(dateStr).append(":\n\n");

    weatherResponses.forEach(weatherResponse -> {
      body.append(weatherResponse.location())
          .append(": ").append(weatherResponse.temp())
          .append("°C, feels like ")
          .append(weatherResponse.feelslike())
          .append("°C\n");
    });

    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(user.getEmail());
      message.setSubject("Weather digest for " + dateStr);
      message.setText(body.toString());
      mailSender.send(message);
    } catch (Exception e) {
      log.error("Failed to send weather digest to '{}': {}", user.getEmail(), e.getMessage());
    }
  }

  private List<WeatherResponse> getWeatherResponses(User user) {
    List<WeatherResponse> weatherResponses = user.getSubscriptions().stream()
        .map(subscription -> getCurrentWeather(user, subscription))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
    return weatherResponses;
  }

  private Optional<WeatherResponse> getCurrentWeather(User user, Subscription subscription) {
    String location = subscription.getLocation().getName();
    try {
      return Optional.of(weatherService.getCurrentWeather(location));
    } catch (Exception e) {
      log.warn("Failed to fetch weather for '{}' for user '{}': {}",
          location, user.getEmail(), e.getMessage());
      return Optional.empty();
    }
  }
}
