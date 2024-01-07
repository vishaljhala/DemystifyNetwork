package com.demystify.network.backend.dao;

import com.demystify.network.backend.config.Address;
import com.demystify.network.backend.config.ConsensusAppProperties;
import com.demystify.network.backend.config.EndpointLimit;
import com.demystify.network.backend.config.RedisConfig.Range;
import com.demystify.network.backend.model.Insights;
import com.demystify.network.backend.model.Node;
import com.demystify.network.backend.model.Node.NodeType;
import com.demystify.network.backend.model.userapiaccess.User;
import com.demystify.network.backend.service.TrustlessServiceBase;
import com.demystify.network.backend.service.discord.DiscordService;
import com.demystify.network.backend.util.Util;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.SetParams;

@Component
public class AddressRedisComponent extends RedisComponent {

  private static final String API_KEY_DETAILS = "user-api-key-details-";
  private static final String API_KEY_DAILY_USAGE = "user-api-key-dialy-usage-";
  private static final String API_KEY_MONTHLY_USAGE = "user-api-key-monthly-usage-";
  private static final String API_KEY_RATE_LIMIT = "user-api-key-rate-limit-";
  private static final String API_KEY_LAST_USED_DATE = "user-api-key-last-used-month-";

  private static final String ADDRESS_HOURLY_USAGE = "address-key-hourly-usage-";
  private static final String ADDRESS_DAILY_USAGE = "address-key-daily-usage-";
  private static final String ADDRESS_MONTHLY_USAGE = "address-key-monthly-usage-";
  private static final String ADDRESS_RATE_LIMIT = "address-key-rate-limit-";
  private static final String ADDRESS_LAST_USED_DATE = "address-key-last-used-month-";

  private static final String ENDPOINT_SECOND_USAGE = "endpoint-key-second-usage-";
  private static final String ENDPOINT_HOURLY_USAGE = "endpoint-key-hourly-usage-";
  private static final String ENDPOINT_DAILY_USAGE = "endpoint-key-daily-usage-";
  private static final String ENDPOINT_MONTHLY_USAGE = "endpoint-key-monthly-usage-";


  private static final Logger LOG = LoggerFactory.getLogger(AddressRedisComponent.class);
  private static final int HOUR_IN_SEC = 3600;
  private static final int DAY_IN_SEC = 86400;
  private static final int MONTH_IN_SEC = 2678400;

  private final Map<Range, JedisPool> jedisAddressesPoolMap;
  private final UserApiAccessRepository userApiAccessRepository;
  private final ConsensusAppProperties consensusAppProperties;
  private final DiscordService discordService;

  public AddressRedisComponent(Map<Range, JedisPool> jedisAddressesPoolMap,
      UserApiAccessRepository userApiAccessRepository,
      ConsensusAppProperties consensusAppProperties, DiscordService discordService) {
    this.jedisAddressesPoolMap = jedisAddressesPoolMap;
    this.userApiAccessRepository = userApiAccessRepository;
    this.consensusAppProperties = consensusAppProperties;
    this.discordService = discordService;
  }

  public void healthCheck() {
    redisOperation(1, jedis -> {
      jedis.set("healthCheck", "AllIsWell");
      return null;
    });
  }

  public Node getAddressPk(String walletAddress, Insights insights) {
    return redisOperation(1, jedis -> {
      Node rootNode = null;
      Pair<byte[], byte[]> addBytes = Util.toEthAddressBytes(walletAddress);
      byte[] pkBytes = jedis.hget(addBytes.getLeft(), addBytes.getRight());
      if (pkBytes != null) {
        Long start = System.currentTimeMillis();
        Pipeline p = jedis.pipelined();
        Response<Map<byte[], byte[]>> redisResponse = p.hgetAll(pkBytes);
        p.sync();
        insights.addAddressRedisQueryTime(start);
        if (redisResponse != null) {
          rootNode = new Node(pkBytes, NodeType.ROOT, null);
          rootNode.redisResponseForAddressQuery = redisResponse;
          rootNode.parseAddressValues(true);
        }
      }
      return rootNode;
    });
  }
  
  public void findByPKPipeline(List<Node> nodes, Insights insights) {

    //List<Pair<byte[],Response<Map<byte[], byte[]>>>> nextNodeResponses = new ArrayList<>();
    redisOperation(1, jedis -> {
      /* BUG Above - We are not grabbing right instance of Redis Address Shard. This will be a future problem */
      Pipeline p = jedis.pipelined();

      for (Node node : nodes) {
          node.redisResponseForAddressQuery = p.hgetAll(node.bytePk);
          insights.addAddressRedisCount(1);
          if(insights.getAddressRedisCount() > TrustlessServiceBase.MAX_ADDRESSES_TO_SCAN)
            break;
      }
      Long start = System.currentTimeMillis();
      p.sync();
      insights.addAddressPipelineQueryTime(start);
      return null;
    });
  }

  public User findUserByApiKey(String apiKey) {
    try {
      return redisOperation(1, jedis -> {
        String apiKeyDetails = jedis.get(API_KEY_DETAILS + apiKey);
        if (apiKeyDetails != null) {
          return User.fromString(apiKey, apiKeyDetails);
        }

        User user = userApiAccessRepository.findByPassword(apiKey);
        if (user == null) {
          return null;
        }

        jedis.set(API_KEY_DETAILS + apiKey, user.toString());
        return user;
      });
    } catch (RuntimeException runtimeException) {
      // This catch is only for debugging purpose to see the details when we have NumberFormatException
      // (apiKeyDetails empty or expiry date (3rd token) is empty). Remove this catch once we have the details
      // and issue is fixed
      LOG.error("Error while finding user by key {}", apiKey, runtimeException);
      throw runtimeException;
    }
  }

  public Boolean userUsageLimitReached(String apiKey, User user) {
    return redisOperation(1, jedis -> {
      int apiKeyDailyUsage = Util.safeParseInt(jedis.get(API_KEY_DAILY_USAGE + apiKey));
      int apiKeyMonthlyUsage = Util.safeParseInt(jedis.get(API_KEY_MONTHLY_USAGE + apiKey));
      Long apiKeyLastUsedDate = Util.safeParseLong(jedis.get(API_KEY_LAST_USED_DATE + apiKey));

      LocalDateTime today = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
      LocalDateTime apiKeyLastUsedTime = LocalDateTime.ofInstant(
          Instant.ofEpochSecond(apiKeyLastUsedDate), ZoneOffset.UTC);

      if (apiKeyLastUsedTime.getMonth() != today.getMonth()) {
        apiKeyDailyUsage = 0;
        apiKeyMonthlyUsage = 0;
        jedis.set(API_KEY_DAILY_USAGE + apiKey, Integer.toString(apiKeyDailyUsage));
        jedis.set(API_KEY_MONTHLY_USAGE + apiKey, Integer.toString(apiKeyDailyUsage));

      } else if (apiKeyLastUsedTime.getDayOfMonth() != today.getDayOfMonth()) {
        apiKeyDailyUsage = 0;
        jedis.set(API_KEY_DAILY_USAGE + apiKey, Integer.toString(apiKeyDailyUsage));
      }

      return apiKeyDailyUsage >= user.getDailyCalls()
          || apiKeyMonthlyUsage >= user.getMonthlyCalls();
    });
  }

  public Boolean apiKeyRateLimitReached(String apiKey, User user) {
    return redisOperation(1, jedis -> {
      int requestInProgress = Util.safeParseInt(jedis.get(API_KEY_RATE_LIMIT + apiKey + Instant.now().getEpochSecond()));
      if (requestInProgress >= user.getConcurrentCalls())
        return true;

      Transaction t = jedis.multi();
      t.incr(API_KEY_RATE_LIMIT + apiKey + Instant.now().getEpochSecond());
      t.expire(API_KEY_RATE_LIMIT + apiKey + Instant.now().getEpochSecond(), 1);
      t.exec();
      return false;
    });
  }

  public Boolean addressUsageLimitReached(String address) {
    return redisOperation(1, jedis -> {
      String hourlyKey = ADDRESS_HOURLY_USAGE + address;
      String dailyKey = ADDRESS_DAILY_USAGE + address;
      String monthlyKey = ADDRESS_MONTHLY_USAGE + address;
      String lastUsedKey = ADDRESS_LAST_USED_DATE + address;

      int hourlyUsage = Util.safeParseInt(jedis.get(hourlyKey));
      int dailyUsage = Util.safeParseInt(jedis.get(dailyKey));
      int monthlyUsage = Util.safeParseInt(jedis.get(monthlyKey));
      Long lastUsedDate = Util.safeParseLong(jedis.get(lastUsedKey));

      LocalDateTime today = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
      LocalDateTime addressLastUsedTime = LocalDateTime.ofInstant(
          Instant.ofEpochSecond(lastUsedDate), ZoneOffset.UTC);

      if (addressLastUsedTime.getMonth() != today.getMonth()) {
        hourlyUsage = 0;
        dailyUsage = 0;
        monthlyUsage = 0;
        jedis.setex(hourlyKey, HOUR_IN_SEC, Integer.toString(hourlyUsage));
        jedis.setex(dailyKey, DAY_IN_SEC, Integer.toString(dailyUsage));
        jedis.setex(monthlyKey, MONTH_IN_SEC, Integer.toString(dailyUsage));
      } else if (addressLastUsedTime.getDayOfMonth() != today.getDayOfMonth()) {
        hourlyUsage = 0;
        dailyUsage = 0;
        jedis.setex(hourlyKey, HOUR_IN_SEC, Integer.toString(hourlyUsage));
        jedis.setex(dailyKey, DAY_IN_SEC, Integer.toString(dailyUsage));

      } else if (addressLastUsedTime.getHour() != today.getHour()) {
        hourlyUsage = 0;
        jedis.setex(hourlyKey, HOUR_IN_SEC, Integer.toString(hourlyUsage));
      }

      Address rateLimitConfig = this.consensusAppProperties.getRateLimit().getAddress();
      if (hourlyUsage >= rateLimitConfig.getHourlyUsage()) {
        String msg = String.format("Hourly usage %d of address %s exceeded",
            rateLimitConfig.getHourlyUsage(), address);
        discordService.sendFeedbackToDiscordChannel(msg);
        LOG.info(msg);
        return true;
      }

      if (dailyUsage >= rateLimitConfig.getDailyUsage()) {
        String msg = String.format("Daily usage %d of address %s exceeded",
            rateLimitConfig.getDailyUsage(), address);
        discordService.sendFeedbackToDiscordChannel(msg);
        LOG.info(msg);
        return true;
      }

      if (monthlyUsage >= rateLimitConfig.getMonthlyUsage()) {
        String msg = String.format("Monthly usage %d of address %s exceeded",
            rateLimitConfig.getMonthlyUsage(), address);
        discordService.sendFeedbackToDiscordChannel(msg);
        LOG.info(msg);
        return true;
      }
      return false;
    });
  }

  public Boolean addressRateLimitReached(String address) {
    return redisOperation(1,
        jedis -> jedis.set(ADDRESS_RATE_LIMIT + address , "1", SetParams.setParams().nx().ex(2))
            == null);
  }

  public void incrementApiUsageCount(String apiKey) {
    redisOperation(1, jedis -> {
      jedis.incr(API_KEY_DAILY_USAGE + apiKey);
      jedis.incr(API_KEY_MONTHLY_USAGE + apiKey);
      jedis.set(API_KEY_LAST_USED_DATE + apiKey, Long.toString(Instant.now().getEpochSecond()));

      return null;
    });
  }

  public void incrementAddressUsageCount(String address) {
    redisOperation(1, jedis -> {
      jedis.incr(ADDRESS_HOURLY_USAGE + address);
      jedis.incr(ADDRESS_DAILY_USAGE + address);
      jedis.incr(ADDRESS_MONTHLY_USAGE + address);
      jedis.set(ADDRESS_LAST_USED_DATE + address, Long.toString(Instant.now().getEpochSecond()));

      return null;
    });
  }

  private <T> T redisOperation(Integer pk, Function<Jedis, T> redisConsumer) {
    Jedis jedis = null;
    Pair<Range, JedisPool> jedisPair = getJedisPool(pk, jedisAddressesPoolMap);
    try {
      JedisPool jedisPool = jedisPair.getRight();
      jedis = jedisPool.getResource();
      return redisConsumer.apply(jedis);
    } catch (JedisConnectionException jcex) {
      LOG.error("Error while getting connection to redis for range {}", jedisPair.getLeft(), jcex);
      throw new JedisConnectionException(
          "Error while getting connection to redis for range " + jedisPair.getLeft() + ". "
              + jcex.getMessage(), jcex);
    } finally {
      Optional.ofNullable(jedis).ifPresent(Jedis::close);
    }
  }


  public boolean allowCallsForEndpoint(String endpoint) {
    String secondKey = ENDPOINT_SECOND_USAGE + endpoint;
    String hourlyKey = ENDPOINT_HOURLY_USAGE + endpoint;
    String dailyKey = ENDPOINT_DAILY_USAGE + endpoint;
    String monthlyKey = ENDPOINT_MONTHLY_USAGE + endpoint;

    EndpointLimit endpointLimit = consensusAppProperties.getRateLimit().getEndpoint();

    return redisOperation(1, jedis -> //
        allowCalls(jedis, secondKey, 1L, endpointLimit.requestPerSecond()) && //
            allowCalls(jedis, hourlyKey, HOUR_IN_SEC, endpointLimit.requestPerHour()) && //
            allowCalls(jedis, dailyKey, 86400, endpointLimit.requestPerDay()) && //
            allowCalls(jedis, monthlyKey, 2678400, endpointLimit.requestPerMonth()) //
    );
  }

  private boolean allowCalls(Jedis jedis, String key, long keyExpiry,
      long maxAllowedCalls) {
    long currentUsage = incrementKeyAndSetExpiry(jedis, key, keyExpiry);
    boolean result = currentUsage <= maxAllowedCalls;
    if (!result) {
      discordService.sendFeedbackToDiscordChannel("Rate limit reached. Key: " + key);
    }
    return result;
  }

  private static long incrementKeyAndSetExpiry(Jedis jedis, String secondKey,
      long expiryInSeconds) {
    long currentRPS = jedis.incr(secondKey);
    if (currentRPS == 1) {
      // If key got set just now, set expiry to 1sec
      jedis.expire(secondKey, expiryInSeconds);
    }
    return currentRPS;
  }
}
