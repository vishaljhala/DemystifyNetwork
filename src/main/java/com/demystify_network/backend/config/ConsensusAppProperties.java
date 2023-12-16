package com.demystify_network.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consensusapp")
public class ConsensusAppProperties {

  private final RateLimit rateLimit;

  public ConsensusAppProperties(RateLimit rateLimit) {
    this.rateLimit = rateLimit;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }
}
