package com.bostontechnologies.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.lmax.disruptor.EventFactory;

public class LogEvent {
    private ILoggingEvent iLoggingEvent;

    public ILoggingEvent getiLoggingEvent() {
        return iLoggingEvent;
    }

    public void setiLoggingEvent(ILoggingEvent iLoggingEvent) {
        this.iLoggingEvent = iLoggingEvent;
    }

    public final static EventFactory<LogEvent> EVENT_FACTORY = new EventFactory<LogEvent>() {
        @Override
        public LogEvent newInstance() {
            return new LogEvent();
        }
    };
}
