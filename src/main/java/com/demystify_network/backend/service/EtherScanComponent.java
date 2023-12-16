package com.demystify_network.backend.service;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EtherScanComponent {

  private static final Logger LOG = LoggerFactory.getLogger(EtherScanComponent.class);
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  private final String apiKey;

  private final String baseUrl;

  public EtherScanComponent(HttpClient httpClient, ObjectMapper objectMapper,
      @Value("${etherscan.api.key}") String apiKey,
      @Value("${etherscan.api.baseUrl}") String baseUrl) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
  }

  public Optional<Instant> fetchFirstTransactionTimestamp(String address) {
    Instant result = null;
    String url = baseUrl
        + "?module=account&action=txlist&startblock=0&endblock=99999999&page=1&offset=2&sort=asc&address="
        + address + "&apikey=" + apiKey;
    HttpRequest httpRequest = HttpRequest
        .newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(2))
        .build();

    try {
      HttpResponse<String> response = httpClient.send(httpRequest, ofString());
      TransactionResponse txResp = objectMapper.readValue(response.body(),
          TransactionResponse.class);

      if ("ok".equalsIgnoreCase(txResp.message) && txResp.result.length > 0) {
        Long timeStamp = txResp.result[0].timeStamp;

        result = Instant.ofEpochSecond(timeStamp);
      }
      return Optional.ofNullable(result);
    } catch (Exception ex) {
      LOG.error("Error fetching transaction list for account {}", address, ex);
      return Optional.empty();
    }
  }

  public Optional<BigInteger> fetchBalance(String address, boolean skipBalCall) {
    if (skipBalCall) {
      return Optional.of(BigInteger.TEN);
    }
    String url =
        baseUrl + "?module=account&action=balance&tag=latest&address=" + address + "&apikey="
            + apiKey;
    HttpRequest httpRequest = HttpRequest
        .newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(1))
        .build();

    try {
      HttpResponse<String> response = httpClient.send(httpRequest, ofString());
      BalanceResponse root = objectMapper.readValue(
          response.body(),
          BalanceResponse.class
      );
      if ("ok".equalsIgnoreCase(root.message)) {
        return Optional.of(root.result);
      }
      LOG.error("Error getting balance for address {}. {}", address, root.message);
      return Optional.empty();
    } catch (Exception e) {
      LOG.error("Error fetching balance for address {}. {}", address, e.getMessage(), e);
      return Optional.empty();
    }
  }

  static class BaseEtherScanResponse {

    public String status;
    public String message;
  }

  static class BalanceResponse extends BaseEtherScanResponse {

    public BigInteger result;
  }

  static class TransactionResponse extends BaseEtherScanResponse {

    public Transaction[] result;
  }

  static class Transaction {

    public Long blockNumber;
    public Long timeStamp;
  }

}
