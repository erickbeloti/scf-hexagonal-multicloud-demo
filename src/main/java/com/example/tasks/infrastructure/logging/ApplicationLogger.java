package com.example.tasks.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLogger {

    private static final Logger log = LoggerFactory.getLogger("TaskApplication");

    public void error(String message, Object... args) {
        log.error(message, args);
    }

    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    public void warn(String message, Object... args) {
        log.warn(message, args);
    }

    public void info(String message, Object... args) {
        log.info(message, args);
    }

    public void debug(String message, Object... args) {
        log.debug(message, args);
    }
}
