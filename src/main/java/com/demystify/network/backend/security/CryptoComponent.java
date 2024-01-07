package com.demystify.network.backend.security;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CryptoComponent {

  private static final Logger LOG = LoggerFactory.getLogger(CryptoComponent.class);
  public static final String ALGORITHM = "AES/CBC/PKCS5Padding";
  private final SecretKey encryptionKey;
  private final IvParameterSpec iv;

  public CryptoComponent(SecretKey encryptionKey, IvParameterSpec iv) {
    this.encryptionKey = encryptionKey;
    this.iv = iv;
  }

  public String encrypt(String input) {
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, iv);
      byte[] cipherText = cipher.doFinal(input.getBytes());
      return Base64.getEncoder().encodeToString(cipherText);
    } catch (Exception e) {
      LOG.error("Error while encrypting", e);
      throw new RuntimeException(e);
    }
  }

  public String decrypt(String cipherText) {
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, encryptionKey, iv);
      byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
      return new String(plainText);
    } catch (Exception e) {
      LOG.error("Error while decrypting", e);
      throw new RuntimeException(e);
    }
  }
}
