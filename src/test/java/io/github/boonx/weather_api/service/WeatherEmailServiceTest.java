package io.github.boonx.weather_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import io.github.boonx.weather_api.client.VisualCrossingWeatherApiClient;
import io.github.boonx.weather_api.dto.VisualCrossingWeatherTimelineResponse;
import io.github.boonx.weather_api.dto.VisualCrossingWeatherTimelineResponse.Weather;
import io.github.boonx.weather_api.entity.Location;
import io.github.boonx.weather_api.entity.Subscription;
import io.github.boonx.weather_api.entity.User;
import io.github.boonx.weather_api.exception.HttpStatusException;
import io.github.boonx.weather_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class WeatherEmailServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private VisualCrossingWeatherApiClient visualCrossingWeatherApiClient;

  @Mock
  private JavaMailSender mailSender;

  private WeatherEmailService weatherEmailService;

  @BeforeEach
  void setUp() {
    weatherEmailService = new WeatherEmailService(
        userRepository, visualCrossingWeatherApiClient, mailSender, "digest@example.com");
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

  private static final LocalDate MONDAY = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

  private static final List<String> WEEK_DAY_NAMES = List.of(
      "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

  private VisualCrossingWeatherTimelineResponse timelineFor(String address) {
    List<Weather> days = IntStream.range(0, 7)
        .mapToObj(i -> new Weather(
            MONDAY.plusDays(i).toString(),
            8.0f + i, 14.0f + i, 6.0f + i, 12.0f + i, i * 0.5f))
        .toList();
    return new VisualCrossingWeatherTimelineResponse(address, days);
  }

  @Test
  void sendsEmailWithWeatherForEachSubscribedLocation() {
    User user = userWithSubscriptions("alice@example.com", "London", "Tokyo");
    when(userRepository.findAllWithSubscriptions()).thenReturn(List.of(user));
    when(visualCrossingWeatherApiClient.getWeatherTimeline(eq("London"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(timelineFor("London"));
    when(visualCrossingWeatherApiClient.getWeatherTimeline(eq("Tokyo"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(timelineFor("Tokyo"));

    weatherEmailService.sendWeatherDigest();

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    SimpleMailMessage sent = captor.getValue();
    assertThat(sent.getTo()).containsExactly("alice@example.com");
    assertThat(sent.getFrom()).isEqualTo("digest@example.com");
    assertThat(sent.getSubject()).startsWith("Weather digest for");
    assertThat(sent.getText()).contains("London:");
    assertThat(sent.getText()).contains("Tokyo:");
    assertThat(sent.getText()).contains("Monday: 8.0–14.0°C, precipitation 0.0mm");
    WEEK_DAY_NAMES.forEach(day -> assertThat(sent.getText()).contains(day));
  }

  @Test
  void whenOneLocationFails_skipsItAndStillSendsEmail() {
    User user = userWithSubscriptions("alice@example.com", "London", "BadLocation");
    when(userRepository.findAllWithSubscriptions()).thenReturn(List.of(user));
    when(visualCrossingWeatherApiClient.getWeatherTimeline(eq("London"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(timelineFor("London"));
    when(visualCrossingWeatherApiClient.getWeatherTimeline(eq("BadLocation"), any(LocalDate.class),
        any(LocalDate.class)))
        .thenThrow(new HttpStatusException("Not found", HttpStatus.NOT_FOUND));

    weatherEmailService.sendWeatherDigest();

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    assertThat(captor.getValue().getText()).contains("London:");
    assertThat(captor.getValue().getText()).doesNotContain("BadLocation");
  }

  @Test
  void whenAllLocationsFail_doesNotSendEmail() {
    User user = userWithSubscriptions("alice@example.com", "BadLocation");
    when(userRepository.findAllWithSubscriptions()).thenReturn(List.of(user));
    when(visualCrossingWeatherApiClient.getWeatherTimeline(eq("BadLocation"), any(LocalDate.class),
        any(LocalDate.class)))
        .thenThrow(new HttpStatusException("Not found", HttpStatus.NOT_FOUND));

    weatherEmailService.sendWeatherDigest();

    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }

  @Test
  void whenMailSenderThrows_continuesWithRemainingUsers() {
    User alice = userWithSubscriptions("alice@example.com", "London");
    User bob = userWithSubscriptions("bob@example.com", "Tokyo");
    when(userRepository.findAllWithSubscriptions()).thenReturn(List.of(alice, bob));
    when(visualCrossingWeatherApiClient.getWeatherTimeline(eq("London"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(timelineFor("London"));
    when(visualCrossingWeatherApiClient.getWeatherTimeline(eq("Tokyo"), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(timelineFor("Tokyo"));
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
