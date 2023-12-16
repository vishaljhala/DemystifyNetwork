package com.demystify_network.backend.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis")
public class RedisConfigProperties {

  private final Map<String, String> transactions;
  private final Map<String, String> addresses;
  private final int minIdle;
  private final int maxTotal;

  public RedisConfigProperties(Map<String, String> transactions, Map<String, String> addresses,
      int minIdle, int maxTotal) {
    this.transactions = transactions;
    this.addresses = addresses;
    this.minIdle = minIdle;
    this.maxTotal = maxTotal;
  }

  public Map<String, String> getTransactions() {
    return transactions;
  }

  public Map<String, String> getAddresses() {
    return addresses;
  }

  public int getMinIdle() {
    return minIdle;
  }

  public int getMaxTotal() {
    return maxTotal;
  }
}
