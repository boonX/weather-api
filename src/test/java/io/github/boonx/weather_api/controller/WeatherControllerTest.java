package io.github.boonx.weather_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import io.github.boonx.weather_api.client.VisualCrossingWeatherApiClient;
import io.github.boonx.weather_api.dto.VisualCrossingWeatherResponse;
import io.github.boonx.weather_api.dto.VisualCrossingWeatherResponse.CurrentConditions;
import io.github.boonx.weather_api.entity.Location;
import io.github.boonx.weather_api.entity.Subscription;
import io.github.boonx.weather_api.entity.User;
import io.github.boonx.weather_api.repository.LocationRepository;
import io.github.boonx.weather_api.repository.SubscriptionRepository;
import io.github.boonx.weather_api.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class WeatherControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private LocationRepository locationRepository;
  @Autowired private SubscriptionRepository subscriptionRepository;

  @MockitoBean private VisualCrossingWeatherApiClient visualCrossingWeatherApiClient;
  @MockitoBean private RedisConnectionFactory redisConnectionFactory;

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  // JWT for USER_ID, signed with auth.jwt.secret-key from application-test.yml, expires 2076
  private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDEiLCJpYXQiOjE3NzUzNzcxNDQsImV4cCI6MzM1MjE3NzE0NH0.9gscARdHojJ6xuMjm7RjPWrjiSefT6QlSCyJkablJL8";

  @Test
  void getCurrentWeather_returnsWeatherResponse() throws Exception {
    when(visualCrossingWeatherApiClient.getWeather("London"))
        .thenReturn(new VisualCrossingWeatherResponse(new CurrentConditions(15.0f, 13.0f)));

    mockMvc.perform(get("/api/weather/London/current"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.location").value("London"))
        .andExpect(jsonPath("$.temp").value(15.0))
        .andExpect(jsonPath("$.feelslike").value(13.0));
  }

  @Test
  void subscribeToLocation_returnsCreated() throws Exception {
    when(visualCrossingWeatherApiClient.getWeather("London"))
        .thenReturn(new VisualCrossingWeatherResponse(new CurrentConditions(15.0f, 13.0f)));

    mockMvc.perform(post("/api/weather/London/subscribe")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
        .andExpect(status().isCreated());

    assertThat(subscriptionRepository.count()).isEqualTo(1);
  }

  @Test
  void deleteSubscription_returnsNoContent() throws Exception {
    Location location = new Location();
    location.setName("London");
    locationRepository.save(location);

    User user = userRepository.findById(USER_ID).orElseThrow();
    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setLocation(location);
    subscriptionRepository.save(subscription);

    mockMvc.perform(delete("/api/weather/London/subscribe")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
        .andExpect(status().isNoContent());

    assertThat(subscriptionRepository.count()).isEqualTo(0);
  }

  @Test
  void getSubscribedLocations_returnsLocationNames() throws Exception {
    Location location = new Location();
    location.setName("London");
    locationRepository.save(location);

    User user = userRepository.findById(USER_ID).orElseThrow();
    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setLocation(location);
    subscriptionRepository.save(subscription);

    mockMvc.perform(get("/api/weather/locations/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("London"));
  }

  @Test
  void getSubscribedCurrentWeather_returnsWeatherResponses() throws Exception {
    Location location = new Location();
    location.setName("London");
    locationRepository.save(location);

    User user = userRepository.findById(USER_ID).orElseThrow();
    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setLocation(location);
    subscriptionRepository.save(subscription);

    when(visualCrossingWeatherApiClient.getWeather("London"))
        .thenReturn(new VisualCrossingWeatherResponse(new CurrentConditions(15.0f, 13.0f)));

    mockMvc.perform(get("/api/weather/locations/current")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].location").value("London"))
        .andExpect(jsonPath("$[0].temp").value(15.0))
        .andExpect(jsonPath("$[0].feelslike").value(13.0));
  }
}
