package org.somda.sdc.biceps.common;

import java.math.BigInteger;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Utility class to create BICEPS compatible time stamps.
 */
public class Timestamp {
    /**
     * Creates a timestamp from now.
     *
     * @return a timestamp in BICEPS format (milliseconds from 1970-01-01T00:00:00Z.
     */
    public static BigInteger now() {
        return BigInteger.valueOf(ZonedDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli());
    }
}
