package io.github.boonx.weather_api.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

  private final Bucket bucket;

  public RateLimitInterceptor(@Value("${api.weather.rate-limit}") int rateLimit) {
    Bandwidth limit = Bandwidth.builder().capacity(rateLimit).refillGreedy(rateLimit, Duration.ofMinutes(1)).build();
    this.bucket = Bucket.builder().addLimit(limit).build();
  }

  @Override
  public boolean preHandle(HttpServletRequest request,
      HttpServletResponse response,
      Object handler) throws Exception {

    if (bucket.tryConsume(1)) {
      return true;
    } else {
      response.setStatus(429);
      response.getWriter().write("Rate limit exceeded. Try again later.");
      return false;
    }
  }
}
