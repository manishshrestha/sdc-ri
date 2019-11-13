package org.somda.sdc.common.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Adapter class to convert XML durations to Java durations and vice versa.
 */
public class DurationAdapter extends XmlAdapter<String, Duration> {
    // https://stackoverflow.com/questions/32044846/regex-for-iso-8601-durations
    // final Pattern pattern = Pattern.compile("^P(?!$)((\\d+Y)|(\\d+\\.\\d+Y$))?((\\d+M)|(\\d+\\.\\d+M$))?((\\d+W)|(\\d+\\.\\d+W$))?((\\d+D)|(\\d+\\.\\d+D$))?(T(?=\\d)((\\d+H)|(\\d+\\.\\d+H$))?((\\d+M)|(\\d+\\.\\d+M$))?(\\d+(\\.\\d+)?S)?)??$");

    // with named groups (from pySDC)
    final Pattern pattern = Pattern.compile("^(?<sign>[+-])?" +
                    "P(?!\\b)" +
                    "(?<years>[0-9]+([,.][0-9]+)?Y)?" +
                    "(?<months>[0-9]+([,.][0-9]+)?M)?" +
                    "(?<weeks>[0-9]+([,.][0-9]+)?W)?" +
                    "(?<days>[0-9]+([,.][0-9]+)?D)?" +
                    "((?<separator>T)" +
                    "(?<hours>[0-9]+([,.][0-9]+)?H)?" +
                    "(?<minutes>[0-9]+([,.][0-9]+)?M)?" +
                    "(?<seconds>[0-9]+([,.][0-9]+)?S)?)?$");

    @Override
    public Duration unmarshal(String v) {
//        try {
//            Duration.parse(v);
//        } catch(Exception e) {
//            for (final Matcher matcher = pattern.matcher(v); matcher.find(); ) {
//                final String prefix = matcher.group(1);
//                final String uri = matcher.group(2);
//                try {
//                    mapping.put(uri, new PrefixNamespaceMappingParser.PrefixNamespacePair(prefix, new URI(uri)));
//                } catch (URISyntaxException e) {
//                    LOG.warn("Given namespace in {} is not a valid URI: {}", prefixNamespaces, uri);
//                }
//            }
//        }
        return null;
    }

    @Override
    public String marshal(Duration v) {
        return v.toString();
    }
}
