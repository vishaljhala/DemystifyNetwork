package com.demystify_network.backend.api.response;

public class ProScoreKnowledgeGraphResponse {

  public String balance = "";
  public String address;
  public String category = "";
  public String tags = "";
  public String socialMediaReports = "";
  public String firstTransactionTimestamp;
  public String[] percentTransactionByRisk = new String[3];
  public String[] percentIndirectTransactionByRisk = new String[3];

  public String riskScore = "";
  public String indirectRiskScore = "";

  public TraceResponse transactionTraces;
}
