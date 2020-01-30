package org.somda.sdc.common.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.math.BigInteger;
import java.time.Instant;

/**
 * Adapter class to convert Participant Model timestamps to Java instants and vice versa.
 */
public class TimestampAdapter extends XmlAdapter<BigInteger, Instant> {
    @Override
    public Instant unmarshal(BigInteger v) {
        return v == null ? null : Instant.ofEpochMilli(v.longValue());
    }

    @Override
    public BigInteger marshal(Instant v) {
        return v == null ? null : BigInteger.valueOf(v.toEpochMilli());
    }
}
