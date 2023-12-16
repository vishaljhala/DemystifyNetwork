package com.demystify_network.backend.config;

public class EndpointLimit {

  private final int rps;
  private final int rpm;

  public EndpointLimit(int rps, int rpm) {
    this.rps = rps;
    this.rpm = rpm;
  }

  public int requestPerSecond() {
    return rps;
  }

  public int requestPerMonth() {
    return rpm;
  }

  public int requestPerDay() {
    return requestPerMonth() / 31;
  }

  public int requestPerHour() {
    return requestPerDay() / 24;
  }
}
