package com.demystify.network.backend.service.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!production")
public class DiscordServiceLocal implements DiscordService {

  private static final Logger LOG = LoggerFactory.getLogger(DiscordServiceLocal.class);

  @Override
  public void sendExceptionToDiscordChannel(String exceptionDetails) {
    LOG.info("Exception to be sent to Discord: {}", exceptionDetails);
  }

  @Override
  public void sendFeedbackToDiscordChannel(String exceptionDetails) {
    LOG.info("Feedback to be sent to Discord: {}", exceptionDetails);
  }
}
