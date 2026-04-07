package io.github.boonx.weather_api.dto;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public record VisualCrossingWeatherTimelineResponse(String address, List<Weather> days) {

  public record Weather(
      String datetime,
      float tempmin,
      float tempmax,
      float feelslikemin,
      float feelslikemax,
      float precip) {

    public String toWeatherReport() {
      String dayOfWeek = LocalDate.parse(datetime)
          .getDayOfWeek()
          .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

      return new StringBuilder()
          .append(dayOfWeek)
          .append(": ").append(tempmin).append("–").append(tempmax).append("°C")
          .append(", precipitation ").append(precip).append("mm")
          .toString();
    }
  }
}
