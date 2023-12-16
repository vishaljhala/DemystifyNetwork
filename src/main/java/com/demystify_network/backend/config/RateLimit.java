package com.demystify_network.backend.config;

public class RateLimit {

  private final Address address;
  private final EndpointLimit endpoint;

  public RateLimit(Address address, EndpointLimit endpoint) {
    this.address = address;
    this.endpoint = endpoint;
  }

  public Address getAddress() {
    return address;
  }

  public EndpointLimit getEndpoint() {
    return endpoint;
  }
}
