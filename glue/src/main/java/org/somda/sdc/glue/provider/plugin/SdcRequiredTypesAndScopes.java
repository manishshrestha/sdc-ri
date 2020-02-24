package org.somda.sdc.glue.provider.plugin;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.LocationContextState;
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

/**
 * Maps all WS-Discovery Types and Scopes required by SDC and sends Hellos respectively.
 * <p>
 * In order to append custom scopes please consider using the {@link ScopesDecorator}.
 */
public class SdcRequiredTypesAndScopes implements SdcDevicePlugin, MdibAccessObserver, ScopesDecorator {
    private static final Logger LOG = LoggerFactory.getLogger(SdcRequiredTypesAndScopes.class);

    private Device device;
    private MdibAccess mdibAccess;

    private Set<URI> allScopes;
    private Set<URI> locationContexts;
    private Set<URI> mdsTypes;

    private boolean initializing;

    @Inject
    SdcRequiredTypesAndScopes() {
        this.allScopes = new HashSet<>();
        this.locationContexts = new HashSet<>();
        this.mdsTypes = new HashSet<>();
        this.initializing = true;
    }

    @Override
    public void beforeStartUp(SdcDeviceContext context) {
        LOG.info("Startup of automatic required types and scopes updating for device with EPR address {}",
                context.getDevice().getEprAddress());

        init(context, Collections.emptySet());
        context.getLocalMdibAccess().registerObserver(this);
    }

    @Override
    public void afterShutDown(SdcDeviceContext context) {
        LOG.info("Automatic required types and scopes updating for device with EPR address {} stopped",
                context.getDevice().getEprAddress());
    }

    @Override
    public void init(SdcDeviceContext context, Set<URI> scopes) {
        device = context.getDevice();
        device.getDiscoveryAccess().setTypes(Collections.singletonList(CommonConstants.MEDICAL_DEVICE_TYPE));
        device.getDiscoveryAccess().setScopes(Collections.singletonList(GlueConstants.SCOPE_SDC_PROVIDER));

        mdibAccess = context.getLocalMdibAccess();

        updateScopes();

        // The initializing flag prevents this call from sending a Hello during initialization
        // which would be bad as Hello is sent automatically on startup by the DPWS device implementation
        appendScopesAndSendHello(Collections.emptySet());
    }

    @Override
    public void appendScopesAndSendHello(Set<URI> scopes) {
        updateScopes();

        // Setup new scopes based from scopes from outside plus recognized scopes from this class
        var newScopes = new HashSet<URI>(locationContexts.size() + mdsTypes.size() + scopes.size());
        newScopes.add(GlueConstants.SCOPE_SDC_PROVIDER);
        newScopes.addAll(locationContexts);
        newScopes.addAll(mdsTypes);

        LOG.debug("Append scopes [{}] to internal scope set [{}]",
                Joiner.on(",").join(scopes),
                Joiner.on(",").join(newScopes));

        newScopes.addAll(scopes);

        // If latest scopes and new scopes sizes are equal
        if (allScopes.size() == newScopes.size()) {
            // Remove latest scopes from new scopes
            var newScopesCopy = new HashSet<>(newScopes);
            newScopesCopy.removeAll(allScopes);

            // If there are no scopes left from the new scopes set, then there was no change at all
            // => return without sending Hello
            if (newScopesCopy.isEmpty()) {
                LOG.debug("No scope changes detected");
                return;
            }
        }

        allScopes = newScopes;
        device.getDiscoveryAccess().setScopes(newScopes);

        if (initializing) {
            initializing = false;
        } else {
            device.getDiscoveryAccess().sendHello();
        }
    }

    @Subscribe
    private void onContextChange(ContextStateModificationMessage message) {
        LOG.info("Context modification received");
        appendScopesAndSendHello(Collections.emptySet());
    }

    @Subscribe
    private void onDescriptionChange(DescriptionModificationMessage message) {
        LOG.info("Description modification received");
        appendScopesAndSendHello(Collections.emptySet());
    }

    private void updateScopes() {
        var locationContextsBefore = locationContexts;
        locationContexts = extractAssociatedLocationContextStateIdentifiers(
                mdibAccess.findContextStatesByType(LocationContextState.class));

        LOG.info("Location context scopes updated from [{}] to [{}]",
                Joiner.on(",").join(locationContextsBefore),
                Joiner.on(",").join(locationContexts));

        var mdsTypesBefore = mdsTypes;
        mdsTypes = extractMdsTypes(mdibAccess.findEntitiesByType(MdsDescriptor.class));
        LOG.info("MDS type scopes updated from [{}] to [{}]",
                Joiner.on(",").join(mdsTypesBefore),
                Joiner.on(",").join(mdsTypes));
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
}
