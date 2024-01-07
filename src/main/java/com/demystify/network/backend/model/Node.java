package com.demystify.network.backend.model;

import com.demystify.network.backend.util.Util;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.codec.binary.Hex;
import redis.clients.jedis.Response;

public class Node {
  public enum NodeType {
      ROOT,
      INTERIM,
      DESTINATION,
  }

  public Node(Integer _intPk, NodeType _nodeType, Node _parent) {
    this.intPk = _intPk;
    this.bytePk = Util.integerTo4Bytes(intPk);
    this.nodeType = _nodeType;
    setup(_parent);
  }

  public Node(byte[] _pk, NodeType _nodeType, Node _parent) {
    this.bytePk = _pk;
    this.intPk = Util.bytesToInteger(_pk);
    this.nodeType = _nodeType;
    setup(_parent);
  }
  private void setup(Node _parent) {
    if(_parent != null) {
      this.uniquePathKey = _parent.uniquePathKey + "_" + Integer.toString(intPk);
      for(Integer _visitedPk: currentPathVisitedNodes){
        this.currentPathVisitedNodes.add(_visitedPk);
      }
    }
    else {
      this.uniquePathKey = Integer.toString(intPk);
      this.currentPathVisitedNodes.add(intPk);
    }
  }
  // public static Node copy(Node _node) {
  //   Node _tempNode = new Node(_node.intPk, _node.nodeType, null);
  //   _tempNode.address = _node.address;
  //   _tempNode.flags = _node.flags;
  //   _tempNode.tags = _node.tags;
  //   _tempNode.lastSocialMediaReport = _node.lastSocialMediaReport;
  //   _tempNode.firstTxnTimeStamp = _node.firstTxnTimeStamp;
  //   //TODO: Deep copy bug
  //   _tempNode.transactionShards = _node.transactionShards;
  //   return _tempNode;
  // } 

  public void parseAddressValues(Boolean isRootNode) {
    Map<byte[], byte[]> fields = redisResponseForAddressQuery.get();
    if(fields != null)
    for (Map.Entry<byte[], byte[]> entry : fields.entrySet()) {
      byte[] k = entry.getKey();
      byte[] v = entry.getValue();
      if (k.length == 1 && k[0] == 'a') {
        this.address = Hex.encodeHexString(v);
      } else if (k.length == 1 && k[0] == 'f') {
        byte[] _flags = Arrays.copyOf(v, 4);
        flags = ByteBuffer.wrap(_flags).getInt();
        if(flags != 0)
          nodeType = NodeType.DESTINATION;

      } else if (k.length == 1 && k[0] == 't') {
        tags = new String(v, StandardCharsets.UTF_8);
      } else if (k.length == 1 && k[0] == 's') {
        socialMediaReport = new String(v, StandardCharsets.UTF_8);
      } else if (k.length == 4 && v.length == 10) {
        String strFirstTransactionTimeStamp = new String(v, StandardCharsets.UTF_8);
        try {
          final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
          sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
          Instant instant = sdf.parse(strFirstTransactionTimeStamp).toInstant();
          if (firstTxnTimeStamp == null) {
            firstTxnTimeStamp = instant;
          } else if (firstTxnTimeStamp.isAfter(instant)) {
            firstTxnTimeStamp = instant;
          }
        } catch (ParseException e) {
        }
        transactionShards.add(new String(k, StandardCharsets.UTF_8));
      }
    }
  }


//SET BY CONSTRUCTOR
  public byte[] bytePk;
  public Integer intPk;
  public NodeType nodeType;	
  public String uniquePathKey;
	public Set<Integer> currentPathVisitedNodes = new HashSet<>();
  
//SET BY PARSE METHOD AFTER READING FROM REDIS
  public String address = "";
  public Integer flags = 0;
  public String tags = "";
  public String socialMediaReport = "";
  public Instant firstTxnTimeStamp = null;
  public List<String> transactionShards = new ArrayList<>();
  public Response<Map<byte[], byte[]>> redisResponseForAddressQuery = null;

//LOGICAL SETUP IN TRACE
  public List<Node> nextNodes = new ArrayList<>();
  
//SET FOR ALL NODES
  // Between any two nodes (from parent)
	public Double directAmountFromParent = 0.0;
  //For All Interim nodes.
  public Double minAmountInCurrentPathSoFar = 0.0;

//SET FOR INTERIM AND DESTINATION NODES
  //Max amount from Root to Root + 1. This is upper cap for sumMinAmountsFromAllPaths.
  public Double maxAmountToDestFromRootPlusOne = 0.0;

//ONLY FOR DESTINATION
  // Directly from root to Destination
	public Double directTransferFromRootToDest = 0.0;
  //Only for Destination. Sum of all minimum amounts leading from Root + 1 Node to Destination (indirect amount)
	public Double sumMinAmountsFormAllPaths = 0.0;
	//Only for Destination. Indirect paths from Root + 1 to Destination.
  public Integer indirectPathCount = 0;
}