package com.demystify_network.backend.util;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public enum AddressType {
  CONTRACT(1 << 0),
  DEPLOYER(1 << 1),
  EXCHANGE(1 << 2),
  WALLET(1 << 3),
  STAKER(1 << 4),
  MINER(1 << 5),
  DEFI(1 << 6),
  PAYMENTS(1 << 7),
  BRIDGE(1 << 8),
  NOT_DEFINED9(1 << 9),
  NOT_DEFINED10(1 << 10),
  NOT_DEFINED11(1 << 11),
  NOT_DEFINED12(1 << 12),
  NOT_DEFINED13(1 << 13),
  NOT_DEFINED14(1 << 14),
  NOT_DEFINED15(1 << 15),
  NOT_DEFINED16(1 << 16),
  GAME(1 << 17),
  VICTIM(1 << 18),
  CROSSCHAIN(1 << 19),
  LAYER2(1 << 20),
  FLASHBOT(1 << 21),
  P2PEXCHANGE(1 << 22),
  SPAMMER(1 << 23),
  LOTTERY(1 << 24),
  GAMBLING(1 << 25),
  MIXER(1 << 26),
  EXPLOITER(1 << 27),
  ADULT(1 << 28),
  HACKER(1 << 29),
  BLOCKEDEXPLOITER(1 << 30),
  US_GOV_BLOCKED(1 << 31);

  private final int type;

  public enum RiskBand {
    GREEN,
    YELLOW,
    RED,
  }

  AddressType(int type) {
    this.type = type;
  }

  public int getType() {
    return type;
  }

  public static String flagsToString(Integer flags) {
    return IntStream.range(0, 32)
        .filter(i -> ((flags & (1 << i)) != 0))
        .mapToObj(i -> values()[i].name())
        .collect(Collectors.joining(","));
  }

  public static RiskBand calculateRiskBand(Integer flags, TransactionType flagType) {
    Integer riskScore = calculateRiskScore(flags, flagType);
    if (riskScore <= 5) {
      return RiskBand.GREEN;
    } else if (riskScore <= 7) {
      return RiskBand.YELLOW;
    } else {
      return RiskBand.RED;
    }
  }

  public static Integer calculateRiskScore(Integer flags, TransactionType flagType) {
    Integer mask = 0;
    Integer riskScore = 0;

    for (int i = 31; i >= 0; i--) {
      mask = (1 << i);
      if (mask == (flags & mask)) {
        Integer _riskScore = scoreForFlaggedBit(i, flagType);
	if (_riskScore > riskScore) {
          riskScore = _riskScore;
        }
      }
    }
    return riskScore;
  }

  private static Integer scoreForFlaggedBit(Integer flaggedBit, TransactionType flagType) {
    switch (flaggedBit) {
      case 0: // Contract
        return 0;
      case 1: // Deployer
        return 0;
      case 2: // Exchange
        return 0;
      case 3: // Wallet
        return 0;
      case 4: // Staker
        return 0;
      case 5: // Miner
        return 3;
      case 6: // DEFI
        return 3;
      case 7: // payments
        return 3;
      case 8: // bridge
        return 3;
      case 9:
        return 0;
      case 10:
        return 0;
      case 11:
        return 0;
      case 12:
        return 0;
      case 13:
        return 0;
      case 14:
        return 0;
      case 15:
        return 0;
      case 16:
        return 0;
      case 17: // game
        return 5;
      case 18: // victim
        return 5;
      case 19: // Crosschain
        return 5;
      case 20: // Layer 2
        return 6;
      case 21: // Flashbot
        return 6;
      case 22: // P2PExchange
        return 6;
      case 23: // spammer
        return 6;
      case 24: // Lottery
        return 6;
      case 25: // Gambling
        // if (flagType == TransactionType.DEBIT) return 6;
        return 7;
      case 26: // mixer
        // if (flagType == TransactionType.DEBIT) return 6;
        return 8;
      case 27: // Exploiter
        // if (flagType == TransactionType.DEBIT) return 6;
        return 9;
      case 28: // adult
        return 10;
      case 29: // Hacker
        // if (flagType == TransactionType.DEBIT) return 6;
        return 10;
      case 30: // BlockedExploiter
        // if (flagType == TransactionType.DEBIT) return 6;
        return 10;
      case 31: // us_gov_blocked
        return 10;
      default:
        return 0;
    }
  }
}
