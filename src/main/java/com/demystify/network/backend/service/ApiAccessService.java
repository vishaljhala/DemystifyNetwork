package com.demystify.network.backend.service;

import com.demystify.network.backend.api.request.AddressRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;

public interface ApiAccessService {

  Pair<Long, Integer> authorizeRequestForApiKey(String apiKey);

  Integer authorizeRequestForAddress(String address);

  int validateRateLimitForEndpoint(String endpoint);

  void updateUsageStats(Long pk, AddressRequest request, String endPoint, String payload,
      HttpServletRequest httpRequest,
      Object additionalInfo, String insights, String fromAdd);

  ResponseEntity<String> healthCheck();

  String testCallResponse();
}
