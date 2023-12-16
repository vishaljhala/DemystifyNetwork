package com.demystify_network.backend.model.userapiaccess;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public final class User {

  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'");

  @Id
  public Long pk;
  @Column("expiry_date")
  public Timestamp expiryDate;
  public Boolean isActive;
  public String password;
  public String email;
  public Long dailyCalls = 0L;
  public Long monthlyCalls = 0L;
  public Long concurrentCalls = 0L;

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
}
