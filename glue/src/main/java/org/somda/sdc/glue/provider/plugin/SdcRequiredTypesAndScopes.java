package org.somda.sdc.glue.provider.plugin;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.ComplexDeviceComponentMapper;
import org.somda.sdc.glue.common.ContextIdentificationMapper;
import org.somda.sdc.glue.provider.SdcDeviceContext;
import org.somda.sdc.glue.provider.SdcDevicePlugin;
import org.somda.sdc.mdpws.common.CommonConstants;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class SdcRequiredTypesAndScopes implements SdcDevicePlugin, MdibAccessObserver {
    private static final Logger LOG = LoggerFactory.getLogger(SdcRequiredTypesAndScopes.class);

    private Device device;

    private Set<URI> locationContexts;
    private Set<URI> mdsTypes;

    private MdibVersion seenMdibVersion;

    @Inject
    SdcRequiredTypesAndScopes() {
        this.locationContexts = new HashSet<>();
        this.mdsTypes = new HashSet<>();
        this.seenMdibVersion = null;
    }

    @Override
    public void beforeStartUp(SdcDeviceContext context) {
        device = context.getDevice();
        device.getDiscoveryAccess().setTypes(Collections.singletonList(CommonConstants.MEDICAL_DEVICE_TYPE));
        device.getDiscoveryAccess().setScopes(Collections.singletonList(GlueConstants.SCOPE_SDC_PROVIDER));

        context.getLocalMdibAccess().registerObserver(this);
    }

    void onContextChange(ContextStateModificationMessage message) {
        // This avoids sending context hellos twice if been communicated by description report already
        var olderMdibVersion = isSeenMdibVersionOlderThan(message.getMdibAccess().getMdibVersion());
        if (olderMdibVersion.isEmpty() || olderMdibVersion.get() >= 0) {
            return;
        }

        var newLocationContexts = extractAssociatedLocationContextStateIdentifiers(
                message.getMdibAccess().findContextStatesByType(LocationContextState.class));

        // check if there are changes to the current associated location contexts
        // only send hello if changes ensued
    }

    private Set<URI> extractAssociatedLocationContextStateIdentifiers(List<LocationContextState> contextStates) {
        var locationContextState = contextStates.stream()
                .filter(contextState -> ContextAssociation.ASSOC.equals(contextState.getContextAssociation()))
                .findFirst();

        if (locationContextState.isEmpty()) {
            return Collections.emptySet();
        }

        Set<URI> uris = new HashSet<>(locationContextState.get().getIdentification().size());
        for (var instanceIdentifier : locationContextState.get().getIdentification()) {
            uris.add(ContextIdentificationMapper.fromInstanceIdentifier(instanceIdentifier,
                    ContextIdentificationMapper.ContextSource.Location));
        }
        return uris;
    }

    void onDescriptionChange(DescriptionModificationMessage message) {
        locationContexts = extractAssociatedLocationContextStateIdentifiers(
                message.getMdibAccess().findContextStatesByType(LocationContextState.class));
        mdsTypes = extractMdsTypes(message.getMdibAccess().findEntitiesByType(MdsDescriptor.class));

        // check if there are changes to the current associated location contexts or MDS types
        // only send hello if changes ensued
    }

    private Set<URI> extractMdsTypes(Collection<MdibEntity> entities) {
        var mdsDescriptors = entities.stream()
                .filter(mdibEntity -> mdibEntity.getDescriptor(MdsDescriptor.class).isPresent())
                .map(mdibEntity -> mdibEntity.getDescriptor(MdsDescriptor.class).get())
                .collect(Collectors.toList());

        var uris = new HashSet<URI>(mdsDescriptors.size());
        for (MdsDescriptor mdsDescriptor : mdsDescriptors) {
            ComplexDeviceComponentMapper.fromComplexDeviceComponent(mdsDescriptor).ifPresent(uris::add);
        }

        return uris;
    }

    private void reassignScopes() {
        var scopes = new HashSet<URI>(locationContexts.size() + mdsTypes.size());
        scopes.add(GlueConstants.SCOPE_SDC_PROVIDER);
        scopes.addAll(locationContexts);
        scopes.addAll(mdsTypes);
        device.getDiscoveryAccess().setScopes(scopes);
        // todo There is currently no mechanism to re-send hellos; this is unacceptable and needs to be refactored
    }

    private Optional<Integer> isSeenMdibVersionOlderThan(MdibVersion mdibVersion) {
        if (seenMdibVersion == null) {
            seenMdibVersion = mdibVersion;
            return Optional.of(-1);
        }

        return seenMdibVersion.compareTo(mdibVersion);
    }
}
