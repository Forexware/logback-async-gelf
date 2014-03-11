Async-Gelf-Logback - An Async Logback Appender for GELF
=======================================================


Example Logback:

<configuration debug="false">

    <appender name="GELF" class="com.bostontechnologies.cscore.logging.gelf.GelfAppender">
        <graylogHostAddress>grayloghost</graylogHostAddress>
        <graylogHostPort>12201</graylogHostPort>
        <!--Optional-->
        <useThreadName>true</useThreadName>
        <useTimestamp>true</useTimestamp>
        <useLevel>true</useLevel>
        <!--BlockingWaitStrategy BusySpinWaitStrategy SleepingWaitStrategy YieldingWaitStrategy -->
        <waitStrategy>BlockingWaitStrategy</waitStrategy>
    </appender>

    <root level="info">
        <appender-ref ref="GELF" />
	</root>

</configuration>


Had a lot of inspiration from https://github.com/Moocar/logback-gelf