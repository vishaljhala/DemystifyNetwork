package com.demystify_network.backend.config;

import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Configuration
public class RedisConfig {

  @Bean
  public Map<String, JedisPool> jedisTransactionsPoolMap(
      RedisConfigProperties redisConfigProperties) {
    return getTransactionJedisPoolMap(redisConfigProperties);
  }

  @Bean
  public Map<Range, JedisPool> jedisAddressesPoolMap(RedisConfigProperties redisConfigProperties) {
    return getAddressesJedisPoolMap(redisConfigProperties);
  }

  private Map<String, JedisPool> getTransactionJedisPoolMap(
      RedisConfigProperties redisConfigProperties) {
    Map<String, String> configMap = redisConfigProperties.getTransactions();
    return configMap
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> constructJedisPool(redisConfigProperties, entry)
            )
        );
  }

  private Map<Range, JedisPool> getAddressesJedisPoolMap(
      RedisConfigProperties redisConfigProperties) {
    Map<String, String> configMap = redisConfigProperties.getAddresses();
    return configMap
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                entry -> {
                  String[] fromToRange = entry.getKey().split("-");
                  return new Range(
                      Integer.parseInt(fromToRange[0]),
                      Integer.parseInt(fromToRange[1])
                  );
                },
                entry -> constructJedisPool(redisConfigProperties, entry)
            )
        );
  }

  private static JedisPool constructJedisPool(RedisConfigProperties redisConfigProperties,
      Map.Entry<String, String> entry) {
    String[] ipHost = entry.getValue().split(":");
    GenericObjectPoolConfig<Jedis> jediPoolConfig = new GenericObjectPoolConfig<>();
    jediPoolConfig.setMinIdle(redisConfigProperties.getMinIdle());
    jediPoolConfig.setMaxTotal(redisConfigProperties.getMaxTotal());
    return new JedisPool(jediPoolConfig, ipHost[0], Integer.parseInt(ipHost[1]));
  }

  public static class Range {

    private final int from;
    private final int to;

    public Range(int from, int to) {
      this.from = from;
      this.to = to;
    }

    public int getFrom() {
      return from;
    }

    public int getTo() {
      return to;
    }

    public boolean isInRange(int key) {
      return key >= getFrom() && key <= getTo();
    }

    @Override
    public String toString() {
      return "Range{" +
          "from=" + from +
          ", to=" + to +
          '}';
    }
  }
}
