package com.demystify_network.backend.dao;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.demystify_network.backend.model.userapiaccess.User;
import java.sql.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.ActiveProfiles;

@DataJdbcTest
@ActiveProfiles("test")
class UserApiAccessRepositoryTest {

  private final UserApiAccessRepository userApiAccessRepository;

  @Autowired
  public UserApiAccessRepositoryTest(UserApiAccessRepository userApiAccessRepository) {
    this.userApiAccessRepository = userApiAccessRepository;
  }

  @Test
  @DisplayName("Find user by password")
  void findUserByPassword() {
    User byPassword = userApiAccessRepository.findByPassword("my-strong-password");
    assertThat(byPassword).isNull();
    User user = new User(null, "first@last.com", "my-strong-password", true,
        new Timestamp(System.currentTimeMillis()), 100L, 10000L, 3L);
    User savedUser = userApiAccessRepository.save(user);
    assertThat(savedUser).isNotNull()
        .extracting(u -> u.getPk()).isNotNull();

    byPassword = userApiAccessRepository.findByPassword("my-strong-password");
    assertThat(byPassword).isNotNull().extracting(User::getEmail).isEqualTo("first@last.com");

  }
}