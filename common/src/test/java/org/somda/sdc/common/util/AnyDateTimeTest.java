package org.somda.sdc.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AnyDateTimeTest {
    @Test
    void forceZoned() {
        var defaultZone = ZoneId.systemDefault();
        var localDateTime = LocalDateTime.now();
        var localDateTimeAsAny = AnyDateTime.create(localDateTime);
        var expectedZonedDateTime = ZonedDateTime.of(localDateTime, defaultZone);
        var actualZonedDateTime = localDateTimeAsAny.forceZoned();
        assertEquals(expectedZonedDateTime, actualZonedDateTime);
    }

    @Test
    void doIfLocal() {
        var localDateTime = LocalDateTime.now();
        var localDateTimeAsAny = AnyDateTime.create(localDateTime);
        assertTrue(localDateTimeAsAny.getLocal().isPresent());
        assertTrue(localDateTimeAsAny.getZoned().isEmpty());

        {
            var triggered = new AtomicBoolean(false);
            localDateTimeAsAny
                    .doIfLocal(actualDateTime -> {
                        assertEquals(localDateTime, actualDateTime);
                        triggered.set(true);
                    })
                    .orElse(zonedDateTime -> fail("Triggered zoned although object is local"));
            assertTrue(triggered.get());
        }
        {
            var triggered = new AtomicBoolean(false);
            localDateTimeAsAny
                    .doIfZoned(zonedDateTime -> fail("Triggered zoned although object is local"))
                    .orElse(actualDateTime -> {
                        assertEquals(localDateTime, actualDateTime);
                        triggered.set(true);
                    });
            assertTrue(triggered.get());
        }
    }

    @Test
    void doIfZoned() {
        var zonedDateTime = ZonedDateTime.now();
        var zonedDateTimeAsAny = AnyDateTime.create(zonedDateTime);
        assertTrue(zonedDateTimeAsAny.getLocal().isEmpty());
        assertTrue(zonedDateTimeAsAny.getZoned().isPresent());

        {
            var triggered = new AtomicBoolean(false);
            zonedDateTimeAsAny
                    .doIfLocal(localDateTime -> fail("Triggered local although object is zoned"))
                    .orElse(actualDateTime -> {
                        assertEquals(zonedDateTime, actualDateTime);
                        triggered.set(true);
                    });
            assertTrue(triggered.get());
        }
        {
            var triggered = new AtomicBoolean(false);
            zonedDateTimeAsAny
                    .doIfZoned(actualDateTime -> {
                        assertEquals(zonedDateTime, actualDateTime);
                        triggered.set(true);
                    })
                    .orElse(localDateTime -> fail("Triggered local although object is zoned"));
            assertTrue(triggered.get());
        }
    }
}