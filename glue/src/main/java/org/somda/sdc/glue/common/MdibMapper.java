package org.somda.sdc.glue.common;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMetricDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.BatteryDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ClockDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.MdDescription;
import org.somda.sdc.biceps.model.participant.MdState;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MeansContextDescriptor;
import org.somda.sdc.biceps.model.participant.OperatorContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.model.participant.WorkflowContextDescriptor;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps MDIB entities from {@linkplain MdibAccess} to an {@linkplain Mdib} object.
 * <p>
 * Use {@link ModificationsBuilder} to map from an {@linkplain Mdib} to {@linkplain MdibDescriptionModifications} in
 * order to add MDIB elements to {@link org.somda.sdc.biceps.consumer.access.RemoteMdibAccess} and
 * {@link org.somda.sdc.biceps.provider.access.LocalMdibAccess}.
 */
public class MdibMapper {
    private static final Logger LOG = LogManager.getLogger(MdibMapper.class);
    private final MdibAccess mdibAccess;
    private final Logger instanceLogger;

    @AssistedInject
    MdibMapper(@Assisted MdibAccess mdibAccess,
               @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.mdibAccess = mdibAccess;
    }

    /**
     * Maps to an {@linkplain Mdib} instance.
     * <p>
     * All information is copied from the {@link MdibAccess} given to the {@linkplain MdibMapper} on construction.
     *
     * @return a fully populated {@link Mdib} instance.
     */
    public Mdib mapMdib() {
        final MdibVersion mdibVersion = mdibAccess.getMdibVersion();

        return Mdib.builder()
            .withSequenceId(mdibVersion.getSequenceId())
            .withInstanceId(mdibVersion.getInstanceId())
            .withMdibVersion(mdibVersion.getVersion())
            .withMdDescription(mapMdDescription(Collections.emptyList()))
            .withMdState(mapMdState(Collections.emptyList()))
            .build();
    }

    /**
     * Maps to an {@link MdState} instance.
     *
     * @param handleFilter a filter to limit the result:
     *                     <ul>
     *                     <li>If the handle reference list is empty,
     *                     all states in the MDIB are included in the result list.
     *                     <li>If a handle reference does match a multi-state handle,
     *                     the corresponding multi-state is included in the result list.
     *                     <li>If a handle reference does match a descriptor handle,
     *                     all states that belong to the corresponding descriptor are included in the result list.
     *                     </ul>
     * @return the mapped instance.
     */
    public MdState mapMdState(List<String> handleFilter) {
        final var mdState = MdState.builder()
            .withStateVersion(mdibAccess.getMdStateVersion());

        if (handleFilter.isEmpty()) {
            for (MdibEntity rootEntity : mdibAccess.getRootEntities()) {
                appendStates(mdState, rootEntity);
            }
        } else {
            Set<String> handleSet = new HashSet<>(handleFilter);
            for (MdibEntity rootEntity : mdibAccess.getRootEntities()) {
                appendStatesIfMatch(mdState, rootEntity, handleSet);
            }
        }

        return mdState.build();
    }

    /**
     * Maps to an {@link MdDescription} instance.
     *
     * @param handleFilter a filter to limit the result:
     *                     <ul>
     *                     <li>If the handle reference list is empty,
     *                     all MDS descriptors are included in the result list.
     *                     <li>If a handle reference does match an MDS descriptor,
     *                     it is included in the result list.
     *                     <li>If a handle reference does not match an MDS descriptor (i.e., any other descriptor),
     *                     the MDS descriptor that is in the parent tree of the handle reference
     *                     is included in the result list.
     *                     </ul>
     * @return the mapped instance.
     */
    public MdDescription mapMdDescription(List<String> handleFilter) {
        Set<String> handleFilterCopy = new HashSet<>(handleFilter);
        final var mdDescriptionBuilder = MdDescription.builder()
            .withDescriptionVersion(mdibAccess.getMdDescriptionVersion());

        List<MdibEntity> rootEntities;
        if (handleFilter.isEmpty()) {
            rootEntities = mdibAccess.getRootEntities();
        } else {
            List<MdibEntity> allRootEntities = mdibAccess.getRootEntities();
            rootEntities = allRootEntities.stream()
                    .filter(mdibEntity -> handleFilterCopy.stream()
                            .anyMatch(handle -> mdibEntity.getHandle().equals(handle)))
                    .collect(Collectors.toList());
            rootEntities.forEach(mdibEntity -> {
                handleFilterCopy.remove(mdibEntity.getHandle());
                allRootEntities.remove(mdibEntity);
            });

            for (MdibEntity entity : allRootEntities) {
                for (String handle : handleFilterCopy) {
                    if (findHandleInSubtree(handle, entity)) {
                        rootEntities.add(entity);
                        handleFilterCopy.remove(handle);
                        break;
                    }
                }
            }
        }

        for (MdibEntity rootEntity : rootEntities) {
            mapMds(mdDescriptionBuilder, rootEntity);
        }

        return mdDescriptionBuilder.build();
    }


    private void appendStates(MdState.Builder<?> builder, MdibEntity entity) {
        builder.addState(entity.getStates());
        for (String childHandle : entity.getChildren()) {
            mdibAccess.getEntity(childHandle).ifPresent(childEntity ->
                    appendStates(builder, childEntity));
        }
    }

    private void appendStatesIfMatch(MdState.Builder<?> builder, MdibEntity entity, Set<String> filterSet) {
        if (filterSet.contains(entity.getHandle())) {
            filterSet.remove(entity.getHandle());
            builder.addState(entity.getStates());
            entity.doIfMultiState(multiStates ->
                    multiStates.forEach(state -> filterSet.remove(state.getHandle())));

        }

        entity.doIfMultiState(multiStates ->
                multiStates.forEach(state -> {
                    if (filterSet.contains(state.getHandle())) {
                        builder.addState(state);
                        filterSet.remove(state.getHandle());
                    }
                }));

        for (String childHandle : entity.getChildren()) {
            mdibAccess.getEntity(childHandle).ifPresent(childEntity ->
                    appendStatesIfMatch(builder, childEntity, filterSet));
        }
    }

    private boolean findHandleInSubtree(String handle, MdibEntity mdibEntity) {
        for (String childHandle : mdibEntity.getChildren()) {
            Optional<MdibEntity> childEntity = mdibAccess.getEntity(childHandle);
            if (childEntity.isPresent()) {
                if (childEntity.get().getHandle().equals(handle)) {
                    return true;
                }
                if (findHandleInSubtree(handle, childEntity.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void mapMds(MdDescription.Builder<?> mdDescriptionBuilder, MdibEntity mds) {
        final Optional<MdsDescriptor> descriptor = mds.getDescriptor(MdsDescriptor.class);
        if (descriptor.isEmpty()) {
            return;
        }

        //noinspection ConstantConditions
        var descriptorCopy = descriptor.get().newCopyBuilder()
            .withBattery(
                mdibAccess.getChildrenByType(mds.getHandle(), BatteryDescriptor.class)
                    .stream().map(it -> (BatteryDescriptor) it.getDescriptor()).collect(Collectors.toList())
            ).withClock(
                mdibAccess.getChildrenByType(mds.getHandle(), ClockDescriptor.class)
                    .stream()
                    .reduce((a, b) -> {
                        throw new IllegalStateException("Multiple Clocks are not allowed, found: " + a + ", " + b);
                    })
                    .map(it -> (ClockDescriptor) it.getDescriptor()).orElse(null)
            );

        mapAlertSystem(descriptorCopy, mdibAccess.getChildrenByType(mds.getHandle(),
                AlertSystemDescriptor.class));
        mapSco(descriptorCopy, mdibAccess.getChildrenByType(mds.getHandle(),
                ScoDescriptor.class));
        mapSystemContext(descriptorCopy, mdibAccess.getChildrenByType(mds.getHandle(),
                SystemContextDescriptor.class));
        mapVmds(descriptorCopy, mdibAccess.getChildrenByType(mds.getHandle(),
                VmdDescriptor.class));

        mdDescriptionBuilder.addMds(descriptorCopy.build());
    }

    private void mapVmds(MdsDescriptor.Builder<?> parent, List<MdibEntity> vmds) {
        for (MdibEntity vmd : vmds) {
            vmd.getDescriptor(VmdDescriptor.class).ifPresent(vmdDescriptor -> {
                var vmdDescriptorCopy = vmdDescriptor.newCopyBuilder();
                mapAlertSystem(vmdDescriptorCopy, mdibAccess.getChildrenByType(vmdDescriptor.getHandle(),
                        AlertSystemDescriptor.class));
                mapSco(vmdDescriptorCopy, mdibAccess.getChildrenByType(vmdDescriptor.getHandle(),
                        ScoDescriptor.class));
                mapChannels(vmdDescriptorCopy, mdibAccess.getChildrenByType(vmdDescriptor.getHandle(),
                        ChannelDescriptor.class));

                parent.addVmd(vmdDescriptorCopy.build());
            });
        }
    }

    private void mapChannels(VmdDescriptor.Builder<?> parent, List<MdibEntity> channels) {
        for (MdibEntity channel : channels) {
            channel.getDescriptor(ChannelDescriptor.class).ifPresent(channelDescriptor -> {
                var channelDescriptorCopy = channelDescriptor.newCopyBuilder()
                    .withMetric(
                        mdibAccess.getChildrenByType(channelDescriptor.getHandle(), AbstractMetricDescriptor.class)
                            .stream().map(it -> (AbstractMetricDescriptor) it.getDescriptor())
                            .collect(Collectors.toList())
                    );

                parent.addChannel(channelDescriptorCopy.build());
            });
        }
    }

    private void mapSco(AbstractComplexDeviceComponentDescriptor.Builder<?> parent, List<MdibEntity> scos) {
        for (MdibEntity sco : scos) {
            sco.getDescriptor(ScoDescriptor.class).ifPresent(scoDescriptor -> {
                var scoDescriptorCopy = scoDescriptor.newCopyBuilder()
                    .withOperation(
                        mdibAccess.getChildrenByType(sco.getHandle(), AbstractOperationDescriptor.class)
                            .stream().map(it -> (AbstractOperationDescriptor) it.getDescriptor())
                            .collect(Collectors.toList())
                    );
                parent.withSco(scoDescriptorCopy.build());
            });
            break;
        }
    }

    private void mapSystemContext(MdsDescriptor.Builder<?> parent, List<MdibEntity> systemContexts) {
        for (MdibEntity systemContext : systemContexts) {
            systemContext.getDescriptor(SystemContextDescriptor.class).ifPresent(systemContextDescriptor -> {
                String parentHandle = systemContextDescriptor.getHandle();

                @SuppressWarnings("ConstantConditions")
                var systemContextDescriptorCopy = systemContextDescriptor.newCopyBuilder()
                    .withPatientContext(
                        mdibAccess.getChildrenByType(parentHandle, PatientContextDescriptor.class)
                            .stream()
                            .reduce((a, b) -> {
                                throw new IllegalStateException(
                                    "Multiple PatientContexts are not allowed, found: " + a + ", " + b
                                );
                            })
                            .map(it -> (PatientContextDescriptor) it.getDescriptor())
                            .orElse(null)
                    )
                    .withLocationContext(
                        mdibAccess.getChildrenByType(parentHandle, LocationContextDescriptor.class)
                            .stream()
                            .reduce((a, b) -> {
                                throw new IllegalStateException(
                                    "Multiple LocationContexts are not allowed, found: " + a + ", " + b
                                );
                            })
                            .map(it -> (LocationContextDescriptor) it.getDescriptor())
                            .orElse(null)
                    )
                    .withEnsembleContext(
                        mdibAccess.getChildrenByType(parentHandle, EnsembleContextDescriptor.class)
                            .stream().map(it -> (EnsembleContextDescriptor) it.getDescriptor())
                            .collect(Collectors.toList())
                    )
                    .withWorkflowContext(
                        mdibAccess.getChildrenByType(parentHandle, WorkflowContextDescriptor.class)
                            .stream().map(it -> (WorkflowContextDescriptor) it.getDescriptor())
                            .collect(Collectors.toList())
                    )
                    .withOperatorContext(
                        mdibAccess.getChildrenByType(parentHandle, OperatorContextDescriptor.class)
                            .stream().map(it -> (OperatorContextDescriptor) it.getDescriptor())
                            .collect(Collectors.toList())
                    )
                    .withMeansContext(
                        mdibAccess.getChildrenByType(parentHandle, MeansContextDescriptor.class)
                            .stream().map(it -> (MeansContextDescriptor) it.getDescriptor())
                            .collect(Collectors.toList())
                    );

                parent.withSystemContext(systemContextDescriptorCopy.build());
            });
            break;
        }
    }

    private void mapAlertSystem(
        AbstractComplexDeviceComponentDescriptor.Builder<?> parent, List<MdibEntity> alertSystems
    ) {
        if (alertSystems.isEmpty()) {
            return;
        }

        final Optional<AlertSystemDescriptor> descriptorOpt =
                alertSystems.get(0).getDescriptor(AlertSystemDescriptor.class);
        if (descriptorOpt.isEmpty()) {
            return;
        }
        var descriptor = descriptorOpt.get();

        var descriptorCopy = descriptor.newCopyBuilder()
            .withAlertCondition(
                mdibAccess.getChildrenByType(descriptor.getHandle(), AlertConditionDescriptor.class)
                    .stream().map(it -> (AlertConditionDescriptor) it.getDescriptor()).collect(Collectors.toList())
            )
            .withAlertSignal(
                mdibAccess.getChildrenByType(descriptor.getHandle(), AlertSignalDescriptor.class)
                    .stream().map(it -> (AlertSignalDescriptor) it.getDescriptor()).collect(Collectors.toList())
            );

//        var updatedDescriptor = descriptorCopy.build();
        parent.withAlertSystem(descriptorCopy.build());
    }
}
