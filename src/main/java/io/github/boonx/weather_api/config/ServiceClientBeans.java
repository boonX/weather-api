package io.github.boonx.weather_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import io.github.boonx.weather_api.client.VisualCrossingWeatherApiClient;

@Configuration
public class ServiceClientBeans {

  @Bean
  public VisualCrossingWeatherApiClient visualCrossingWeatherApiClient(
      @Value("${services.visual-crossing.weather-api.url}") String baseUrl,
      @Value("${services.visual-crossing.api-key}") String apiKey) {
    RestClient restClient = RestClient.create(baseUrl);
    return new VisualCrossingWeatherApiClient(restClient, apiKey);
  }

}
