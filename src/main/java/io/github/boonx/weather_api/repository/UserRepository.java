package io.github.boonx.weather_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.boonx.weather_api.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.subscriptions s JOIN FETCH s.location")
  List<User> findAllWithSubscriptions();
}
