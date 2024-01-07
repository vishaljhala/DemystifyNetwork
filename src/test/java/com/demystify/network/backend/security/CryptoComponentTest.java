package com.demystify.network.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.demystify.network.backend.config.CryptoConfig;
import javax.crypto.IllegalBlockSizeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    assertThat(plainText).isEqualTo(cc.decrypt(cc.encrypt(plainText)));
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
    assertThat(rex.getCause() instanceof IllegalBlockSizeException).isTrue();
    IllegalBlockSizeException ibsEx = (IllegalBlockSizeException) rex.getCause();
    assertThat(ibsEx.getMessage()).isEqualTo(
        "Input length must be multiple of 16 when decrypting with padded cipher");

    rex =
        assertThrows(
            RuntimeException.class,
            () -> cc.decrypt(encryptedValue + "smarty pants")
        );

    assertThat(rex.getCause() instanceof IllegalArgumentException).isTrue();
    IllegalArgumentException iaEx = (IllegalArgumentException) rex.getCause();
    assertThat(iaEx.getMessage()).isEqualTo("Input byte array has incorrect ending byte at 24");
  }
}
