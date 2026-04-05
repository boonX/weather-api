package io.github.boonx.weather_api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.boonx.weather_api.dto.WeatherResponse;
import io.github.boonx.weather_api.service.JwtService;
import io.github.boonx.weather_api.service.WeatherService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeatherControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private WeatherService weatherService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private RedisConnectionFactory redisConnectionFactory;

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String TEST_TOKEN = "test-token";

  @BeforeEach
  void setUp() {
    when(jwtService.isValid(TEST_TOKEN)).thenReturn(true);
    when(jwtService.extractUserId(TEST_TOKEN)).thenReturn(USER_ID.toString());
  }

  @Test
  void getCurrentWeather_returnsWeatherResponse() throws Exception {
    when(weatherService.getCurrentWeather("London"))
        .thenReturn(new WeatherResponse("London", 15.0f, 13.0f));

    mockMvc.perform(get("/api/weather/London/current"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.location").value("London"))
        .andExpect(jsonPath("$.temp").value(15.0))
        .andExpect(jsonPath("$.feelslike").value(13.0));
  }

  @Test
  void subscribeToLocation_returnsCreated() throws Exception {
    mockMvc.perform(post("/api/weather/London/subscribe")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
        .andExpect(status().isCreated());

    verify(weatherService).subscribeToLocation(USER_ID, "London");
  }

  @Test
  void deleteSubscription_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/weather/London/subscribe")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
        .andExpect(status().isNoContent());

    verify(weatherService).deleteSubscription(USER_ID, "London");
  }

  @Test
  void getSubscribedLocations_returnsLocationNames() throws Exception {
    when(weatherService.getSubscribedLocations(USER_ID))
        .thenReturn(List.of("London", "New York"));

    mockMvc.perform(get("/api/weather/locations/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("London"))
        .andExpect(jsonPath("$[1]").value("New York"));
  }

  @Test
  void getSubscribedCurrentWeather_returnsWeatherResponses() throws Exception {
    when(weatherService.getSubscribedCurrentWeather(USER_ID))
        .thenReturn(List.of(
            new WeatherResponse("London", 15.0f, 13.0f),
            new WeatherResponse("New York", 22.0f, 20.0f)));

    mockMvc.perform(get("/api/weather/locations/current")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].location").value("London"))
        .andExpect(jsonPath("$[0].temp").value(15.0))
        .andExpect(jsonPath("$[0].feelslike").value(13.0))
        .andExpect(jsonPath("$[1].location").value("New York"))
        .andExpect(jsonPath("$[1].temp").value(22.0))
        .andExpect(jsonPath("$[1].feelslike").value(20.0));
  }
}
