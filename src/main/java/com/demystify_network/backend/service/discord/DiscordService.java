package com.demystify_network.backend.service.discord;

public interface DiscordService {

  void sendExceptionToDiscordChannel(String exceptionDetails);

  void sendFeedbackToDiscordChannel(String exceptionDetails);
}
