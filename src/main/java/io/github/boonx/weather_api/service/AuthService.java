package io.github.boonx.weather_api.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.github.boonx.weather_api.dto.LoginRequest;
import io.github.boonx.weather_api.dto.LoginResponse;
import io.github.boonx.weather_api.dto.RegisterRequest;
import io.github.boonx.weather_api.entity.User;
import io.github.boonx.weather_api.exception.HttpStatusException;
import io.github.boonx.weather_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public LoginResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new HttpStatusException("Email already registered", HttpStatus.BAD_REQUEST);
    }

    User user = new User();
    user.setEmail(request.email());
    user.setPassword(passwordEncoder.encode(request.password()));

    User savedUser = userRepository.save(user);

    return new LoginResponse(jwtService.generateToken(savedUser.getId()));
  }

  public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new HttpStatusException("Invalid credentials", HttpStatus.BAD_REQUEST));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new HttpStatusException("Invalid credentials", HttpStatus.BAD_REQUEST);
    }

    return new LoginResponse(jwtService.generateToken(user.getId()));
  }
}
