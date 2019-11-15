package org.somda.sdc.glue.consumer;

import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.dpws.client.DiscoveryFilter;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SdcDiscoveryFilterBuilderTest {
    private static final String EXPECTED_PKP_SCOPE = "sdc.mds.pkp:1.2.840.10004.20701.1.1";

    @Test
    void addContext() {
        final InstanceIdentifier identification = new InstanceIdentifier();
        identification.setExtensionName("an-extension");
        identification.setRootName("http://a-root");

        {
            final LocationContextState locationContextState = new LocationContextState();
            locationContextState.getIdentification().add(identification);
            locationContextState.setContextAssociation(ContextAssociation.ASSOC);
            final DiscoveryFilter discoveryFilter = SdcDiscoveryFilterBuilder.create().addContext(locationContextState).get();
            assertEquals(2, discoveryFilter.getScopes().size());
            assertEquals(EXPECTED_PKP_SCOPE, discoveryFilter.getScopes().get(0));
            assertEquals("sdc.ctxt.loc:/http%3A%2F%2Fa-root/an-extension", discoveryFilter.getScopes().get(1));
        }

        {
            final LocationContextState locationContextState = new LocationContextState();
            locationContextState.getIdentification().add(identification);
            locationContextState.setContextAssociation(ContextAssociation.NO);
            final DiscoveryFilter discoveryFilter = SdcDiscoveryFilterBuilder.create().addContext(locationContextState).get();
            assertEquals(1, discoveryFilter.getScopes().size());
            assertEquals(EXPECTED_PKP_SCOPE, discoveryFilter.getScopes().get(0));
        }
    }

    @Test
    void addDeviceComponent() {
        final DiscoveryFilter discoveryFilter = SdcDiscoveryFilterBuilder.create()
                .addDeviceComponent(createVmd("http://a-codingsystem", "a-version", "a-code"))
                .addDeviceComponent(createVmd(null, "", "a-code"))
                .addDeviceComponent(createVmd("urn:oid:1.2.840.10004.1.1.1.0.0.1", null, "a-code"))
                .get();

        assertEquals(4, discoveryFilter.getScopes().size());
        assertEquals(EXPECTED_PKP_SCOPE, discoveryFilter.getScopes().get(0));
        assertEquals("sdc.cdc.type:/http%3A%2F%2Fa-codingsystem/a-version/a-code", discoveryFilter.getScopes().get(1));
        assertEquals("sdc.cdc.type:///a-code", discoveryFilter.getScopes().get(2));
        assertEquals("sdc.cdc.type:///a-code", discoveryFilter.getScopes().get(3));
    }

    private VmdDescriptor createVmd(@Nullable String codingSystem,
                                    @Nullable String codingSystemVersion,
                                    @Nullable String code) {
        final VmdDescriptor vmdDescriptor = new VmdDescriptor();
        vmdDescriptor.setType(createCodedValue(codingSystem, codingSystemVersion, code));
        return vmdDescriptor;
    }

    private CodedValue createCodedValue(@Nullable String codingSystem,
                                        @Nullable String codingSystemVersion,
                                        @Nullable String code) {
        final CodedValue codedValue = new CodedValue();
        codedValue.setCodingSystem(codingSystem);
        codedValue.setCodingSystemVersion(codingSystemVersion);
        codedValue.setCode(code);
        return codedValue;
    }
}