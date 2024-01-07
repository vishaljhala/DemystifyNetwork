package com.demystify.network.backend.util;

public enum TransactionType {
  ANY(0),
  CREDIT(1),
  DEBIT(2);

  private final int type;

  TransactionType(int type) {
    this.type = type;
  }

  public int getType() {
    return type;
  }
}
