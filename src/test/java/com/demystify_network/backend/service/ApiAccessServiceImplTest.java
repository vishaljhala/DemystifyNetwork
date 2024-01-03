package com.demystify_network.backend.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demystify_network.backend.dao.AddressRedisComponent;
import com.demystify_network.backend.dao.StatsRepository;
import com.demystify_network.backend.model.userapiaccess.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiAccessServiceImplTest {

  @Mock
  private StatsRepository statsRepository;
  @Mock
  private AddressRedisComponent addressRedisComponent;
  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private ApiAccessServiceImpl service;


  @Test
  @DisplayName("Should return 401 for a api key where no user found")
  void shouldReturn401ForAApiKeyWhereNoUserFound() {
    when(addressRedisComponent.findUserByApiKey(anyString())).thenReturn(null);

    Pair<Long, Integer> resultPair = service.authorizeRequestForApiKey("someKey");

    assertThat(resultPair).isNotNull()
        .extracting(Pair::getValue)
        .isNotNull()
        .isEqualTo(401);
  }

  @Test
  @DisplayName("Should return 401 for an api key where user is found, but has expired")
  void shouldReturn401ForAnApiKeyWhereUserIsFoundButHasExpired() {
    User user = mock(User.class);
    when(user.getExpiryDate()).thenReturn(
        new Timestamp(ZonedDateTime.now().minusDays(1L).toEpochSecond()));
    when(addressRedisComponent.findUserByApiKey(anyString())).thenReturn(user);

    Pair<Long, Integer> resultPair = service.authorizeRequestForApiKey("someExpiredKey");
    assertThat(resultPair).isNotNull()
        .extracting(Pair::getValue)
        .isNotNull()
        .isEqualTo(401);
  }

  @ParameterizedTest
  @CsvSource({
      "userUsageReached, true",
      "apiUsageReached, true"
  })
  @DisplayName("Should return 429 when api usage or user usage has reached the limit")
  void shouldReturn429WhenApiUsageOrUserUsageHasReachedTheLimit(String apiKey,
      Boolean returnValue) {
    User user = mock(User.class);
    Timestamp value = new Timestamp(ZonedDateTime.now().plusDays(10L).toInstant().toEpochMilli());
    when(user.getExpiryDate()).thenReturn(
        value);
    when(user.getActive()).thenReturn(Boolean.TRUE);
    when(addressRedisComponent.findUserByApiKey(anyString())).thenReturn(user);
    when(addressRedisComponent.userUsageLimitReached(apiKey, user)).thenReturn(returnValue);

    if (Boolean.FALSE == returnValue) {
      when(addressRedisComponent.apiKeyRateLimitReached(apiKey, user)).thenReturn(
          Boolean.TRUE);
    }

    Pair<Long, Integer> resultPair = service.authorizeRequestForApiKey(apiKey);

    assertThat(resultPair).isNotNull()
        .extracting(Pair::getValue)
        .isNotNull()
        .isEqualTo(429);
  }

  @ParameterizedTest
  @CsvSource({
      "addressUsageLimitReached, true, false, 429",
      "addressRateLimitReached, false, true, 429",
      "okAddress, false, false, 200"
  })
  @DisplayName("Tests to authorize requests based on the address usage")
  void testsToAuthorizeRequestsBasedOnTheAddressUsage(String address,
      Boolean addressUsageLimitReached, Boolean addressRateLimitReached, Integer expectedResult) {
    when(addressRedisComponent.addressUsageLimitReached(address)).thenReturn(
        addressUsageLimitReached);
    if (Boolean.FALSE == addressUsageLimitReached) {
      when(addressRedisComponent.addressRateLimitReached(address)).thenReturn(
          addressRateLimitReached);
    }
    Integer result = service.authorizeRequestForAddress(address);
    assertThat(result).isEqualTo(expectedResult);
    verify(addressRedisComponent).addressUsageLimitReached(anyString());
    if (Boolean.FALSE == addressUsageLimitReached) {
      verify(addressRedisComponent).addressRateLimitReached(anyString());
    } else {
      verify(addressRedisComponent, never()).addressRateLimitReached(anyString());
    }
  }

  @ParameterizedTest
  @CsvSource({
      "endpoint429, false, 429",
      "endpoint200, true, 200"
  })
  @DisplayName("Should validate rate limit for the endpoint")
  void shouldValidateRateLimitForTheEndpoint(String endpoint, Boolean allowCallsForEndpointResult,
      Integer expectedResult) {
    when(addressRedisComponent.allowCallsForEndpoint(endpoint)).thenReturn(
        allowCallsForEndpointResult);

    assertThat(service.validateRateLimitForEndpoint(endpoint)).isEqualTo(expectedResult);
  }
}