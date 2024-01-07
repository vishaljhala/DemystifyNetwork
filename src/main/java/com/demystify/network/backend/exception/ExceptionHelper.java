package com.demystify.network.backend.exception;

import com.demystify.network.backend.service.discord.DiscordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHelper {

  @Autowired
  private DiscordService discordService;

  @ExceptionHandler(value = {Exception.class})
  public void handleException(Exception exception) throws Exception {
    StackTraceElement[] stackTraceElements = exception.getStackTrace();

    StringBuilder exceptionData = new StringBuilder(
        exception.getClass() + ": " + exception.getMessage());

    int count = 0;
    boolean consensusappsElementFound = false;

    for (StackTraceElement stackTraceElement : stackTraceElements) {
      if (count >= 3 && consensusappsElementFound) {
        break;
      }

      String stackTraceElementStr = stackTraceElement.toString();
      if (!consensusappsElementFound) {
        consensusappsElementFound = stackTraceElementStr.startsWith("com.consensusapps");
      }

      if (count < 3 || consensusappsElementFound) {
        exceptionData.append(" at ").append(stackTraceElementStr);
      }

      count++;
    }
    discordService.sendExceptionToDiscordChannel(exceptionData.toString());
    throw exception;
  }
}
