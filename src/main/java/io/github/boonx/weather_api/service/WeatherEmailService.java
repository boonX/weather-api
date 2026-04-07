package io.github.boonx.weather_api.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.github.boonx.weather_api.client.VisualCrossingWeatherApiClient;
import io.github.boonx.weather_api.dto.VisualCrossingWeatherTimelineResponse;
import io.github.boonx.weather_api.dto.VisualCrossingWeatherTimelineResponse.Weather;
import io.github.boonx.weather_api.entity.Subscription;
import io.github.boonx.weather_api.entity.User;
import io.github.boonx.weather_api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WeatherEmailService {

  private final UserRepository userRepository;
  private final VisualCrossingWeatherApiClient visualCrossingWeatherApiClient;
  private final JavaMailSender mailSender;
  private final String from;

  public WeatherEmailService(
      UserRepository userRepository,
      VisualCrossingWeatherApiClient visualCrossingWeatherApiClient,
      JavaMailSender mailSender,
      @Value("${mail.weather-digest.from}") String from) {
    this.userRepository = userRepository;
    this.visualCrossingWeatherApiClient = visualCrossingWeatherApiClient;
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
    LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate sunday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    List<VisualCrossingWeatherTimelineResponse> weatherResponses = getWeatherResponses(user, monday, sunday);
    if (weatherResponses.isEmpty()) {
      log.warn("Skipping email for '{}' — no weather data could be fetched", user.getEmail());
      return;
    }

    DateTimeFormatter dayMonth = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH);
    String weekStr = monday.format(dayMonth) + " – " + sunday.format(dayMonth) + " " + sunday.getYear();
    String body = buildBody(weatherResponses, weekStr);

    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(user.getEmail());
      message.setSubject("Weather digest for " + weekStr);
      message.setText(body);
      mailSender.send(message);
    } catch (Exception e) {
      log.error("Failed to send weather digest to '{}': {}", user.getEmail(), e.getMessage());
    }
  }

  private String buildBody(List<VisualCrossingWeatherTimelineResponse> weatherResponses, String weekStr) {
    StringBuilder body = new StringBuilder("Your weather digest for ")
        .append(weekStr).append(":\n\n");

    for (VisualCrossingWeatherTimelineResponse weatherResponse : weatherResponses) {
      body.append(weatherResponse.address()).append(":\n");
      for (Weather dayWeather : weatherResponse.days()) {
        String weatherReport = dayWeather.toWeatherReport();
        body.append("  ");
        body.append(weatherReport);
        body.append("\n");
      }
      body.append("\n");
    }
    return body.toString();
  }

  private List<VisualCrossingWeatherTimelineResponse> getWeatherResponses(User user, LocalDate start, LocalDate end) {
    return user.getSubscriptions().stream()
        .map(subscription -> getWeatherTimeline(user, subscription, start, end))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private Optional<VisualCrossingWeatherTimelineResponse> getWeatherTimeline(User user, Subscription subscription,
      LocalDate start, LocalDate end) {
    String location = subscription.getLocation().getName();
    try {
      return Optional.of(visualCrossingWeatherApiClient.getWeatherTimeline(location, start, end));
    } catch (Exception e) {
      log.warn("Failed to fetch weather for '{}' for user '{}': {}",
          location, user.getEmail(), e.getMessage());
      return Optional.empty();
    }
  }
}
