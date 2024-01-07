package com.demystify.network.backend.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.demystify.network.backend.util.LoggerExtension;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@WireMockTest
class EtherScanComponentTest {

  public static final HttpClient httpClient = HttpClient
      .newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .connectTimeout(Duration.ofSeconds(2))
      .build();
  private static final String key = "apiKey";
  private static EtherScanComponent etherscanComponent;

  @RegisterExtension
  static LoggerExtension loggerExtension = new LoggerExtension(
      EtherScanComponent.class
  );

  @BeforeAll
  static void beforeAll(WireMockRuntimeInfo wireMockRuntimeInfo) {
    ObjectMapper om = new ObjectMapper();
    om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    om.registerModule(new Jdk8Module());
    om.registerModule(new JavaTimeModule());

    etherscanComponent = new EtherScanComponent(httpClient, om, key,
        wireMockRuntimeInfo.getHttpBaseUrl());
  }

  @Test
  @DisplayName("Should be able to query balance api and return balance for ETH")
  void shouldBeAbleToQueryBalanceApiAndReturnBalanceForEth(WireMockRuntimeInfo wireMockRuntimeInfo)
      throws Exception {

    WireMock wireMock = wireMockRuntimeInfo.getWireMock();
    wireMock.register(stubFor(
        get(
            "/?module=account&action=balance&tag=latest&address=OKADDRESS&apikey=" +
                key
        )
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(getResponseBody("okaddress.json"))
            )
    ));
    Optional<BigInteger> okaddress = etherscanComponent.fetchBalance(
        "OKADDRESS",
        false);
    okaddress.ifPresentOrElse(
        balance -> {
          assertEquals(new BigInteger("12962475894090771"), balance);
        },
        () -> fail("Expected to receive ETH balance")
    );
  }

  @Test
  @DisplayName("Should return empty when request times out")
  void shouldReturnEmptyWhenRequestTimesOut(WireMockRuntimeInfo wireMockRuntimeInfo) {
    WireMock wireMock = wireMockRuntimeInfo.getWireMock();
    wireMock.register(stubFor(
        get(
            "/?module=account&action=balance&tag=latest&address=TIMEOUT&apikey=" +
                key
        )
            .willReturn(aResponse().withStatus(200).withFixedDelay(1001))
    ));
    Optional<BigInteger> timedOutResp = etherscanComponent.fetchBalance(
        "TIMEOUT",
        false);

    timedOutResp.ifPresentOrElse(
        bal -> fail("Should not have received the balance for this test"),
        () -> {
          List<ILoggingEvent> logEvents = loggerExtension.getEvents();
          List<ILoggingEvent> errorEvents = logEvents
              .stream()
              .filter(event -> event.getLevel() == Level.ERROR)
              .toList();
          assertFalse(errorEvents.isEmpty());
          assertEquals(1, errorEvents.size());
          assertEquals(
              "Error fetching balance for address TIMEOUT. request timed out",
              errorEvents.get(0).getFormattedMessage()
          );
        }
    );
  }

  private static String getResponseBody(String responseBodyFile)
      throws Exception {
    Path path = Paths.get(
        ClassLoader
            .getSystemResource("referencefiles/" + responseBodyFile)
            .toURI()
    );
    return Files.readString(path);
  }
}
