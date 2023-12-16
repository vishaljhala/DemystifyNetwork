package com.demystify_network.backend.api.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class TraceResponse {

  public enum NodeType {
    TOP_CREDITS_BY_AMOUNT,
    TOP_CREDITS_BY_RISK,
    TOP_DEBITS_BY_AMOUNT,
    TOP_DEBITS_BY_RISK
  }

  public static class DirectTransfer {

    public String amount = "";
  }

  public static class IndirectTransfers {

    public int totalPaths;
    public String estimatedAmount;
  }

  public static class AddressNode {

    public String id;
    public String address;
    public String category;
    public String tags;
    public DirectTransfer directTransfer = new DirectTransfer();
    public IndirectTransfers indirectTransfers = new IndirectTransfers();
  }

  public void addAddressNode(
      Integer id,
      String address,
      String tags,
      String category,
      NodeType nodeType,
      String directTransferEstimatedAmount,
      String indirectTransferEstimatedAmount,
      int indirectTransferTotalPaths
  ) {
    AddressNode node = new AddressNode();
    node.address = address;
    node.tags = tags;
    node.category = category;
    node.id = id.toString();
    node.directTransfer.amount = directTransferEstimatedAmount;
    node.indirectTransfers.estimatedAmount = indirectTransferEstimatedAmount;
    node.indirectTransfers.totalPaths = indirectTransferTotalPaths;

    if (nodeType == NodeType.TOP_CREDITS_BY_AMOUNT) {
      topCreditsByAmount.add(node);
    }
    if (nodeType == NodeType.TOP_CREDITS_BY_RISK) {
      topCreditsByRisk.add(node);
    }
    if (nodeType == NodeType.TOP_DEBITS_BY_AMOUNT) {
      topDebitsByAmount.add(node);
    }
    if (nodeType == NodeType.TOP_DEBITS_BY_RISK) {
      topDebitsByRisk.add(node);
    }
  }

  @JsonIgnore
  public List<AddressNode> topCreditsByAmount = new ArrayList<>();
  public List<AddressNode> topCreditsByRisk = new ArrayList<>();
  @JsonIgnore
  public List<AddressNode> topDebitsByAmount = new ArrayList<>();
  public List<AddressNode> topDebitsByRisk = new ArrayList<>();
}
