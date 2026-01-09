package uk.ac.cam.cl.dtg.isaac.api;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestAppender extends AbstractAppender {
    private final List<LogEvent> events = new ArrayList<>();

    public TestAppender() {
        super("TestAppender", null, null, true, Property.EMPTY_ARRAY);
        start();
    }

    @Override
    public void append(final LogEvent event) {
        events.add(event.toImmutable());
    }

    public void assertLevel(final Level level) {
        assertEquals(1, this.events.size());
        assertEquals(level, this.events.get(0).getLevel());
    }

    public void assertMessage(final String message) {
        assertEquals(1, this.events.size());
        assertEquals(message, this.events.get(0).getMessage().getFormattedMessage());
    }
}