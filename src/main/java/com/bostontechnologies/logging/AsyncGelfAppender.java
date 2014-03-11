package com.bostontechnologies.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.AppenderBase;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class AsyncGelfAppender extends AppenderBase<ILoggingEvent> {

    //Required Fields
    private String graylogHostAddress;
    private int graylogHostPort = -1;

    //Optional Fields
    private String host;
    private boolean useTimestamp;
    private boolean useLevel;
    private boolean useThreadName;
    private int ringSize = 1024 * 8 * 2;
    private WaitStrategy waitStrategy;

    //Private Fields
    private ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "Async-Gelf-Logger");
        }
    });

    private Disruptor<LogEvent> disruptor;
    private Socket socket;


    @Override
    public void start() {
        super.start();

        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        if (waitStrategy == null) {
            waitStrategy = new BlockingWaitStrategy();
        }

        disruptor = new Disruptor<LogEvent>(LogEvent.EVENT_FACTORY, ringSize, executorService, ProducerType.MULTI, waitStrategy);

        EventHandler<LogEvent> handler = new EventHandler<LogEvent>() {
            @Override
            public void onEvent(LogEvent event, long sequence, boolean endOfBatch) throws Exception {
                try {
                    ILoggingEvent iLoggingEvent = event.getiLoggingEvent();
                    StringBuilder appender = new StringBuilder();

                    appender.append("{\"version\":\"1.1\"")
                            .append(",\"host\":\"").append(host).append("\"")
                            .append(",\"short_message\":\"").append(iLoggingEvent.getMessage()).append("\"");

                    //full message
                    IThrowableProxy throwableProxy = iLoggingEvent.getThrowableProxy();
                    if (throwableProxy != null) {
                        appender.append(",\"full_message\":\"").append(throwableProxy.getMessage()).append("\"");
                        StackTraceElementProxy[] proxyStackTraces = throwableProxy.getStackTraceElementProxyArray();
                        for (StackTraceElementProxy stack : proxyStackTraces) {
                            appender.append("\n").append(stack.getStackTraceElement());
                        }
                        appender.append("\"");

                        if (proxyStackTraces != null && proxyStackTraces.length > 0) {
                            StackTraceElement lastStack = proxyStackTraces[0].getStackTraceElement();
                            appender.append(",\"_file\":\"").append(lastStack.getFileName()).append("\"");
                            appender.append(",\"_line\":\"").append(String.valueOf(lastStack.getLineNumber())).append("\"");

                        }
                    }

                    if (useTimestamp) {
                        double time = iLoggingEvent.getTimeStamp() / 1000d;
                        appender.append(",\"timestamp\":\"").append(time).append("\"");
                    }

                    if (useLevel) {
                        appender.append(",\"level\":\"").append(LevelToSyslogSeverity.convert(iLoggingEvent)).append("\"");
                    }

                    if (useThreadName) {
                        appender.append(",\"_thread_name\":\"").append(iLoggingEvent.getThreadName()).append("\"");
                    }

                    appender.append("}\0");

                    String str = appender.toString();

                    if (socket == null || !socket.isConnected()){
                        Connect();
                    }

                    PrintWriter out = new PrintWriter(socket.getOutputStream());
                    out.print(str);
                    out.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        disruptor.handleEventsWith(handler);

        disruptor.start();
    }

    private void Connect() {
        try {
            socket = new Socket(graylogHostAddress, graylogHostPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        long seq = disruptor.getRingBuffer().next();
        disruptor.getRingBuffer().get(seq).setiLoggingEvent(iLoggingEvent);
        disruptor.getRingBuffer().publish(seq);
    }

    @Override
    public void stop() {
        super.stop();
        try {
            System.out.println("Stopping");
            socket.close();
            disruptor.shutdown();
            executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setGraylogHostAddress(String graylogHostAddress) {
        this.graylogHostAddress = graylogHostAddress;
    }

    public void setGraylogHostPort(int graylogHostPort) {
        this.graylogHostPort = graylogHostPort;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setUseThreadName(Boolean useThreadName) {
        this.useThreadName = useThreadName;
    }

    public void setUseLevel(boolean useLevel) {
        this.useLevel = useLevel;
    }

    public void setUseTimestamp(boolean useTimestamp) {
        this.useTimestamp = useTimestamp;
    }

    public void setRingSize(int ringSize) {
        this.ringSize = ringSize;
    }

    public void setWaitStrategy(String waitStrategy) {
        if (waitStrategy.equalsIgnoreCase("BlockingWaitStrategy")) {
            this.waitStrategy = new BlockingWaitStrategy();
        } else if (waitStrategy.equalsIgnoreCase("BusySpinWaitStrategy")) {
            this.waitStrategy = new BusySpinWaitStrategy();
        } else if (waitStrategy.equalsIgnoreCase("SleepingWaitStrategy")) {
            this.waitStrategy = new SleepingWaitStrategy();
        } else if (waitStrategy.equalsIgnoreCase("YieldingWaitStrategy")) {
            this.waitStrategy = new YieldingWaitStrategy();
        }
    }
}
