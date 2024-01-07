package com.demystify.network.backend.model;

import com.demystify.network.backend.util.Util;

public class Edge {
  public Edge(byte[] _destBytePk, String _strAmount) {
    destBytePk = _destBytePk;
    destIntPk = Util.bytesToInteger(destBytePk);
    strAmount = _strAmount;
  }
  public byte[] destBytePk;
  public String strAmount;
  public Integer destIntPk;
}
