package com.demystify_network.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;

import com.demystify_network.backend.api.request.AddressRequest;

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
