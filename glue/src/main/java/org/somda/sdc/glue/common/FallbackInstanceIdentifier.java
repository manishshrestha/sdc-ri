package org.somda.sdc.glue.common;

import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationDetail;

import javax.annotation.Nullable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Utility class to create fallback instance identifiers from location detail.
 * <p>
 * The instance identifier generated with this class meet the specification of IEEE 11073-20701-2018 section 9.4.1.1.
 */
public class FallbackInstanceIdentifier {
    private static final String LOCATION_ROOT_SEGMENT = "sdc.ctxt.loc.detail";
    private static final String DELIMITER = "/";

    /**
     * Creates an instance identifier based on the fallback algorithm defined in IEEE 11073-20701-2018 section 9.4.1.1.
     *
     * @param locationDetail the location detail used to derive the instance identifier.
     * @return a converted instance identifier or {@link Optional#empty()} if no location information
     * is set.
     */
    public static Optional<InstanceIdentifier> create(LocationDetail locationDetail) {
        final String extension = encode(locationDetail.getFacility())
                + DELIMITER + encode(locationDetail.getBuilding())
                + DELIMITER + encode(locationDetail.getFloor())
                + DELIMITER + encode(locationDetail.getPoC())
                + DELIMITER + encode(locationDetail.getRoom())
                + DELIMITER + encode(locationDetail.getBed());
        if (!extension.equals("/////")) {
            InstanceIdentifier instanceIdentifier = new InstanceIdentifier();
            instanceIdentifier.setRootName(LOCATION_ROOT_SEGMENT);
            instanceIdentifier.setExtensionName(extension);
            return Optional.of(instanceIdentifier);
        }

        return Optional.empty();
    }

    private static String encode(@Nullable String text) {
        return text == null ? "" : URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
