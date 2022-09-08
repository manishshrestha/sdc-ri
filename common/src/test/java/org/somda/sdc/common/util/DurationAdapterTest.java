package org.somda.sdc.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationAdapterTest {
    private DurationAdapter durationAdapter;

    @BeforeEach
    void beforeEach() {
        durationAdapter = new DurationAdapter();
    }

    @Test
    void roundTrip() {
        final List<Duration> expectedDurations = Arrays.asList(
                Duration.ofDays(20),
                Duration.ofHours(80),
                Duration.ofMinutes(180),
                Duration.ofMillis(10924),
                Duration.ofNanos(23456),
                Duration.parse("P10DT15H10M450.90S"),
                Duration.parse("P0DT15H0M450.90S"),
                Duration.parse("P0DT10M450S"),
                Duration.parse("-P0DT10M450S"));

        expectedDurations.forEach(expectedDuration -> {
            assertEquals(expectedDuration, runRoundTrip(expectedDuration));
        });
    }

    @Test
    void fullIso8601Format() {
        final Duration p0Y0M0W0DT10H0M = durationAdapter.unmarshal("P0Y0M0W0DT10H0M");
        assertEquals(Duration.ofHours(10), p0Y0M0W0DT10H0M);

        final Duration p15Y0M1D = durationAdapter.unmarshal("P15Y0M1D");
        assertEquals(Duration.ofHours(24), p15Y0M1D);

        final Duration p0M0DT1H100M0S = durationAdapter.unmarshal("P0M0DT1H100M0S");
        assertEquals(Duration.ofMinutes(160), p0M0DT1H100M0S);

        final Duration pT0H0_5S = durationAdapter.unmarshal("PT0H0.5S");
        assertEquals(Duration.ofMillis(500), pT0H0_5S);
    }

    private Duration runRoundTrip(Duration expectedDuration) {
        return durationAdapter.unmarshal(durationAdapter.marshal(expectedDuration));
    }
}