package io.github.boonx.weather_api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.boonx.weather_api.entity.Location;

public interface LocationRepository extends JpaRepository<Location, UUID> {

  Optional<Location> findByName(String name);

}
