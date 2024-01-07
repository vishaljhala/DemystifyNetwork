package com.demystify.network.backend.config;

public class Address {

  private final int hourlyUsage;
  private final int dailyUsage;
  private final int monthlyUsage;

  public Address(int hourlyUsage, int dailyUsage, int monthlyUsage) {
    this.hourlyUsage = hourlyUsage;
    this.dailyUsage = dailyUsage;
    this.monthlyUsage = monthlyUsage;
  }

  public int getHourlyUsage() {
    return hourlyUsage;
  }

  public int getDailyUsage() {
    return dailyUsage;
  }

  public int getMonthlyUsage() {
    return monthlyUsage;
  }
}
