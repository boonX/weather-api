package io.github.boonx.weather_api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.boonx.weather_api.entity.Location;
import io.github.boonx.weather_api.entity.Subscription;
import io.github.boonx.weather_api.entity.User;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  boolean existsByUserAndLocation(User user, Location location);

  Optional<Subscription> findByUserAndLocation(User user, Location location);

}
