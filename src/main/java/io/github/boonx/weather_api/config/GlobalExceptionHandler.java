package io.github.boonx.weather_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.boonx.weather_api.exception.HttpStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(HttpStatusException.class)
  public ResponseEntity<ProblemDetail> handleExternalServiceException(HttpStatusException ex) {
    log.error("External API error", ex);

    ProblemDetail problemDetail = ProblemDetail.forStatus(ex.getStatusCode());
    problemDetail.setDetail(ex.getMessage());

    return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
  }
}
