package io.github.boonx.weather_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import io.github.boonx.weather_api.dto.WeatherResponse;
import io.github.boonx.weather_api.entity.Location;
import io.github.boonx.weather_api.entity.Subscription;
import io.github.boonx.weather_api.entity.User;
import io.github.boonx.weather_api.exception.HttpStatusException;
import io.github.boonx.weather_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class WeatherEmailServiceTest {

  // TODO: Use test container for testing the query?
  @Mock
  private UserRepository userRepository;

  @Mock
  private WeatherService weatherService;

  @Mock
  private JavaMailSender mailSender;

  private WeatherEmailService weatherEmailService;

  @BeforeEach
  void setUp() {
    weatherEmailService = new WeatherEmailService(
        userRepository, weatherService, mailSender, "digest@example.com");
  }

  private User userWithSubscriptions(String email, String... locationNames) {
    User user = new User();
    user.setEmail(email);
    for (String name : locationNames) {
      Location location = new Location();
      location.setName(name);
      Subscription sub = new Subscription();
      sub.setUser(user);
      sub.setLocation(location);
      user.getSubscriptions().add(sub);
    }
    return user;
  }

  @Test
  void sendsEmailWithWeatherForEachSubscribedLocation() {
    User user = userWithSubscriptions("alice@example.com", "London", "Tokyo");
    when(userRepository.findAllWithSubscriptions()).thenReturn(List.of(user));
    when(weatherService.getCurrentWeather("London"))
        .thenReturn(new WeatherResponse("London", 12.3f, 10.1f));
    when(weatherService.getCurrentWeather("Tokyo"))
        .thenReturn(new WeatherResponse("Tokyo", 18.7f, 17.9f));

    weatherEmailService.sendWeatherDigest();

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    SimpleMailMessage sent = captor.getValue();
    assertThat(sent.getTo()).containsExactly("alice@example.com");
    assertThat(sent.getFrom()).isEqualTo("digest@example.com");
    assertThat(sent.getSubject()).startsWith("Weather digest for");
    assertThat(sent.getText()).contains("London: 12.3°C, feels like 10.1°C");
    assertThat(sent.getText()).contains("Tokyo: 18.7°C, feels like 17.9°C");
  }

  @Test
  void whenOneLocationFails_skipsItAndStillSendsEmail() {
    User user = userWithSubscriptions("alice@example.com", "London", "BadLocation");
    when(userRepository.findAllWithSubscriptions()).thenReturn(List.of(user));
    when(weatherService.getCurrentWeather("London"))
        .thenReturn(new WeatherResponse("London", 12.3f, 10.1f));
    when(weatherService.getCurrentWeather("BadLocation"))
        .thenThrow(new HttpStatusException("Not found", HttpStatus.NOT_FOUND));

    weatherEmailService.sendWeatherDigest();

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    assertThat(captor.getValue().getText()).contains("London: 12.3°C");
    assertThat(captor.getValue().getText()).doesNotContain("BadLocation");
  }

  @Test
  void whenAllLocationsFail_doesNotSendEmail() {
    User user = userWithSubscriptions("alice@example.com", "BadLocation");
    when(userRepository.findAllWithSubscriptions()).thenReturn(List.of(user));
    when(weatherService.getCurrentWeather("BadLocation"))
        .thenThrow(new HttpStatusException("Not found", HttpStatus.NOT_FOUND));

    weatherEmailService.sendWeatherDigest();

    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }

  @Test
  void whenMailSenderThrows_continuesWithRemainingUsers() {
    User alice = userWithSubscriptions("alice@example.com", "London");
    User bob = userWithSubscriptions("bob@example.com", "Tokyo");
    when(userRepository.findAllWithSubscriptions()).thenReturn(List.of(alice, bob));
    when(weatherService.getCurrentWeather("London"))
        .thenReturn(new WeatherResponse("London", 12.3f, 10.1f));
    when(weatherService.getCurrentWeather("Tokyo"))
        .thenReturn(new WeatherResponse("Tokyo", 18.7f, 17.9f));
    doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

    assertThatNoException().isThrownBy(() -> weatherEmailService.sendWeatherDigest());

    verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
  }

  @Test
  void whenNoUsersWithSubscriptions_doesNothing() {
    when(userRepository.findAllWithSubscriptions()).thenReturn(List.of());

    weatherEmailService.sendWeatherDigest();

    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }
}
