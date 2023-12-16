package com.demystify_network.backend.service;

import static com.demystify_network.backend.util.Util.TS_FORMATTER;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;

import com.demystify_network.backend.api.request.AddressRequest;
import com.demystify_network.backend.api.response.ProScoreKnowledgeGraphResponse;
import com.demystify_network.backend.api.response.TraceResponse;
import com.demystify_network.backend.api.response.TraceResponse.NodeType;
import com.demystify_network.backend.dao.AddressRedisComponent;
import com.demystify_network.backend.dao.TransactionsRedisComponent;
import com.demystify_network.backend.model.Insights;
import com.demystify_network.backend.model.Node;
import com.demystify_network.backend.util.AddressType;
import com.demystify_network.backend.util.TransactionType;

@Service
public class TrustlessProServiceImpl extends TrustlessServiceBase implements TrustlessProService {

  public TrustlessProServiceImpl(EtherScanComponent etherscanComponent,
      TransactionsRedisComponent transactionsRedisComponent,
      AddressRedisComponent addressRedisComponent) {
    super(etherscanComponent, transactionsRedisComponent, addressRedisComponent);
  }

  @Override
  public ProScoreKnowledgeGraphResponse proScore(AddressRequest request, Insights insights) {
    ProScoreKnowledgeGraphResponse response = new ProScoreKnowledgeGraphResponse();
    response.address = request.address;
    Node rootNodeIncome = addressRedisComponent.getAddressPk(request.address, insights);
    Node rootNodeExp = addressRedisComponent.getAddressPk(request.address, insights);

    insights.add("addressRedisComponent.getAddressPk");
    if (rootNodeIncome == null || rootNodeExp == null) {
      return response;
    }
    // fetch balance in a thread
    CompletableFuture<Optional<BigInteger>> balFuture =
        retrieveBalance(request.address, request.skipBalCall);
    insights.add("retrieveBalanceThreadCreated");

    Map<String, Node> rawExpenditures = trace(rootNodeExp, TransactionType.DEBIT, insights);
    insights.add("expenditureTraceComplete");
    insights.resetAddressRedisCount();

    Map<String, Node> rawIncomes = trace(rootNodeIncome, TransactionType.CREDIT, insights);
    insights.add("incomeTraceComplete");

    List<Node> expenditures= dedupDestinationNodes(rootNodeExp, rawExpenditures);
    List<Node> incomes = dedupDestinationNodes(rootNodeIncome, rawIncomes);
    dumpNodes(expenditures, incomes);

    populateBalance(response, balFuture);

    updateRootResponse(rootNodeExp, response);
    updateRiskScores(incomes, expenditures, response);

    TraceResponse tR = new TraceResponse();
    addLeavesByRisk(incomes, tR, TransactionType.CREDIT);
    addLeavesByRisk(expenditures, tR, TransactionType.DEBIT);

    response.transactionTraces = tR;
    //dump(rootNodeExp, "");
    //dump(rootNodeIncome, "");
    return response;
  }

  private List<Node> dedupDestinationNodes(Node rootNode, Map<String, Node> destinations) {
    Map<Integer, Node> uniqueDestinationNodes = new HashMap<>();
    for (var entry : destinations.entrySet()) {
      Node currentNode = entry.getValue();
      //This condition removes Cyclic reference back to root.
      if(currentNode.intPk != rootNode.intPk) {
        // combine destination nodes.
        if(uniqueDestinationNodes.containsKey(currentNode.intPk)) {
          Node _tempNode = uniqueDestinationNodes.get(currentNode.intPk);
          currentNode.maxAmountToDestFromRootPlusOne +=  _tempNode.maxAmountToDestFromRootPlusOne;
          currentNode.directTransferFromRootToDest += _tempNode.directTransferFromRootToDest;
          currentNode.sumMinAmountsFormAllPaths += _tempNode.sumMinAmountsFormAllPaths;
          currentNode.indirectPathCount += _tempNode.indirectPathCount;
        } 
        else {
          uniqueDestinationNodes.put(currentNode.intPk, currentNode);
        }       
      }
    }
    LOG.debug(String.format("Destinations: %d, After Dedup: %d", destinations.size(), uniqueDestinationNodes.size()));
    return new ArrayList<Node>(uniqueDestinationNodes.values());
  }

  void updateRootResponse(Node root, ProScoreKnowledgeGraphResponse response) {
    response.firstTransactionTimestamp =
        TS_FORMATTER.format(root.firstTxnTimeStamp);
    response.category =
        root.flags != 0
            ? AddressType.flagsToString(root.flags)
            : "";
    response.tags = root.tags;
    response.socialMediaReports = root.socialMediaReport;
  }

  private void addLeavesByRisk(List<Node> destinations, TraceResponse tR, TransactionType txnType) {
    LOG.debug("RISK " + txnType.toString());
    int i = 0;
    List<Node> riskyDestinations = new ArrayList<>();
    for(Node dest: destinations)
      if(AddressType.calculateRiskBand(dest.flags, txnType) == AddressType.RiskBand.RED)
        riskyDestinations.add(dest);

    Collections.sort(riskyDestinations, (a, b) -> (int)Math.round((b.directTransferFromRootToDest + b.sumMinAmountsFormAllPaths) - (a.directTransferFromRootToDest + a.sumMinAmountsFormAllPaths)) );

    for(Node riskyDest: riskyDestinations) {
      tR.addAddressNode(i, 
        riskyDest.address,
        riskyDest.tags, 
        populateCategory(riskyDest),
        txnType == TransactionType.CREDIT ? NodeType.TOP_CREDITS_BY_RISK : NodeType.TOP_DEBITS_BY_RISK,
        String.format("%.2f", riskyDest.directTransferFromRootToDest),
        String.format("%.2f", riskyDest.sumMinAmountsFormAllPaths), 
        riskyDest.indirectPathCount
        );
      if (i >= 25) {
        break;
      }
    }
  }

  private void dump(Node root, String basePath) {
    basePath += String.format("%s, %.6f - %.6f - %.6f - %.6f - %.6f - %d,",root.address, root.directAmountFromParent, root.minAmountInCurrentPathSoFar
		    ,root.maxAmountToDestFromRootPlusOne, root.directTransferFromRootToDest, root.sumMinAmountsFormAllPaths, root.indirectPathCount); 
    if(root.nodeType == com.demystify_network.backend.model.Node.NodeType.DESTINATION) {
      LOG.debug(basePath + "\n");
      return;
    }
    for(Node current: root.nextNodes) {
      dump(current, basePath);
    }
  }

  private void dumpNodes(List<Node> expenditures, List<Node> incomes) {
    String log = "";
    for (Node currentNode : expenditures)
      log += String.format("%s, %d, %.6f, %.6f, %.6f, %.6f, %.6f, %d\n",currentNode.address, currentNode.flags, currentNode.directAmountFromParent, currentNode.minAmountInCurrentPathSoFar
    		    ,currentNode.maxAmountToDestFromRootPlusOne, currentNode.directTransferFromRootToDest, currentNode.sumMinAmountsFormAllPaths, currentNode.indirectPathCount); 
    for (Node currentNode : incomes)
      log += String.format("%s, %d, %.6f, %.6f, %.6f, %.6f, %.6f, %d\n",currentNode.address, currentNode.flags, currentNode.directAmountFromParent, currentNode.minAmountInCurrentPathSoFar
    		    ,currentNode.maxAmountToDestFromRootPlusOne, currentNode.directTransferFromRootToDest, currentNode.sumMinAmountsFormAllPaths, currentNode.indirectPathCount); 

    LOG.debug(log );
  }

}
