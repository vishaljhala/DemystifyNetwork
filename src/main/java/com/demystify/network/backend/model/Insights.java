package com.demystify.network.backend.model;

import java.util.ArrayList;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Insights {

  public static final Logger LOG = LoggerFactory.getLogger(Insights.class);
  private final ArrayList<Pair<String, Long>> insights = new ArrayList<>();
  private Long addressRedisQueryTime = 0L;
  private Long txnRedisQueryTime = 0L;
  private Long addressRedisQueryCount = 0L;
  private Long addressPipelineQueryCount = 0L;
  private Long txnRedisQueryCount = 0L;
  private Long cumAdddressRedisQueryCount = 0L;
  
  public void addAddressRedisQueryTime(Long start) {
    addressRedisQueryTime += System.currentTimeMillis() - start;
    addressRedisQueryCount += 1;
  }

  public void addAddressRedisCount(Integer cnt) {
    addressRedisQueryCount += cnt;
  }
  public Long getAddressRedisCount() {
    return addressRedisQueryCount;
  }

  public void resetAddressRedisCount() {
    cumAdddressRedisQueryCount += addressRedisQueryCount;
    addressRedisQueryCount = 0L;
  }

  public void addAddressPipelineQueryTime(Long start) {
    addressRedisQueryTime += System.currentTimeMillis() - start;
    addressPipelineQueryCount += 1;
  }

  public void addTxnRedisQueryTime(Long start) {
    txnRedisQueryTime += System.currentTimeMillis() - start;
    txnRedisQueryCount += 1;
  }

  public void add(String step) {
    insights.add(Pair.of(step, System.currentTimeMillis()));
  }

  public String toString() {
    String str = insights.stream()
        .map(pair -> pair.getLeft() + "," + pair.getRight())
        .collect(Collectors.joining(","));
    str += ",addressRedisQueryTime," + addressRedisQueryTime + ",";
    str += "addressRedisQueryCount," + (cumAdddressRedisQueryCount + addressRedisQueryCount) + ",";
    str += "addressPipelineQueryCount," + addressPipelineQueryCount + ",";
    str += "txnRedisQueryTime," + txnRedisQueryTime + ",";
    str += "txnRedisQueryCount," + txnRedisQueryCount + ",";
    str += "avgAddressRedisQueryTime," + (addressRedisQueryTime / (double)(cumAdddressRedisQueryCount + addressRedisQueryCount)) + ",";
    str += "avgTxnRedisQueryTime," + (txnRedisQueryTime / (double)txnRedisQueryCount) + ",";
    return str;
  }
}
