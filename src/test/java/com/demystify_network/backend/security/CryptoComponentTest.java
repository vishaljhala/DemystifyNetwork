package com.demystify_network.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.crypto.IllegalBlockSizeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.demystify_network.backend.config.CryptoConfig;
import com.demystify_network.backend.security.CryptoComponent;

class CryptoComponentTest {

  private CryptoComponent cc;

  @BeforeEach
  void setUp() throws Exception {
    CryptoConfig config = new CryptoConfig();
    cc = new CryptoComponent(config.encryptionKey(), config.iv());
  }

  @Test
  @DisplayName("Should be able to encrypt and decrypt")
  void shouldBeAbleToEncryptAndDecrypt() {
    String plainText = "blah blah";
    assertEquals(cc.decrypt(cc.encrypt(plainText)), plainText);
  }

  @Test
  @DisplayName("Decryption should fail for tempered encrypted value")
  void decryptionShouldFailForTemperedEncryptedValue() {
    String plainText = "blah blah";
    String encryptedValue = cc.encrypt(plainText);
    System.out.println(encryptedValue);
    RuntimeException rex = assertThrows(
        RuntimeException.class,
        () ->
            cc.decrypt(encryptedValue.substring(0, encryptedValue.length() - 10))
    );
    assertTrue(rex.getCause() instanceof IllegalBlockSizeException);
    IllegalBlockSizeException ibsEx = (IllegalBlockSizeException) rex.getCause();
    assertEquals(
        "Input length must be multiple of 16 when decrypting with padded cipher",
        ibsEx.getMessage()
    );

    rex =
        assertThrows(
            RuntimeException.class,
            () -> cc.decrypt(encryptedValue + "smarty pants")
        );

    assertTrue(rex.getCause() instanceof IllegalArgumentException);
    IllegalArgumentException iaEx = (IllegalArgumentException) rex.getCause();
    assertEquals(
        "Input byte array has incorrect ending byte at 24",
        iaEx.getMessage()
    );
  }
}
