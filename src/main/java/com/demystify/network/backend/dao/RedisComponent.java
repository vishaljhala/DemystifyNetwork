package com.demystify.network.backend.dao;

import com.demystify.network.backend.config.RedisConfig.Range;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.tuple.Pair;
import redis.clients.jedis.JedisPool;

public abstract class RedisComponent {

  protected Pair<Range, JedisPool> getJedisPool(
      Integer pk,
      Map<Range, JedisPool> jedisPoolMap
  ) {
    Entry<Range, JedisPool> jedisPool = jedisPoolMap
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().isInRange(pk))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unable to find the jedis pool for PK " + pk));
    return Pair.of(jedisPool.getKey(), jedisPool.getValue());
  }

  protected Pair<String, JedisPool> getTransactionJedisPool(
      String year,
      Map<String, JedisPool> jedisPoolMap
  ) {
    Entry<String, JedisPool> jedisPoolEntry = jedisPoolMap
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().equals(year))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unable to find the jedis pool for year " + year));

    return Pair.of(jedisPoolEntry.getKey(), jedisPoolEntry.getValue());
  }
}
