package com.demystify.network.backend.service;


import com.demystify.network.backend.api.response.ProScoreKnowledgeGraphResponse;
import com.demystify.network.backend.dao.AddressRedisComponent;
import com.demystify.network.backend.dao.TransactionsRedisComponent;
import com.demystify.network.backend.model.Edge;
import com.demystify.network.backend.model.Insights;
import com.demystify.network.backend.model.Node;
import com.demystify.network.backend.model.Node.NodeType;
import com.demystify.network.backend.util.AddressType;
import com.demystify.network.backend.util.AddressType.RiskBand;
import com.demystify.network.backend.util.TransactionType;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// class Checker implements Comparator<Pair<String, Path>> {

//   public int compare(Pair<String, Path> a, Pair<String, Path> b) {
//     List<Double> aVals = a.getRight().pathAmounts;
//     List<Double> bVals = b.getRight().pathAmounts;
//     if (aVals == null || bVals == null || bVals.size() == 0 || bVals.size() == 0) {
//       return 1;
//     }
//     try {
//       if (aVals.get(aVals.indexOf(Collections.min(aVals))) >= bVals.get(
//           bVals.indexOf(Collections.min(bVals)))) {
//         return -1;
//       }
//     } catch (Exception e) {
//     }
//     return 1;
//   }
// }

public abstract class TrustlessServiceBase {

  protected static final Logger LOG = LoggerFactory.getLogger(TrustlessServiceBase.class);

  protected final EtherScanComponent etherscanComponent;
  protected final TransactionsRedisComponent transactionsRedisComponent;
  protected final AddressRedisComponent addressRedisComponent;
  static final int MAX_PATHS_TO_SCAN = 20000;
  public static final int MAX_ADDRESSES_TO_SCAN = 25000;

  public TrustlessServiceBase(EtherScanComponent etherscanComponent,
      TransactionsRedisComponent transactionsRedisComponent,
      AddressRedisComponent addressRedisComponent) {
    this.etherscanComponent = etherscanComponent;
    this.transactionsRedisComponent = transactionsRedisComponent;
    this.addressRedisComponent = addressRedisComponent;
  }

  protected Map<String, Node>  trace(Node rootNode, TransactionType direction,
      Insights insights) {
    Set<String> visitedPaths = new HashSet<>();
    Queue<Node> currentLevelPathsToExplore = new LinkedList<>();
    //Here String Key = PK of root_1 node + "_" + PK of destination Node
    Map<String, Node> destinationNodes = new HashMap<>();

    int level = 0;

    currentLevelPathsToExplore.add(rootNode);

    while (currentLevelPathsToExplore.size() > 0 && level < 5) {
      Queue<Node> nextLevelPathsToExplore = new LinkedList<>();

      while (currentLevelPathsToExplore.size() > 0) {
        Node node = currentLevelPathsToExplore.poll();
        visitedPaths.add(node.uniquePathKey);
        if (visitedPaths.size() > MAX_PATHS_TO_SCAN || insights.getAddressRedisCount()  > MAX_ADDRESSES_TO_SCAN) {
          break;
        }

        List<Edge> edges = queryNextNodesViaEdges(
            node.bytePk,
            direction, node.transactionShards, level == 0, insights);

        List<Node> nextNodes = new ArrayList<>();
        for(Edge edge:edges) {
          Node _tempNode = new Node(edge.destBytePk, NodeType.INTERIM, node);
          if(!visitedPaths.contains(_tempNode.uniquePathKey) && !_tempNode.currentPathVisitedNodes.contains(_tempNode.intPk)) {
            _tempNode.directAmountFromParent = Double.parseDouble(edge.strAmount);
            if(level > 0)
              _tempNode.minAmountInCurrentPathSoFar = Math.min(_tempNode.directAmountFromParent, node.minAmountInCurrentPathSoFar);
            else
              _tempNode.minAmountInCurrentPathSoFar = _tempNode.directAmountFromParent;
            nextNodes.add(_tempNode);
          }
        }
        addressRedisComponent.findByPKPipeline(nextNodes, insights);

        List<Node> noResponseNextNodes = new ArrayList<>();
        for (Node nextNode : nextNodes)
          if(nextNode.redisResponseForAddressQuery == null)
            noResponseNextNodes.add(nextNode);
        nextNodes.removeAll(noResponseNextNodes);

        for (Node nextNode : nextNodes) {
          nextNode.parseAddressValues(false);
          node.nextNodes.add(nextNode);
          if (nextNode.nodeType == NodeType.INTERIM) { 
            //Interim Node found
            if(level == 0)
              nextNode.maxAmountToDestFromRootPlusOne = nextNode.directAmountFromParent;
            else
              nextNode.maxAmountToDestFromRootPlusOne = node.maxAmountToDestFromRootPlusOne;
            if (level < 4) {
              nextLevelPathsToExplore.add(nextNode);
            }
          } else {
            //Destination found. 
            String[] path = nextNode.uniquePathKey.split("_");
            if(path.length == 2) {
              //Direct Transfer to Dest
              nextNode.maxAmountToDestFromRootPlusOne = nextNode.directAmountFromParent;

              String destinationKey = path[0] + "_" + path[1];
              nextNode.maxAmountToDestFromRootPlusOne = nextNode.directAmountFromParent;
              nextNode.directTransferFromRootToDest = nextNode.directAmountFromParent;

              destinationNodes.put(destinationKey, nextNode);
            } else {
              //Atleast 1 interin node in between. Indirect transfer with atleast 1 interim node in between
              String destinationKey = path[0] + "_" + path[1] + "_" + path[path.length - 1];
              if(destinationNodes.containsKey(destinationKey)) {
                Node oldDestinationNode = destinationNodes.get(destinationKey);
                oldDestinationNode.sumMinAmountsFormAllPaths += nextNode.minAmountInCurrentPathSoFar;
                oldDestinationNode.indirectPathCount += 1; 
              } else {
                nextNode.maxAmountToDestFromRootPlusOne = node.maxAmountToDestFromRootPlusOne;
                nextNode.sumMinAmountsFormAllPaths = nextNode.minAmountInCurrentPathSoFar;
                nextNode.indirectPathCount = 1;
                destinationNodes.put(destinationKey, nextNode);
              }
            }
          }
        }
      }
      currentLevelPathsToExplore = nextLevelPathsToExplore;
      level++;

      if (visitedPaths.size() > MAX_PATHS_TO_SCAN || insights.getAddressRedisCount() > MAX_ADDRESSES_TO_SCAN) {
        break;
      }
    }
    return destinationNodes;
  }

  protected List<Edge> queryNextNodesViaEdges(byte[] bytesPk,
      TransactionType direction, List<String> shards, boolean isRootNode, Insights insights) {

    if (direction == TransactionType.CREDIT) {
      return transactionsRedisComponent.findCreditTransactions(bytesPk, shards, isRootNode,
          insights);
    }

    return transactionsRedisComponent.findDebitTransactions(bytesPk, shards, isRootNode, insights);
  }

  protected void updateRiskScores(List<Node> incomes,
    List<Node> expenditures, ProScoreKnowledgeGraphResponse response) {
    // Integer[] incomeFlags = { 0, 0, 0, 0, 0, 0 };
    // Integer[] expenditureFlags = { 0, 0, 0, 0, 0, 0 };

    double directTransferTotal = 0.0;
    double indirectTransferTotal = 0.0;
    double directLegalFunds = 0.0;
    double indirectLegalFunds = 0.0;
    double[] directTransferRiskBands = new double[3];
    double[] indirectTransferRiskBands = new double[3];

    for (Node node : incomes) {
      RiskBand riskBand = AddressType.calculateRiskBand(node.flags,
          TransactionType.CREDIT);
      if (riskBand == RiskBand.GREEN) {
        directLegalFunds += node.directTransferFromRootToDest;
        directTransferRiskBands[0] += node.directTransferFromRootToDest;
        indirectLegalFunds += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
        indirectTransferRiskBands[0] += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
      } else if (riskBand == RiskBand.YELLOW) {
        directTransferRiskBands[1] += node.directTransferFromRootToDest;
        indirectTransferRiskBands[1] += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
      } else if (riskBand == RiskBand.RED) {
        directLegalFunds -= node.directTransferFromRootToDest;
        directTransferRiskBands[2] += node.directTransferFromRootToDest;
        indirectLegalFunds -= Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
        indirectTransferRiskBands[2] += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
      }
      directTransferTotal += node.directTransferFromRootToDest;
      indirectTransferTotal += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
    }
    for (Node node : expenditures) {
      RiskBand riskBand = AddressType.calculateRiskBand(node.flags,
          TransactionType.DEBIT);
      if (riskBand == RiskBand.GREEN) {
        directLegalFunds += node.directTransferFromRootToDest;
        directTransferRiskBands[0] += node.directTransferFromRootToDest;
        indirectLegalFunds += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
        indirectTransferRiskBands[0] += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
      } else if (riskBand == RiskBand.YELLOW) {
        directTransferRiskBands[1] += node.directTransferFromRootToDest;
        indirectTransferRiskBands[1] += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
      } else if (riskBand == RiskBand.RED) {
        directLegalFunds -= node.directTransferFromRootToDest;
        directTransferRiskBands[2] += node.directTransferFromRootToDest;
        indirectLegalFunds -= Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
        indirectTransferRiskBands[2] += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
      }
      directTransferTotal += node.directTransferFromRootToDest;
      indirectTransferTotal += Math.min(node.maxAmountToDestFromRootPlusOne, node.sumMinAmountsFormAllPaths);
    }
    for (int i = 0; i < response.percentTransactionByRisk.length; i++) {
      Double interim = (directTransferRiskBands[i] / directTransferTotal) * 100.0;
      if (interim.isInfinite() || interim.isNaN()) {
        interim = 0.0;
      }
      response.percentTransactionByRisk[i] = String.format("%.2f", interim);
      LOG.debug(
          "Direct Risk Amounts: " + i + " " + response.percentTransactionByRisk[i]);
    }
    for (int i = 0; i < response.percentIndirectTransactionByRisk.length; i++) {
      Double interim = (indirectTransferRiskBands[i] / indirectTransferTotal) * 100.0;
      if (interim.isInfinite() || interim.isNaN()) {
        interim = 0.0;
      }
      response.percentIndirectTransactionByRisk[i] = String.format("%.2f", interim);
      LOG.debug(
          "IndirectDirect Risk Amounts: " + i + " " + response.percentIndirectTransactionByRisk[i]);
    }
    
    double riskScore = 5.0;
    if (directTransferTotal > 0.0) {
      riskScore -= (directLegalFunds * 5.0) / directTransferTotal;
    }
    LOG.debug("Risk Score: " + riskScore + " legalFunds " + directLegalFunds + " cum "
        + directTransferTotal);

    riskScore = Math.max(riskScore, calcMinRiskByAge(response));
    //Highest precedence: if high risk entity, assign highest risk and return
    for (String flag : response.category.split(",")) {
      AddressType flagVal;
      try {
        flagVal = AddressType.valueOf(flag);
        if (flagVal == AddressType.US_GOV_BLOCKED ||
            flagVal == AddressType.BLOCKEDEXPLOITER ||
            flagVal == AddressType.HACKER ||
            flagVal == AddressType.EXPLOITER) {
          riskScore = 10;
        }
      } catch (IllegalArgumentException | NullPointerException e) {
      }
    }
    
    response.riskScore = String.format("%.2f", riskScore);

    double indirectRiskScore = 5.0;
    if (indirectTransferTotal > 0.0) {
      indirectRiskScore -= (indirectLegalFunds * 5.0) / indirectTransferTotal;
    }
    LOG.debug("Indirect Risk Score: " + indirectRiskScore + " indirectlegalFunds " + indirectLegalFunds + " cum "
        + indirectTransferTotal);

    indirectRiskScore = Math.max(indirectRiskScore, calcMinRiskByAge(response));
    response.indirectRiskScore = String.format("%.2f", indirectRiskScore);
  }

  protected double calcMinRiskByAge(ProScoreKnowledgeGraphResponse response) {
    // Code the rules in Ascending order of precendence.
    // Eg: Rule at bottom of function will override everything above it.

    double minScore = 4;

    if (StringUtils.isNotBlank(response.firstTransactionTimestamp)) {
      final SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      try {
        Instant result = sdf.parse(response.firstTransactionTimestamp).toInstant();
        ZonedDateTime firstTxnAt = result.atZone(ZoneOffset.UTC);
        ZonedDateTime now = ZonedDateTime.now();
        if (firstTxnAt.isBefore(now.minusYears(1))) {
          minScore = 3;
        }
        if (firstTxnAt.isBefore(now.minusYears(2))) {
          minScore = 2;
        }
        if (firstTxnAt.isBefore(now.minusYears(3))) {
          minScore = 0;
        }
        //LOG.info("Max Score: " + maxScore + " " + firstTxnAt.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
      } catch (ParseException e) {
      }
    }
    return minScore;
  }


  protected CompletableFuture<Optional<BigInteger>> retrieveBalance(
      String walletAddress, boolean skipBalCall) {
    return CompletableFuture
        .supplyAsync(() -> etherscanComponent.fetchBalance(walletAddress, skipBalCall))
        .thenApply(optBal -> optBal);
  }

  protected String populateCategory(Node node) {
    String category =
        node.flags != 0 ? AddressType.flagsToString(node.flags) : "";
    return category;
  }

  protected void populateBalance(ProScoreKnowledgeGraphResponse response,
      CompletableFuture<Optional<BigInteger>> balFuture) {
    balFuture
        .join()
        .ifPresentOrElse(
            bal -> response.balance = bal.toString(),
            () -> response.balance = "");
  }
}
