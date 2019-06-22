package org.ieee11073.sdc.dpws.device.helper;

import com.google.inject.Inject;

import java.net.URI;
import java.util.Optional;

/**
 * Extracts a base path for from an URI to be used with HTTP addresses.
 */
public class UriBaseContextPath {
    final String basePath;

    /**
     * Accepts an URI and tries to cut out the base path on construction.
     */
    public UriBaseContextPath(URI uri) {
        this.basePath = deriveFrom(uri);
    }

    /**
     * Get extracted base path.
     *
     * If the parser was not able to find a base path, an empty string is returned.
     */
    public String get() {
        return basePath;
    }

    private String deriveFrom(URI uri) {
        final Optional<SupportedEprUriScheme> supportedUriScheme =
                getSupportedScheme(uri.getScheme(), uri.getSchemeSpecificPart());
        if (!supportedUriScheme.isPresent()) {
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
