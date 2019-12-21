package org.somda.sdc.glue.consumer;

import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.dpws.client.DiscoveryFilter;
import org.somda.sdc.dpws.client.DiscoveryFilterBuilder;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.ContextIdentificationMapper;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public static SdcDiscoveryFilterBuilder create() {
        return new SdcDiscoveryFilterBuilder();
    }

    /**
     * Constructs a new object with empty types and scopes.
     */
    private SdcDiscoveryFilterBuilder() {
        this.discoveryFilterBuilder = new DiscoveryFilterBuilder();
        this.discoveryFilterBuilder.addType(TYPE_MEDICAL_DEVICE);
        this.discoveryFilterBuilder.addScope(GlueConstants.SCOPE_SDC_PROVIDER.toString());
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
     * @param <T>   a context state type.
     * @return this object.
     */
    public <T extends AbstractContextState> SdcDiscoveryFilterBuilder addContext(T state) {
        createScopeFromContext(state).ifPresent(scope -> addScope(scope));
        return this;
    }

    /**
     * Adds a device component type.
     *
     * @param component the location context state.
     * @param <T>       a complex device component descriptor type.
     * @return this object.
     */
    public <T extends AbstractComplexDeviceComponentDescriptor> SdcDiscoveryFilterBuilder addDeviceComponent(T component) {
        if (component.getType() == null) {
            return this;
        }
        createScopeFromCodedValue("sdc.cdc.type", component.getType()).ifPresent(scope -> addScope(scope));
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
    // optional means: not associated or no identification found
    private static Optional<String> createScopeFromContext(AbstractContextState contextState) {
        if (!contextState.getContextAssociation().equals(ContextAssociation.ASSOC)) {
            return Optional.empty();
        }

        if (contextState.getIdentification().isEmpty()) {
            return Optional.empty();
        }

        ContextIdentificationMapper.ContextSource contextSource = mapToContextSource(contextState);
        return Optional.of(ContextIdentificationMapper.fromInstanceIdentifier(contextState.getIdentification().get(0), contextSource).toString());
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

    private static String encode(@Nullable String text) throws UnsupportedEncodingException {
        return text == null ? "" : URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
