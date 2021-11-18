package org.somda.sdc.common.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Adapter class to convert between XML Schema DateTime and {@linkplain AnyDateTime}.
 */
public class AnyDateTimeAdapter extends XmlAdapter<String, AnyDateTime> {
    private static final DateTimeFormatter LOCAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public AnyDateTime unmarshal(String v) {
        try {
            return AnyDateTime.create(OFFSET_FORMATTER.parse(v, OffsetDateTime::from));
        } catch (DateTimeParseException e) {
            return AnyDateTime.create(LOCAL_FORMATTER.parse(v, LocalDateTime::from));
        }
    }

    @Override
    public String marshal(AnyDateTime v) {
        var offset = v.getOffset();
        if (offset.isPresent()) {
            return OFFSET_FORMATTER.format(offset.get());
        } else {
            return LOCAL_FORMATTER.format(v.getLocal().orElseThrow(
                    () -> new RuntimeException(
                            "Could not marshal AnyDateTimeObject as it misses a offset and local part")));
        }
    }
}
