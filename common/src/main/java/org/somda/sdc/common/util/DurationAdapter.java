package org.somda.sdc.common.util;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter class to convert XSD durations to Java durations and vice versa.
 * <p>
 * This adapter was added because the native use of {@link Duration} does not work with all ISO 8601 formats.
 * {@link Duration} only accepts {@code PnDTnHnMn.nS}, whereas this adapter accepts {@code PnYnMnWnDTnHnMn.nS}.
 * <p>
 * <em>Important note: the duration parser ignores years, months and weeks.</em>
 */
public class DurationAdapter extends XmlAdapter<String, Duration> {
    private static final String SIGN = "sign";
    private static final String DAYS = "days";
    private static final String HOURS = "hours";
    private static final String MINUTES = "minutes";
    private static final String SECONDS = "seconds";

    private static final Pattern pattern = Pattern.compile("^(?<" + SIGN + ">[+-])?" +
            "P(?!\\b)" +
            "(?:(?<years>[0-9]+([,.][0-9]+)?)Y)?" +
            "(?:(?<months>[0-9]+([,.][0-9]+)?)M)?" +
            "(?:(?<weeks>[0-9]+([,.][0-9]+)?)W)?" +
            "(?:(?<" + DAYS + ">[0-9]+([,.][0-9]+)?)D)?" +
            "((?<separator>T)" +
            "(?:(?<" + HOURS + ">[0-9]+([,.][0-9]+)?)H)?" +
            "(?:(?<" + MINUTES + ">[0-9]+([,.][0-9]+)?)M)?" +
            "(?:(?<" + SECONDS + ">[0-9]+([,.][0-9]+)?)S)?)?$", Pattern.CASE_INSENSITIVE);

    @Override
    public Duration unmarshal(String v) {
        if (v == null) {
            return null;
        }

        try {
            return Duration.parse(v);
        } catch (Exception e) {
            final Matcher matcher = pattern.matcher(v);
            if (matcher.matches()) {
                final String sign = matcher.group(SIGN);
                return Duration.parse(String.format("%sP%sDT%sH%sM%sS",
                        sign == null ? "" : sign,
                        givenOrZero(matcher.group(DAYS)),
                        givenOrZero(matcher.group(HOURS)),
                        givenOrZero(matcher.group(MINUTES)),
                        givenOrZero(matcher.group(SECONDS))));
            }
        }

        throw new DateTimeParseException("XML Schema duration could not be parsed to a Duration", v, 0);
    }

    @Override
    public String marshal(Duration v) {
        return v == null ? null : v.toString();
    }

    private String givenOrZero(@Nullable String given) {
        return given == null ? "0" : given;
    }
}
