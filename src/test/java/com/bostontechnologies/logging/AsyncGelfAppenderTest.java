package com.bostontechnologies.logging;

import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.PrintStream;

public class AsyncGelfAppenderTest {
    private static PrintStream originalSystemErrorStream;

    private final LoggingEvent event = new LoggingEvent();


    @BeforeClass
    public static void beforeClass() {
        originalSystemErrorStream = System.err;
        //System.setErr(new NullPrintStream());
    }

    @AfterClass
    public static void afterClass() {
        System.setErr(originalSystemErrorStream);
    }

    @Before
    public void setUpMockObjects() {
    }

    @Test
    public void testValidCreation() {
        final AsyncGelfAppender appender = new AsyncGelfAppender();
    }

}
