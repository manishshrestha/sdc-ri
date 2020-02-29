package org.somda.sdc.common.util;

import java.util.UUID;

public class UriUtil {
    public static String createUuid(String hexDigitEncodedUuid) {
        return "urn:uuid:" + hexDigitEncodedUuid;
    }

    public static String createUuid(UUID uuid) {
        return "urn:uuid:" + uuid.toString();
    }
}
