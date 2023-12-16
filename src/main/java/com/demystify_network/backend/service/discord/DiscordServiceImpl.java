package com.demystify_network.backend.service.discord;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@EnableAsync
@Profile("production")
public class DiscordServiceImpl implements DiscordService {

  public static final Logger LOG = LoggerFactory.getLogger(
      DiscordServiceImpl.class
  );

  @Value("${discord.exception.webhook.url}")
  private String discordExceptionWebhookUrl;

  @Value("${discord.feedback.webhook.url}")
  private String discordFeedbackWebhookUrl;

  @Async
  public void sendExceptionToDiscordChannel(String exceptionDetails) {
    try {
      DiscordWebhook discordWebhook = DiscordWebhook.getExceptionDiscordHandler(
          discordExceptionWebhookUrl, "Consensusapps Exception");
      discordWebhook.sendData(exceptionDetails);
    } catch (IOException ioex) {
      LOG.error("Original exception details:" + exceptionDetails);
      LOG.error("Error in sending message to discord channel", ioex);
    }
  }

  @Async
  public void sendFeedbackToDiscordChannel(String feedbackDetails) {
    try {
      DiscordWebhook discordWebhook = DiscordWebhook.getFeedbackDiscordHandler(
          discordFeedbackWebhookUrl, "Consensusapps Feedback");
      discordWebhook.sendData(feedbackDetails);
    } catch (Exception exception) {
      LOG.error("Error in sending message to discord channel", exception);
    }
  }
}
