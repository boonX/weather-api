package io.github.boonx.weather_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
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
import io.github.boonx.weather_api.exception.HttpStatusException;
import io.github.boonx.weather_api.repository.LocationRepository;
import io.github.boonx.weather_api.repository.SubscriptionRepository;
import io.github.boonx.weather_api.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class WeatherControllerTest {

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private LocationRepository locationRepository;
  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @MockitoBean
  private VisualCrossingWeatherApiClient visualCrossingWeatherApiClient;
  @MockitoBean
  private JavaMailSender mailSender;

  @Autowired
  private MockMvc mockMvc;

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  // JWT for USER_ID, signed with auth.jwt.secret-key from application-test.yml,
  // expires 2076
  private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDEiLCJpYXQiOjE3NzUzNzcxNDQsImV4cCI6MzM1MjE3NzE0NH0.9gscARdHojJ6xuMjm7RjPWrjiSefT6QlSCyJkablJL8";

  @Nested
  class GetCurrentWeather {

    @Test
    void returnsWeatherResponse() throws Exception {
      when(visualCrossingWeatherApiClient.getCurrentWeather("London"))
          .thenReturn(new VisualCrossingWeatherResponse(new CurrentConditions(15.0f, 13.0f)));

      mockMvc.perform(get("/api/weather/London/current"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.location").value("London"))
          .andExpect(jsonPath("$.temp").value(15.0))
          .andExpect(jsonPath("$.feelslike").value(13.0));
    }

    @Test
    void whenApiReturns404_returns404() throws Exception {
      when(visualCrossingWeatherApiClient.getCurrentWeather("London"))
          .thenThrow(new HttpStatusException("Location not found", HttpStatus.NOT_FOUND));

      mockMvc.perform(get("/api/weather/London/current"))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  class SubscribeToLocation {

    @Test
    void returnsCreated() throws Exception {
      when(visualCrossingWeatherApiClient.getCurrentWeather("London"))
          .thenReturn(new VisualCrossingWeatherResponse(new CurrentConditions(15.0f, 13.0f)));

      mockMvc.perform(post("/api/weather/London/subscribe")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isCreated());

      assertThat(subscriptionRepository.count()).isEqualTo(1);
    }

    @Test
    void whenApiReturns404_returns404() throws Exception {
      when(visualCrossingWeatherApiClient.getCurrentWeather("London"))
          .thenThrow(new HttpStatusException("Location not found", HttpStatus.NOT_FOUND));

      mockMvc.perform(post("/api/weather/London/subscribe")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isNotFound());
    }

    @Test
    void whenUserNotFound_returns400() throws Exception {
      when(visualCrossingWeatherApiClient.getCurrentWeather("London"))
          .thenReturn(new VisualCrossingWeatherResponse(new CurrentConditions(15.0f, 13.0f)));

      userRepository.deleteAll();

      mockMvc.perform(post("/api/weather/London/subscribe")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("User not found"));
    }

    @Test
    void whenAlreadySubscribed_returns400() throws Exception {
      when(visualCrossingWeatherApiClient.getCurrentWeather("London"))
          .thenReturn(new VisualCrossingWeatherResponse(new CurrentConditions(15.0f, 13.0f)));

      Location location = new Location();
      location.setName("London");
      locationRepository.save(location);

      User user = userRepository.findById(USER_ID).orElseThrow();
      Subscription subscription = new Subscription();
      subscription.setUser(user);
      subscription.setLocation(location);
      subscriptionRepository.save(subscription);

      mockMvc.perform(post("/api/weather/London/subscribe")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("User is already subscribed to location"));
    }

    @Test
    void withoutToken_returns401() throws Exception {
      mockMvc.perform(post("/api/weather/London/subscribe"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  class DeleteSubscription {

    @Test
    void returnsNoContent() throws Exception {
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
    void whenLocationNotFound_returns400() throws Exception {
      mockMvc.perform(delete("/api/weather/London/subscribe")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Location not found"));
    }

    @Test
    void whenSubscriptionNotFound_returns400() throws Exception {
      Location location = new Location();
      location.setName("London");
      locationRepository.save(location);

      mockMvc.perform(delete("/api/weather/London/subscribe")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Subscription not found"));
    }

    @Test
    void withoutToken_returns401() throws Exception {
      mockMvc.perform(delete("/api/weather/London/subscribe"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  class GetSubscribedLocations {

    @Test
    void returnsLocationNames() throws Exception {
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
    void whenUserNotFound_returns400() throws Exception {
      userRepository.deleteAll();

      mockMvc.perform(get("/api/weather/locations/me")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("User not found"));
    }

    @Test
    void withoutToken_returns401() throws Exception {
      mockMvc.perform(get("/api/weather/locations/me"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  class GetSubscribedCurrentWeather {

    @Test
    void returnsWeatherResponses() throws Exception {
      Location location = new Location();
      location.setName("London");
      locationRepository.save(location);

      User user = userRepository.findById(USER_ID).orElseThrow();
      Subscription subscription = new Subscription();
      subscription.setUser(user);
      subscription.setLocation(location);
      subscriptionRepository.save(subscription);

      when(visualCrossingWeatherApiClient.getCurrentWeather("London"))
          .thenReturn(new VisualCrossingWeatherResponse(new CurrentConditions(15.0f, 13.0f)));

      mockMvc.perform(get("/api/weather/locations/current")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].location").value("London"))
          .andExpect(jsonPath("$[0].temp").value(15.0))
          .andExpect(jsonPath("$[0].feelslike").value(13.0));
    }

    @Test
    void whenUserNotFound_returns400() throws Exception {
      userRepository.deleteAll();

      mockMvc.perform(get("/api/weather/locations/current")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("User not found"));
    }

    @Test
    void whenApiReturns404_returns404() throws Exception {
      Location location = new Location();
      location.setName("London");
      locationRepository.save(location);

      User user = userRepository.findById(USER_ID).orElseThrow();
      Subscription subscription = new Subscription();
      subscription.setUser(user);
      subscription.setLocation(location);
      subscriptionRepository.save(subscription);

      when(visualCrossingWeatherApiClient.getCurrentWeather("London"))
          .thenThrow(new HttpStatusException("Location not found", HttpStatus.NOT_FOUND));

      mockMvc.perform(get("/api/weather/locations/current")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
          .andExpect(status().isNotFound());
    }

    @Test
    void withoutToken_returns401() throws Exception {
      mockMvc.perform(get("/api/weather/locations/current"))
          .andExpect(status().isUnauthorized());
    }
  }
}
