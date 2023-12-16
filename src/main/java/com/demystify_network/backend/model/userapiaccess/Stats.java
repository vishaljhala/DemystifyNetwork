package com.demystify_network.backend.model.userapiaccess;


import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

public class Stats {

  @Id
  public Long pk;
  @Column("user_id")
  public Long userId;
  public String address;
  public String endpoint;

  public String score;
  public String insights;
  @Column("invoked_at")
  public Timestamp invokedAt;
  public String ip;
  @Column("additional_info")
  public String additionalInfo;

  public Stats(Long userId, String address, String endpoint, String score, String ip,
      String additionalInfo, String insights) {
    this.userId = userId;
    this.address = address;
    this.endpoint = endpoint;
    this.score = score;
    this.insights = insights;
    this.invokedAt = Timestamp.from(Instant.now());
    this.ip = ip;
    this.additionalInfo = additionalInfo;
  }
}
