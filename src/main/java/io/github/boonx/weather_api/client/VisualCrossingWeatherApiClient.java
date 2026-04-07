package io.github.boonx.weather_api.client;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import io.github.boonx.weather_api.dto.VisualCrossingWeatherResponse;
import io.github.boonx.weather_api.dto.VisualCrossingWeatherTimelineResponse;
import io.github.boonx.weather_api.exception.HttpStatusException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VisualCrossingWeatherApiClient {

  private final RestClient restClient;
  private final String apiKey;

  @Cacheable(value = "weather", key = "#location")
  public VisualCrossingWeatherResponse getCurrentWeather(String location) {
    return restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/" + location)
            .queryParam("unitGroup", "metric")
            .queryParam("key", apiKey)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::isError, this::handleError)
        .body(VisualCrossingWeatherResponse.class);
  }

  public VisualCrossingWeatherTimelineResponse getWeatherTimeline(String location, LocalDate start, LocalDate end) {
    String startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE);
    String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE);
    return restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/" + location)
            .path("/" + startStr)
            .path("/" + endStr)
            .queryParam("unitGroup", "metric")
            .queryParam("key", apiKey)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::isError, this::handleError)
        .body(VisualCrossingWeatherTimelineResponse.class);
  }

  private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
    String body = new String(response.getBody().readAllBytes());

    throw new HttpStatusException(
        "External API failed: " + response.getStatusCode() + " - " + body, response.getStatusCode());
  }
}
