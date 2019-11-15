package org.somda.sdc.glue.consumer;

import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.dpws.client.DiscoveryFilter;
import org.somda.sdc.dpws.client.DiscoveryFilterBuilder;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.ContextIdentificationMapper;

import javax.xml.namespace.QName;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Optional;

/**
 * A variant of the {@linkplain DiscoveryFilterBuilder} that adds scopes and types required by SDC.
 * <p>
 * The following type is assigned: {@code {http://standards.ieee.org/downloads/11073/11073-20702-2016}MedicalDevice}
 * The following scope is assigned: {@code sdc.mds.pkp:1.2.840.10004.20701.1.1}
 * <p>
 * todo DGr add reference to MDPWS type
 *
 * @see GlueConstants#OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER
 */
public class SdcDiscoveryFilterBuilder {
    private final DiscoveryFilterBuilder discoveryFilterBuilder;

    // todo DGr should be defined in MDPWS
    private static final QName TYPE_MEDICAL_DEVICE = new QName("http://standards.ieee.org/downloads/11073/11073-20702-2016", "MedicalDevice");

    private static final String SCOPE_SDC_PARTICIPANT = "sdc.mds.pkp:" + GlueConstants.OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER;

    /**
     * Constructs a new object with empty types and scopes.
     */
    public SdcDiscoveryFilterBuilder() {
        this.discoveryFilterBuilder = new DiscoveryFilterBuilder();
        this.discoveryFilterBuilder.addType(TYPE_MEDICAL_DEVICE);
        this.discoveryFilterBuilder.addScope(SCOPE_SDC_PARTICIPANT);
    }

    /**
     * Adds a type.
     *
     * @param type the type as QName according to WS-Discovery.
     * @return this object.
     */
    public SdcDiscoveryFilterBuilder addType(QName type) {
        discoveryFilterBuilder.addType(type);
        return this;
    }

    /**
     * Adds a scope.
     *
     * @param scope the scope URI as string.
     * @return this object.
     */
    public SdcDiscoveryFilterBuilder addScope(String scope) {
        discoveryFilterBuilder.addScope(scope);
        return this;
    }

    /**
     * Adds a primary context state instance identifier as scope.
     *
     * @param state the location context state.
     * @return this object.
     */
    public <T extends AbstractContextState> SdcDiscoveryFilterBuilder addContext(T state) {
        addScope(createScopeFromContext(state));
        return this;
    }

    /**
     * Adds a device component type.
     *
     * @param component the location context state.
     * @return this object.
     */
    public <T extends AbstractComplexDeviceComponentDescriptor> SdcDiscoveryFilterBuilder addDeviceComponent(T component) {
        if (component.getType() == null) {
            return this;
        }
        createScopeFromCodedValue("sdc.cdc.type:", component.getType()).ifPresent(scope -> addScope(scope));
        return this;
    }

    /**
     * Gets a discovery filter with all types and scopes added via {@link #addType(QName)} and
     * {@link #addScope(String)}.
     *
     * @return a {@linkplain DiscoveryFilter} instance.
     */
    public DiscoveryFilter get() {
        return discoveryFilterBuilder.get();
    }

    // Creates a scope for a context based on the grammar in IEEE 11073-20701 section 9.4
    // empty string means: not associated or no identification found
    private static String createScopeFromContext(AbstractContextState contextState) {
        if (!contextState.getContextAssociation().equals(ContextAssociation.ASSOC)) {
            return "";
        }

        if (contextState.getIdentification().isEmpty()) {
            return "";
        }

        ContextIdentificationMapper.ContextSource contextSource = mapToContextSource(contextState);
        return ContextIdentificationMapper.fromInstanceIdentifier(contextState.getIdentification().get(0), contextSource).toString();
    }

    private static ContextIdentificationMapper.ContextSource mapToContextSource(AbstractContextState contextState) {
        for (ContextIdentificationMapper.ContextSource value : ContextIdentificationMapper.ContextSource.values()) {
            if (value.getSourceClass().isAssignableFrom(contextState.getClass())) {
                return value;
            }
        }
        throw new RuntimeException(String.format("Reached unknown context: %s", contextState.getClass().toString()));
    }

    private Optional<String> createScopeFromCodedValue(String scheme, CodedValue codedValue) {
        try {
            String codingSystem = codedValue.getCodingSystem();
            if ("urn:oid:1.2.840.10004.1.1.1.0.0.1".equals(codingSystem)) {
                codingSystem = null;
            }

            return Optional.of(scheme + ":" +
                    "/" + encode(codingSystem) +
                    "/" + encode(codedValue.getCodingSystemVersion()) +
                    "/" + encode(codedValue.getCode()));
        } catch (UnsupportedEncodingException e) {
            return Optional.empty();
        }
    }

    private static String encode(String text) throws UnsupportedEncodingException {
        return text == null ? "" : URLEncoder.encode(text, "UTF-8");
    }
}
