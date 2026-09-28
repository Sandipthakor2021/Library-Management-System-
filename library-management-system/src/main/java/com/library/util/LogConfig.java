package com.library.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Configures java.util.logging (part of the JDK, so the project has zero dependencies):
 * one-line records on stdout. Pass Level.FINE to see search and event-bus tracing.
 */
public final class LogConfig {
    private LogConfig() { }

    public static void init(Level level) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tH:%1$tM:%1$tS %4$-7s [%3$s] %5$s%6$s%n");
        LogManager.getLogManager().reset();
        Handler handler = new ConsoleHandler() {
            { setOutputStream(System.out); }
        };
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(level);
        root.addHandler(handler);
    }
}
