package org.ieee11073.sdc.dpws.device.helper;

import java.net.URI;
import java.util.Optional;

/**
 * Extracts a base path for from an URI to be used with HTTP addresses.
 */
public class UriBaseContextPath {
    final String basePath;

    /**
     * Accepts an URI and tries to cut out the base path on construction.
     *
     * @param uri the inspected URI.
     */
    public UriBaseContextPath(URI uri) {
        this.basePath = deriveFrom(uri);
    }

    /**
     * Gets extracted base path.
     *
     * @return the base path or an empty string if the parser was not able to find a base path.
     */
    public String get() {
        return basePath;
    }

    private String deriveFrom(URI uri) {
        final Optional<SupportedEprUriScheme> supportedUriScheme =
                getSupportedScheme(uri.getScheme(), uri.getSchemeSpecificPart());
        if (supportedUriScheme.isEmpty()) {
            return "";
        }
        switch (supportedUriScheme.get()) {
            case HTTP:
            case HTTPS:
                return uri.getPath().substring(1); // skip preceding slash
            case URN_UUID:
            case URN_OID:
                return uri.getSchemeSpecificPart().substring(supportedUriScheme.get().getSpecificPart().length() + 1);
            default:
                return "";
        }
    }

    private Optional<SupportedEprUriScheme> getSupportedScheme(String scheme, String schemeSpecificPart) {
        for (SupportedEprUriScheme supportedScheme : SupportedEprUriScheme.values()) {
            if (!supportedScheme.getSchemeName().equalsIgnoreCase(scheme)) {
                continue;
            }

            if (supportedScheme.getSpecificPart().isEmpty()) {
                return Optional.of(supportedScheme);
            }

            if (schemeSpecificPart.toLowerCase().startsWith(supportedScheme.getSpecificPart())) {
                return Optional.of(supportedScheme);
            }
        }

        return Optional.empty();
    }

}
