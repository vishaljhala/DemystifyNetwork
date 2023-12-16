package com.demystify_network.backend.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

public final class LoggerExtension
    implements BeforeEachCallback, AfterEachCallback {

  private Logger logger;
  private ListAppender<ILoggingEvent> appender;
  private final String loggerName;
  private final Level level;

  public LoggerExtension() {
    this(Level.ALL, Logger.ROOT_LOGGER_NAME);
  }

  public LoggerExtension(Class<?> loggerClass) {
    this(Level.ALL, loggerClass.getCanonicalName());
  }

  public LoggerExtension(Level level, Class<?> loggerClass) {
    this(level, loggerClass.getCanonicalName());
  }

  public LoggerExtension(Level level, String loggerName) {
    this.loggerName = loggerName;
    this.level = level;
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    appender = new ListAppender<>();
    logger = (Logger) LoggerFactory.getLogger(loggerName);
    logger.addAppender(appender);
    logger.setLevel(level);
    appender.start();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    logger.detachAppender(appender);
  }

  public List<ILoggingEvent> getEvents() {
    if (appender == null) {
      throw new RuntimeException(
          "LoggerExtension needs to be annotated with @RegisterExtension"
      );
    }

    return appender.list;
  }
}
