package org.somda.sdc.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnyDateTimeAdapterTest {
    private AnyDateTimeAdapter adapter;

    @BeforeEach
    void beforeEach() {
        this.adapter = new AnyDateTimeAdapter();
    }

    @Test
    void roundTrip() {
        /*
            # Valid values
            2004-04-12T13:20:00        1:20 pm on April 12, 2004
            2004-04-12T13:20:15.5      1:20 pm and 15.5 seconds on April 12, 2004
            2004-04-12T13:20:00-05:00  1:20 pm on April 12, 2004, US Eastern Standard Time
            2004-04-12T13:20:00Z       1:20 pm on April 12, 2004, Coordinated Universal Time (UTC)
         */
        var validDateTimes = List.of(
                "2004-04-12T13:20:00",
                "2004-04-12T13:20:15.5",
                "2004-04-12T13:20:00-05:00",
                "2004-04-12T13:20:00Z"
        );

        for (String validDateTime : validDateTimes) {
            assertEquals(validDateTime, adapter.marshal(adapter.unmarshal(validDateTime)));
        }
    }

    @Test
    void failure() {
        /*
            # Invalid values
            2004-04-12T13:00           seconds must be specified
            2004-04-1213:20:00         the letter T is required
            99-04-12T13:00             the century must not be left truncated
            2004-04-12                 the time is required
            2004-04-12T13:20:00-05     time zone offset is incomplete, no colon and subsequent value
            2004-04-12T13:20:00-05:    time zone offset is incomplete, no value after colon
            2004-04-12Z                time is required, zone without time is not allowed
         */
        var invalidDateTimes = List.of(
                // "2004-04-12T13:00", // Java's parser accepts this and sets seconds to 0 implicitly
                "2004-04-1213:20:00",
                "99-04-12T13:00",
                "2004-04-12",
                // "2004-04-12T13:20:00-05:", // Java's parser accepts this and sets offset minutes to 0 implicitly
                "2004-04-12T13:20:00-05:",
                "2004-04-12Z"
        );

        for (String invalidDateTime : invalidDateTimes) {
            assertThrows(DateTimeParseException.class, () -> adapter.marshal(adapter.unmarshal(invalidDateTime)));
        }
    }
}