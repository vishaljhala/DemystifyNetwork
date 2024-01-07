package com.demystify.network.backend.dao;

import com.demystify.network.backend.model.Edge;
import com.demystify.network.backend.model.Insights;
import com.demystify.network.backend.util.Util;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

@Component
public class TransactionsRedisComponent extends RedisComponent {

  protected static final Logger LOG = LoggerFactory.getLogger(TransactionsRedisComponent.class);

  private final Map<String, JedisPool> jedisTransactionsPoolMap;

  public TransactionsRedisComponent(Map<String, JedisPool> jedisTransactionsPoolMap) {
    this.jedisTransactionsPoolMap = jedisTransactionsPoolMap;
  }

  public List<Edge> findDebitTransactions(
      byte[] pk, List<String> shards, boolean isRootNode, Insights insights) {
    return findTransactions(shards, isRootNode, insights, pk);
  }

  public List<Edge> findCreditTransactions(
      byte[] pk, List<String> shards, boolean isRootNode, Insights insights) {
    return findTransactions(shards, isRootNode, insights, Util.toFromPk(pk));
  }

  private List<Edge> findTransactions(
      List<String> shards, boolean isRootNode, Insights insights, byte[] pk) {
    Integer queryCount = (int) Math.ceil((double) (isRootNode ? 1000 : 100) / shards.size());

    return shards.stream()
        .flatMap(shard -> redisOperation(shard, pk, queryCount, insights).stream())
        .toList();
  }

  private List<Edge> redisOperation(
      String shard, byte[] pk, Integer queryCount, Insights insights) {
    Jedis jedis = null;
    Pair<String, JedisPool> jedisPair = getTransactionJedisPool(shard, jedisTransactionsPoolMap);
    try {
      JedisPool jedisPool = jedisPair.getRight();
      jedis = jedisPool.getResource();

      return queryRedisTransactions(jedis, pk, queryCount, insights);
    } catch (JedisConnectionException jcex) {
      LOG.error("Error while getting connection to redis for range {}", jedisPair.getLeft(), jcex);

      throw new JedisConnectionException(
          "Error while getting connection to redis for range "
              + jedisPair.getLeft()
              + ". "
              + jcex.getMessage(),
          jcex);
    } finally {
      Optional.ofNullable(jedis).ifPresent(Jedis::close);
    }
  }

  private List<Edge> queryRedisTransactions(Jedis jedis, byte[] pk,
      Integer queryCount, Insights insights) {
    long toAddressCount = jedis.hlen(pk);
    List<Edge> retAddresses = new ArrayList<>();
    // if(pk.length == 4)
    //   LOG.info("pk size: " + pk.length + " " + pk[0] + " " + pk[1] + " " + pk[2] + " " + pk[3] +
    // " count: " + toAddressCount);
    // else
    //   LOG.info("pk size: " + pk.length + " " + pk[0] + " " + pk[1] + " " + pk[2] + " " + pk[3] +
    // " " + pk[4]  + " count: " + toAddressCount);

    if (toAddressCount <= 100) {
      Long start = System.currentTimeMillis();
      Map<byte[], byte[]> toAddresses = jedis.hgetAll(pk);
      insights.addTxnRedisQueryTime(start);
      for (Entry<byte[], byte[]> keyVal : toAddresses.entrySet()) {
        retAddresses.add(
            new Edge(keyVal.getKey(), new String(keyVal.getValue(), StandardCharsets.UTF_8)));
      }
    } else {
      ScanParams params = new ScanParams().count(queryCount);
      Long start = System.currentTimeMillis();
      ScanResult<Entry<byte[], byte[]>> toAddressesBytes =
          jedis.hscan(pk, ScanParams.SCAN_POINTER_START.getBytes(StandardCharsets.UTF_8), params);
      insights.addTxnRedisQueryTime(start);

      for (Entry<byte[], byte[]> keyVal : toAddressesBytes.getResult()) {
        retAddresses.add(
            new Edge(keyVal.getKey(), new String(keyVal.getValue(), StandardCharsets.UTF_8)));
      }
    }
    return retAddresses;
  }
}
