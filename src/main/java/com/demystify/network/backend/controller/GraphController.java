package com.demystify.network.backend.controller;

import com.demystify.network.backend.api.request.AddressRequest;
import com.demystify.network.backend.api.response.ProScoreKnowledgeGraphResponse;
import com.demystify.network.backend.exception.NotFoundException;
import com.demystify.network.backend.model.Insights;
import com.demystify.network.backend.service.ApiAccessService;
import com.demystify.network.backend.service.TrustlessProService;
import com.demystify.network.backend.util.Util;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraphController {

  private static final Set<String> MM_ADDINFO_KEYS_EIP =
      Set.of("to", "from", "value", "maxFeePerGas", "maxPriorityFeePerGas");
  private static final Set<String> MM_ADDINFO_KEYS_LEGACY =
      Set.of("to", "from", "value", "gas", "gasPrice");

  private final TrustlessProService trustlessProService;
  private final ApiAccessService apiAccessService;
  private final boolean validateMetaMaskReq;

  private static final Logger LOG = LoggerFactory.getLogger(GraphController.class);

  public GraphController(TrustlessProService trustlessProService,
      ApiAccessService apiAccessService,
      @Value("${consensusapp.metamask.request-validation.enabled}") boolean validateMetaMaskReq) {
    this.trustlessProService = trustlessProService;
    this.apiAccessService = apiAccessService;
    this.validateMetaMaskReq = validateMetaMaskReq;
  }

  @GetMapping("/api/health/rualv")
  public ResponseEntity<String> healthCheck() {
    return apiAccessService.healthCheck();
  }

  @PostMapping("/address/scoreTest")
  public ResponseEntity<String> proScoreTest(@RequestBody AddressRequest request,
      HttpServletRequest httpServletRequest) {
    if (!isValidScoreRequest(request, false)) {
      return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY);
    }
    //Check for authorization
    Pair<Long, Integer> result = apiAccessService.authorizeRequestForApiKey(request.apiKey);
    if (result.getRight() == 429) {
      return new ResponseEntity<>(null, HttpStatus.TOO_MANY_REQUESTS);
    }
    if (result.getRight() == 401) {
      return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
    }

    return new ResponseEntity<>(apiAccessService.testCallResponse(), HttpStatus.OK);
  }

  @PostMapping("/address/threatIntel")
  public ResponseEntity<ProScoreKnowledgeGraphResponse> proScoreMetaMask(
      @RequestBody AddressRequest request, HttpServletRequest httpServletRequest) {
    Insights insights = new Insights();
    insights.add("letTheFunBegin");

    int endpointRateLimitResult = apiAccessService.validateRateLimitForEndpoint("threatIntel");
    if (endpointRateLimitResult != 200) {
      return new ResponseEntity<>(null, HttpStatus.valueOf(endpointRateLimitResult));
    }

    if (!isValidScoreRequest(request, false)) {
      return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    String fromAdd = validateMetaMaskRequestAndReturnAddress(request.additionalInfo, "from",
        validateMetaMaskReq);

    Integer result = apiAccessService.authorizeRequestForAddress(fromAdd);
    if (result == 429) {
      return new ResponseEntity<>(null, HttpStatus.TOO_MANY_REQUESTS);
    }

    //Service Layer
    ProScoreKnowledgeGraphResponse response = trustlessProService.proScore(request, insights);
    //Update stats at end.
    apiAccessService.updateUsageStats(-1L, request, "/address/threatIntel",
        "{score: " + response.riskScore + "}",
        httpServletRequest, request.additionalInfo, insights.toString(), fromAdd);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  private String validateMetaMaskRequestAndReturnAddress(Object additionalInfo, String addressKey,
      boolean validateAllKeys) {
    return Optional.ofNullable(additionalInfo)
        .filter(addInf -> addInf instanceof Map)
        .map(addInf -> (Map<?, ?>) addInf)
        .filter(map -> !validateAllKeys || map.keySet()
            .containsAll(MM_ADDINFO_KEYS_EIP) || map.keySet().containsAll(MM_ADDINFO_KEYS_LEGACY))
        .map(addInf -> addInf.get(addressKey))
        .map(Object::toString)
        .orElseThrow(() -> {
          String msg = String.format("Not all keys defined in %s or in %s found in %s",
              MM_ADDINFO_KEYS_EIP, MM_ADDINFO_KEYS_LEGACY,
              additionalInfo);
          LOG.warn(msg);
          return new NotFoundException(msg);
        });
  }

  @PostMapping("/address/score")
  public ResponseEntity<ProScoreKnowledgeGraphResponse> proScore(
      @RequestBody AddressRequest request, HttpServletRequest httpServletRequest) {
    Insights insights = new Insights();
    insights.add("letTheFunBegin");

    if (!isValidScoreRequest(request, true)) {
      return new ResponseEntity<>(null, HttpStatus.UNPROCESSABLE_ENTITY);
    }
    // insights.add("isValidScoreRequest");

    //Check for authorization
    Pair<Long, Integer> result = apiAccessService.authorizeRequestForApiKey(request.apiKey);
    insights.add("userApiAccessService.authorizeRequest");

    if (result.getRight() == 429) {
      return new ResponseEntity<>(null, HttpStatus.TOO_MANY_REQUESTS);
    }
    if (result.getRight() == 401) {
      return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
    }

    //Service Layer
    ProScoreKnowledgeGraphResponse response = trustlessProService.proScore(
        request, insights);

    //Update stats at end.
    apiAccessService.updateUsageStats(result.getLeft(), request, "/address/score",
        "{score: " + response.riskScore + "}",
        httpServletRequest, request.additionalInfo, insights.toString(), null);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  private boolean isValidScoreRequest(AddressRequest request, boolean validateAPIKey) {
    request.address = request.address.toLowerCase();
    request.apiKey = request.apiKey.toLowerCase();
    if (!Util.validEthAddress(request.address)) {
      return false;
    }

    if (!validateAPIKey) {
      return true;
    }

    try {
      UUID.fromString(request.apiKey);
    } catch (IllegalArgumentException e) {
      return false;
    }

    return true;
  }
}
