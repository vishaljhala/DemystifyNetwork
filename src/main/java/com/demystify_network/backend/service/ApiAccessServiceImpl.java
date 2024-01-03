package com.demystify_network.backend.service;

import com.demystify_network.backend.api.request.AddressRequest;
import com.demystify_network.backend.dao.AddressRedisComponent;
import com.demystify_network.backend.dao.StatsRepository;
import com.demystify_network.backend.model.userapiaccess.Stats;
import com.demystify_network.backend.model.userapiaccess.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ApiAccessServiceImpl implements ApiAccessService {

  private static final Logger LOG = LoggerFactory.getLogger(ApiAccessServiceImpl.class);
  private final StatsRepository statsRepository;
  private final AddressRedisComponent addressRedisComponent;
  private final ObjectMapper objectMapper;
  private final String sampleResponse;

  public ApiAccessServiceImpl(AddressRedisComponent addressRedisComponent,
      StatsRepository statsRepository, ObjectMapper objectMapper) {
    this.addressRedisComponent = addressRedisComponent;
    this.statsRepository = statsRepository;
    this.objectMapper = objectMapper;
    this.sampleResponse = loadSampleResponse();
  }

  private String loadSampleResponse() {
    String sampleResponseFile = "/sample-response.json";
    StringBuilder result = new StringBuilder();
    try (InputStream inputStream = getClass().getResourceAsStream(sampleResponseFile)) {
      int i;
      while ((i = inputStream.read()) != -1) {
        result.append((char) i);
      }
      return result.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Pair<Long, Integer> authorizeRequestForApiKey(String apiKey) {
    User user = addressRedisComponent.findUserByApiKey(apiKey);
    if (user == null || user.getExpiryDate().compareTo(Timestamp.from(Instant.now())) < 0
        || !user.getActive()) {
      return Pair.of(null, 401);
    }

    if (addressRedisComponent.userUsageLimitReached(apiKey, user)
        || addressRedisComponent.apiKeyRateLimitReached(apiKey, user)) {
      return Pair.of(user.getPk(), 429);
    }

    return Pair.of(user.getPk(), 200);
  }

  @Override
  public Integer authorizeRequestForAddress(String address) {
    if (addressRedisComponent.addressUsageLimitReached(address)
        || addressRedisComponent.addressRateLimitReached(address)) {
      return 429;
    }
    return 200;
  }

  @Override
  public int validateRateLimitForEndpoint(String endpoint) {
    if (addressRedisComponent.allowCallsForEndpoint(endpoint)) {
      return 200;
    }
    return 429;
  }

  @Override
  public void updateUsageStats(Long pk, AddressRequest request, String endPoint, String payload,
      HttpServletRequest httpRequest, Object additionalInfo, String insights, String fromAdd) {
    if (pk >= 0) {
      addressRedisComponent.incrementApiUsageCount(request.apiKey);
    }

    if (fromAdd != null) {
      addressRedisComponent.incrementAddressUsageCount(fromAdd);
    }

    String addInfo = Optional.ofNullable(additionalInfo).map(this::toJson).orElse("");

    Stats stats = new Stats(pk, request.address, endPoint, payload, getClientIpAddress(httpRequest),
        addInfo, insights);
    statsRepository.save(stats);
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ResponseEntity<String> healthCheck() {
    addressRedisComponent.healthCheck();
    return new ResponseEntity<>("", HttpStatus.OK);
  }

  private String getClientIpAddress(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    return ip;
  }

  @Override
  public String testCallResponse() {
    return sampleResponse;
  }
}
