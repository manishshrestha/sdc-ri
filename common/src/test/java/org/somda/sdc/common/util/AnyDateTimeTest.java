package org.somda.sdc.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AnyDateTimeTest {
    @Test
    void forceOffset() {
        var defaultOffset = OffsetDateTime.now().getOffset();
        var localDateTime = LocalDateTime.now();
        var localDateTimeAsAny = AnyDateTime.create(localDateTime);
        var expectedOffsetDateTime = OffsetDateTime.of(localDateTime, defaultOffset);
        var actualOffsetDateTime = localDateTimeAsAny.forceOffset();
        assertEquals(expectedOffsetDateTime, actualOffsetDateTime);
    }

    @Test
    void doIfLocal() {
        var localDateTime = LocalDateTime.now();
        var localDateTimeAsAny = AnyDateTime.create(localDateTime);
        assertTrue(localDateTimeAsAny.getLocal().isPresent());
        assertTrue(localDateTimeAsAny.getOffset().isEmpty());

        {
            var triggered = new AtomicBoolean(false);
            localDateTimeAsAny
                    .doIfLocal(actualDateTime -> {
                        assertEquals(localDateTime, actualDateTime);
                        triggered.set(true);
                    })
                    .orElse(offsetDateTime -> fail("Triggered offset although object is local"));
            assertTrue(triggered.get());
        }
        {
            var triggered = new AtomicBoolean(false);
            localDateTimeAsAny
                    .doIfOffset(offsetDateTime -> fail("Triggered offset although object is local"))
                    .orElse(actualDateTime -> {
                        assertEquals(localDateTime, actualDateTime);
                        triggered.set(true);
                    });
            assertTrue(triggered.get());
        }
    }

    @Test
    void doIfOffset() {
        var offsetDateTime = OffsetDateTime.now();
        var offsetDateTimeAsAny = AnyDateTime.create(offsetDateTime);
        assertTrue(offsetDateTimeAsAny.getLocal().isEmpty());
        assertTrue(offsetDateTimeAsAny.getOffset().isPresent());

        {
            var triggered = new AtomicBoolean(false);
            offsetDateTimeAsAny
                    .doIfLocal(localDateTime -> fail("Triggered local although object is offset"))
                    .orElse(actualDateTime -> {
                        assertEquals(offsetDateTime, actualDateTime);
                        triggered.set(true);
                    });
            assertTrue(triggered.get());
        }
        {
            var triggered = new AtomicBoolean(false);
            offsetDateTimeAsAny
                    .doIfOffset(actualDateTime -> {
                        assertEquals(offsetDateTime, actualDateTime);
                        triggered.set(true);
                    })
                    .orElse(localDateTime -> fail("Triggered local although object is offset"));
            assertTrue(triggered.get());
        }
    }
}