package com.demystify.network.backend.model.userapiaccess;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("USERS")
public final class User {

  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'");

  @Id
  private Long pk;
  @Column("EXPIRY_DATE")
  private Timestamp expiryDate;
  private Boolean isActive;
  private String password;
  private String email;
  private Long dailyCalls = 0L;
  private Long monthlyCalls = 0L;
  private Long concurrentCalls = 0L;

  public User(Long pk, String email, String password, Boolean isActive, Timestamp expiryDate,
      Long dailyCalls, Long monthlyCalls, Long concurrentCalls) {
    this.pk = pk;
    this.expiryDate = expiryDate;
    this.email = email;
    this.isActive = isActive;
    this.password = password;
    this.dailyCalls = dailyCalls;
    this.monthlyCalls = monthlyCalls;
    this.concurrentCalls = concurrentCalls;
  }

  public String toString() {
    return String.format("%d,%b,%s,%d,%d,%d", pk, isActive,
        DTF.format(expiryDate.toInstant().atZone(ZoneOffset.UTC)), dailyCalls,
        monthlyCalls, concurrentCalls);
  }

  public static User fromString(String key, String values) {
    String[] tokens = values.split(",");
    Date date = Date.from(LocalDateTime.parse(tokens[2], DTF).toInstant(ZoneOffset.UTC));
    User user = new User(Long.parseLong(tokens[0]), null, key, Boolean.parseBoolean(tokens[1]),
        new Timestamp(date.getTime()), Long.parseLong(tokens[3]), Long.parseLong(tokens[4]),
        Long.parseLong(tokens[5]));
    return user;
  }

  public Long getPk() {
    return pk;
  }

  public void setPk(Long pk) {
    this.pk = pk;
  }

  public Timestamp getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(Timestamp expiryDate) {
    this.expiryDate = expiryDate;
  }

  public Boolean getActive() {
    return isActive;
  }

  public void setActive(Boolean active) {
    isActive = active;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Long getDailyCalls() {
    return dailyCalls;
  }

  public void setDailyCalls(Long dailyCalls) {
    this.dailyCalls = dailyCalls;
  }

  public Long getMonthlyCalls() {
    return monthlyCalls;
  }

  public void setMonthlyCalls(Long monthlyCalls) {
    this.monthlyCalls = monthlyCalls;
  }

  public Long getConcurrentCalls() {
    return concurrentCalls;
  }

  public void setConcurrentCalls(Long concurrentCalls) {
    this.concurrentCalls = concurrentCalls;
  }
}
