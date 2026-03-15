package io.github.boonx.weather_api.exception;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public class HttpStatusException extends RuntimeException {

  private final HttpStatusCode statusCode;

  public HttpStatusException(String message, HttpStatusCode statusCode) {
    super(message);
    this.statusCode = statusCode;
  }
}
