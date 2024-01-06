package com.demystify.network.backend.dao;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.demystify.network.backend.model.userapiaccess.Stats;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.ActiveProfiles;

@DataJdbcTest
@ActiveProfiles("test")
class StatsRepositoryTest {

  private final StatsRepository statsRepository;

  @Autowired
  StatsRepositoryTest(StatsRepository statsRepository) {
    this.statsRepository = statsRepository;
  }

  @Test
  @DisplayName("Insert new stats entry")
  void insertNewStatsEntry() {
    Optional<Stats> byId = statsRepository.findById(666L);
    assertThat(byId).isEmpty();

    Stats newStats = statsRepository.save(
        new Stats(42L, "address", "/endpoint", "0.12", "127.0.0.1", "addInfo", "insights"));

    assertThat(newStats).isNotNull();

    Long newId = newStats.pk;

    byId = statsRepository.findById(newId);
    assertThat(byId).isNotEmpty();

    assertThat(byId.get()).extracting(s -> s.pk).isEqualTo(newId);

  }
}