package com.demystify_network.backend.util;

import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HexFormat;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;

public class Util {

  public static final DateTimeFormatter TS_FORMATTER =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy").withZone(ZoneOffset.UTC);
  public static final String ETH_ADD_REGEX = "^0x[0-9a-f]{40}$";

  public static Pair<String, String> toHashUUID(String data) {
    String uuid1 = String.format("%s-%s-%s-%s-%s", data.substring(2, 10), data.substring(10, 14),
        data.substring(14, 18), data.substring(18, 22), data.substring(22, 34));

    String uuid2 = String.format("%s-%s-%s-%s-%s", data.substring(34, 42), data.substring(42, 46),
        data.substring(46, 50), data.substring(50, 54), data.substring(54));
    return Pair.of(uuid1, uuid2);
  }

  public static Pair<String, Integer> toAddressUuidInt(String data) {
    String uuid1 = String.format("%s-%s-%s-%s-%s", data.substring(2, 10), data.substring(10, 14),
        data.substring(14, 18), data.substring(18, 22), data.substring(22, 34));
    String uuId = data.substring(34);
    Integer val = Integer.parseUnsignedInt(uuId, 16);
    return Pair.of(uuid1, val);
  }

  public static String toHashString(String uuid1, String uuid2) {
    return new StringBuilder("0x")
        .append(uuid1.replaceAll("-", ""))
        .append(uuid2.replaceAll("-", ""))
        .toString();
  }

  public static String toAddressString(String uuid, Integer value) {
    StringBuilder stringUuid = new StringBuilder("0x").append(uuid.replaceAll("-", ""));
    String hex = String.format("%08x", value);
    return stringUuid.append(hex).toString();
  }

  public static Pair<byte[], byte[]> integerToBytes(int i) {
    byte[] result1 = new byte[3];
    byte[] result2 = new byte[1];

    result1[0] = (byte) (i >> 24);
    result1[1] = (byte) (i >> 16);
    result1[2] = (byte) (i >> 8);
    result2[0] = (byte) (i /*>> 0*/);

    return Pair.of(result1, result2);
  }

  public static byte[] integerTo4Bytes(int i) {
    byte[] result1 = new byte[4];
    result1[0] = (byte) (i >> 24);
    result1[1] = (byte) (i >> 16);
    result1[2] = (byte) (i >> 8);
    result1[3] = (byte) (i /*>> 0*/);

    return result1;
  }

  public static Pair<byte[], byte[]> toEthAddressBytes(String address) {
    if (address.startsWith("0x") || address.startsWith("0X")) {
      address = address.substring(2);
    }
    if (address.length() != 40) {
      return null;
    }
    byte[] addBytes = HexFormat.of().parseHex(address);
    return Pair.of(Arrays.copyOfRange(addBytes, 0, 10), Arrays.copyOfRange(addBytes, 10, 20));
  }

  public static String toEthAddressString(byte[] address) {
    return Hex.encodeHexString(address);
  }

  public static Integer bytesToInteger(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getInt(0);
  }

  public static byte[] stringToIntBytes(String strPk) {
    return integerTo4Bytes(Integer.parseInt(strPk));
  }

  public static Integer safeParseInt(String strInt) {
    try {
      return Integer.parseInt(strInt);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public static Long safeParseLong(String strLong) {
    try {
      return Long.parseLong(strLong);
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  public static boolean validEthAddress(String ethereumAddress) {
    if (ethereumAddress.length() != 42) {
      return false;
    }
    return ethereumAddress.matches(ETH_ADD_REGEX);
  }

  public static byte[] toFromPk(byte[] oldPk) {
    byte[] newPk = new byte[5];
    newPk[0] = 0;
    for (int i = 1; i < 5; i++) {
      newPk[i] = oldPk[i - 1];
    }
    return newPk;
  }
}
