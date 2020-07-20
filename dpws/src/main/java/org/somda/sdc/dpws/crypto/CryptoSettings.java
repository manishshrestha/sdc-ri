package org.somda.sdc.dpws.crypto;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

/**
 * Common interface to retrieve key and trust store information.
 * <p>
 * Depending on the data source key and trust store information can be referenced using a file
 * ({@link #getKeyStoreFile()}, {@link #getTrustStoreFile()}) or streams ({@link #getKeyStoreStream()},
 * {@link #getKeyStoreStream()}).
 *
 * @see <a href="https://www.cloudera.com/documentation/enterprise/5-10-x/topics/cm_sg_create_key_trust.html"
 * >More information on key and trust stores</a>
 */
public interface CryptoSettings {
    Optional<File> getKeyStoreFile();

    Optional<InputStream> getKeyStoreStream();

    String getKeyStorePassword();

    Optional<File> getTrustStoreFile();

    Optional<InputStream> getTrustStoreStream();

    String getTrustStorePassword();
}
