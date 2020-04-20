package org.somda.sdc.common.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Adapter class to convert between XML Schema DateTime and {@linkplain AnyDateTime}.
 */
public class AnyDateTimeAdapter extends XmlAdapter<String, AnyDateTime> {
    private static final DateTimeFormatter LOCAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ZONED_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public AnyDateTime unmarshal(String v) {
        try {
            return AnyDateTime.create(ZONED_FORMATTER.parse(v, ZonedDateTime::from));
        } catch (DateTimeParseException e) {
            return AnyDateTime.create(LOCAL_FORMATTER.parse(v, LocalDateTime::from));
        }
    }

    @Override
    public String marshal(AnyDateTime v) {
        if (v.getZoned().isPresent()) {
            return ZONED_FORMATTER.format(v.getZoned().get());
        } else {
            return LOCAL_FORMATTER.format(v.getLocal().orElseThrow(
                    () -> new RuntimeException(
                            "Could not marshal AnyDateTimeObject as it misses a zoned and local part")));
        }
    }
}
