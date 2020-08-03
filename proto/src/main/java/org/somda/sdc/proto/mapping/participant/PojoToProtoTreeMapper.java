package org.somda.sdc.proto.mapping.participant;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.MdDescription;
import org.somda.sdc.biceps.model.participant.MdState;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.NumericMetricDescriptor;
import org.somda.sdc.biceps.model.participant.ObjectFactory;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricDescriptor;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.SetAlertStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetComponentStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetContextStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetStringOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetValueOperationDescriptor;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.ObjectUtil;
import org.somda.sdc.glue.common.ModificationsBuilder;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractOperationDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractSetStateOperationDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AlertConditionDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.ChannelDescriptorMsg;
import org.somda.sdc.proto.model.biceps.MdDescriptionMsg;
import org.somda.sdc.proto.model.biceps.MdStateMsg;
import org.somda.sdc.proto.model.biceps.MdibMsg;
import org.somda.sdc.proto.model.biceps.MdibVersionGroupMsg;
import org.somda.sdc.proto.model.biceps.MdsDescriptorMsg;
import org.somda.sdc.proto.model.biceps.StringMetricDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.SystemContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.VmdDescriptorMsg;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Maps MDIB entities from {@linkplain MdibAccess} to an {@linkplain Mdib} object.
 * <p>
 * Use {@link ModificationsBuilder} to map from an {@linkplain Mdib} to {@linkplain MdibDescriptionModifications} in
 * order to add MDIB elements to {@link org.somda.sdc.biceps.consumer.access.RemoteMdibAccess} and
 * {@link org.somda.sdc.biceps.provider.access.LocalMdibAccess}.
 */
public class PojoToProtoTreeMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoTreeMapper.class);
    private final MdibAccess mdibAccess;
    private final ObjectFactory participantModelFactory;
    private final ObjectUtil objectUtil;
    private final PojoToProtoBaseMapper baseMapper;
    private final PojoToProtoAlertMapper alertMapper;
    private final PojoToProtoComponentMapper componentMapper;
    private final PojoToProtoContextMapper contextMapper;
    private final PojoToProtoMetricMapper metricMapper;
    private final PojoToProtoOperationMapper operationMapper;
    private final PojoToProtoOneOfMapper oneOfMapper;
    private final Logger instanceLogger;

    @AssistedInject
    PojoToProtoTreeMapper(@Assisted MdibAccess mdibAccess,
                          ObjectFactory participantModelFactory,
                          ObjectUtil objectUtil,
                          PojoToProtoBaseMapper baseMapper,
                          PojoToProtoAlertMapper alertMapper,
                          PojoToProtoComponentMapper componentMapper,
                          PojoToProtoContextMapper contextMapper,
                          PojoToProtoMetricMapper metricMapper,
                          PojoToProtoOperationMapper operationMapper,
                          PojoToProtoOneOfMapper nodeMapper,
                          @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.baseMapper = baseMapper;
        this.alertMapper = alertMapper;
        this.componentMapper = componentMapper;
        this.contextMapper = contextMapper;
        this.metricMapper = metricMapper;
        this.operationMapper = operationMapper;
        this.oneOfMapper = nodeMapper;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.mdibAccess = mdibAccess;
        this.participantModelFactory = participantModelFactory;
        this.objectUtil = objectUtil;
    }

    /**
     * Maps to an {@linkplain Mdib} instance.
     * <p>
     * All information is copied from the {@link MdibAccess} given to the {@linkplain PojoToProtoTreeMapper} on construction.
     *
     * @return a fully populated {@link Mdib} instance.
     */
    public MdibMsg mapMdib() {
        var mdib = MdibMsg.newBuilder();

        var mdibVersion = mdibAccess.getMdibVersion();
        var mdibVersionGroup = MdibVersionGroupMsg.newBuilder();
        mdibVersionGroup.setAInstanceId(Util.toUInt64(mdibVersion.getInstanceId()));
        Util.doIfNotNull(mdibVersion.getSequenceId(), mdibVersionGroup::setASequenceId);
        mdibVersionGroup.setAMdibVersion(Util.toUInt64(mdibVersion.getVersion()));

        mdib.setAMdibVersionGroup(mdibVersionGroup.build());
        mdib.setMdDescription(mapMdDescription(Collections.emptyList()));
        mdib.setMdState(mapMdState(Collections.emptyList()));
        return mdib.build();
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
    public MdStateMsg mapMdState(List<String> handleFilter) {
        var mdState = MdStateMsg.newBuilder();
        mdState.setAStateVersion(Util.toUInt64(mdibAccess.getMdStateVersion()));

        if (handleFilter.isEmpty()) {
            for (MdibEntity rootEntity : mdibAccess.getRootEntities()) {
                appendStates(mdState, rootEntity);
            }
        } else {
            var handleSet = new HashSet<>(handleFilter);
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
    public MdDescriptionMsg mapMdDescription(List<String> handleFilter) {
        var mdDescription = MdDescriptionMsg.newBuilder();
        var handleFilterCopy = new HashSet<>(handleFilter);
        mdDescription.setADescriptionVersion(Util.toUInt64(mdibAccess.getMdDescriptionVersion()));

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
            mapMds(mdDescription, rootEntity);
        }

        return mdDescription.build();
    }


    private void appendStates(MdStateMsg.Builder mdState, MdibEntity entity) {
        mdState.addAllState(entity.getStates().stream()
                .map(oneOfMapper::mapAbstractStateOneOf)
                .collect(Collectors.toList()));
        for (String childHandle : entity.getChildren()) {
            mdibAccess.getEntity(childHandle).ifPresent(childEntity ->
                    appendStates(mdState, childEntity));
        }
    }

    private void appendStatesIfMatch(MdStateMsg.Builder mdState, MdibEntity entity, Set<String> filterSet) {
        if (filterSet.contains(entity.getHandle())) {
            filterSet.remove(entity.getHandle());
            mdState.addAllState(entity.getStates().stream()
                    .map(oneOfMapper::mapAbstractStateOneOf)
                    .collect(Collectors.toList()));
            entity.doIfMultiState(multiStates ->
                    multiStates.forEach(state -> filterSet.remove(state.getHandle())));

        }

        entity.doIfMultiState(multiStates ->
                multiStates.forEach(state -> {
                    if (filterSet.contains(state.getHandle())) {
                        mdState.addState(oneOfMapper.mapAbstractStateOneOf(state));
                        filterSet.remove(state.getHandle());
                    }
                }));

        for (String childHandle : entity.getChildren()) {
            mdibAccess.getEntity(childHandle).ifPresent(childEntity ->
                    appendStatesIfMatch(mdState, childEntity, filterSet));
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

    private void mapMds(MdDescriptionMsg.Builder mdDescription, MdibEntity mds) {
        var descriptor = mds.getDescriptor(MdsDescriptor.class);
        if (descriptor.isEmpty()) {
            return;
        }

        var builder = componentMapper.mapMdsDescriptor(descriptor.get());

//        mapZeroOrMoreDescriptors(
//                descriptorCopy,
//                mdibAccess.getChildrenByType(mds.getHandle(), BatteryDescriptor.class),
//                "getBattery");
//        mapZeroOrOneDescriptor(
//                descriptorCopy,
//                mdibAccess.getChildrenByType(mds.getHandle(), ClockDescriptor.class),
//                "setClock");
        // TODO: AbstractComplexDeviceComponentDescriptorBuilder has been built in MDS mapping already -
        //  this call creates another one which might be slightly inefficient
        var compBuilder = builder.getAbstractComplexDeviceComponentDescriptorBuilder();
        mapSco(compBuilder, mdibAccess.getChildrenByType(mds.getHandle(), ScoDescriptor.class));
        mapAlertSystem(compBuilder, mdibAccess.getChildrenByType(mds.getHandle(), AlertSystemDescriptor.class));
        mapSystemContext(builder, mdibAccess.getChildrenByType(mds.getHandle(),
                SystemContextDescriptor.class));
        mapVmds(builder, mdibAccess.getChildrenByType(mds.getHandle(),
                VmdDescriptor.class));

        mdDescription.addMds(builder);
    }

    private void mapVmds(MdsDescriptorMsg.Builder parent, List<MdibEntity> vmds) {
        for (MdibEntity vmd : vmds) {
            vmd.getDescriptor(VmdDescriptor.class).ifPresent(vmdDescriptor -> {
                var builder = componentMapper.mapVmdDescriptor(vmdDescriptor);
                // TODO: redundant copy of AbstractComplexDeviceComponentDescriptorBuilder again
                var compBuilder = builder.getAbstractComplexDeviceComponentDescriptorBuilder();

                mapAlertSystem(compBuilder, mdibAccess.getChildrenByType(vmd.getHandle(), AlertSystemDescriptor.class));
                mapSco(compBuilder, mdibAccess.getChildrenByType(vmd.getHandle(), ScoDescriptor.class));
                mapChannels(builder, mdibAccess.getChildrenByType(vmdDescriptor.getHandle(), ChannelDescriptor.class));
                parent.addVmd(builder);
            });
        }
    }

    private void mapChannels(VmdDescriptorMsg.Builder parent, List<MdibEntity> channels) {
        for (MdibEntity channel : channels) {
            channel.getDescriptor(ChannelDescriptor.class).ifPresent(channelDescriptor -> {
                var builder = componentMapper.mapChannelDescriptor(channelDescriptor);

                // TODO: Handle other types.
                mapEnumStringMetricDescriptor(
                        builder,
                        mdibAccess.getChildrenByType(channelDescriptor.getHandle(), EnumStringMetricDescriptor.class)
                );
                mapStringMetricDescriptor(
                        builder,
                        mdibAccess.getChildrenByType(channelDescriptor.getHandle(), StringMetricDescriptor.class)
                );
                mapNumericMetricDescriptor(
                        builder,
                        mdibAccess.getChildrenByType(channelDescriptor.getHandle(), NumericMetricDescriptor.class)
                );
                mapRealTimeSampleArrayDescriptor(
                        builder,
                        mdibAccess.getChildrenByType(channelDescriptor.getHandle(), RealTimeSampleArrayMetricDescriptor.class)
                );

                parent.addChannel(builder);
            });
        }
    }

    private void mapRealTimeSampleArrayDescriptor(ChannelDescriptorMsg.Builder parent, List<MdibEntity> descriptors) {
        for (MdibEntity descriptor : descriptors) {
            descriptor.getDescriptor(RealTimeSampleArrayMetricDescriptor.class).ifPresent(rtsDescriptor -> {
                        var builder = metricMapper.mapRealTimeSampleArrayMetricDescriptor(rtsDescriptor);
                        parent.addMetric(AbstractMetricDescriptorOneOfMsg.newBuilder().setRealTimeSampleArrayMetricDescriptor(builder));
                    }
            );
        }
    }

    private void mapNumericMetricDescriptor(ChannelDescriptorMsg.Builder parent, List<MdibEntity> descriptors) {
        for (MdibEntity descriptor : descriptors) {
            descriptor.getDescriptor(NumericMetricDescriptor.class).ifPresent(numericMetricDescriptor -> {
                        var builder = metricMapper.mapNumericMetricDescriptor(numericMetricDescriptor);
                        parent.addMetric(AbstractMetricDescriptorOneOfMsg.newBuilder().setNumericMetricDescriptor(builder));
                    }
            );
        }
    }

    private void mapEnumStringMetricDescriptor(ChannelDescriptorMsg.Builder parent, List<MdibEntity> descriptors) {
        for (MdibEntity descriptor : descriptors) {
            descriptor.getDescriptor(EnumStringMetricDescriptor.class).ifPresent(enumStringMetricDescriptor -> {
                        var builder = metricMapper.mapEnumStringMetricDescriptor(enumStringMetricDescriptor);
                        parent.addMetric(
                                AbstractMetricDescriptorOneOfMsg.newBuilder().setStringMetricDescriptorOneOf(
                                        StringMetricDescriptorOneOfMsg.newBuilder().setEnumStringMetricDescriptor(builder)
                                )
                        );
                    }
            );
        }
    }

    private void mapStringMetricDescriptor(ChannelDescriptorMsg.Builder parent, List<MdibEntity> descriptors) {
        for (MdibEntity descriptor : descriptors) {
            descriptor.getDescriptor(StringMetricDescriptor.class)
                    // is this filter necessary? yes, because stupid inheritance
                    .filter(desc -> !(desc instanceof EnumStringMetricDescriptor)).ifPresent(stringMetricDescriptor -> {
                        var builder = metricMapper.mapStringMetricDescriptor(stringMetricDescriptor);
                        parent.addMetric(
                                AbstractMetricDescriptorOneOfMsg.newBuilder().setStringMetricDescriptorOneOf(
                                        StringMetricDescriptorOneOfMsg.newBuilder().setStringMetricDescriptor(builder)
                                )
                        );
                    }
            );
        }
    }

    private void mapSco(AbstractComplexDeviceComponentDescriptorMsg.Builder parent, List<MdibEntity> scos) {
        for (MdibEntity sco : scos) {
            sco.getDescriptor(ScoDescriptor.class).ifPresent(scoDescriptor -> {
                var scoBuilder = componentMapper.mapScoDescriptor(scoDescriptor);

                mapOperationDescriptor(
                        mdibAccess.getChildrenByType(sco.getHandle(), ActivateOperationDescriptor.class),
                        ActivateOperationDescriptor.class,
                        descriptor -> {
                            var opBuilder = operationMapper.mapActivateOperationDescriptor((descriptor));
                            scoBuilder.addOperation(AbstractOperationDescriptorOneOfMsg.newBuilder()
                                    .setAbstractSetStateOperationDescriptorOneOf(
                                            AbstractSetStateOperationDescriptorOneOfMsg.newBuilder()
                                                    .setActivateOperationDescriptor(opBuilder)));
                        });

                mapOperationDescriptor(
                        mdibAccess.getChildrenByType(sco.getHandle(), SetStringOperationDescriptor.class),
                        SetStringOperationDescriptor.class,
                        descriptor -> {
                            var opBuilder = operationMapper.mapSetStringOperationDescriptor((descriptor));
                            scoBuilder.addOperation(AbstractOperationDescriptorOneOfMsg.newBuilder()
                                    .setSetStringOperationDescriptor(opBuilder));
                        });

                mapOperationDescriptor(
                        mdibAccess.getChildrenByType(sco.getHandle(), SetValueOperationDescriptor.class),
                        SetValueOperationDescriptor.class,
                        descriptor -> {
                            var opBuilder = operationMapper.mapSetValueOperationDescriptor((descriptor));
                            scoBuilder.addOperation(AbstractOperationDescriptorOneOfMsg.newBuilder()
                                    .setSetValueOperationDescriptor(opBuilder));
                        });

                mapOperationDescriptor(
                        mdibAccess.getChildrenByType(sco.getHandle(), SetMetricStateOperationDescriptor.class),
                        SetMetricStateOperationDescriptor.class,
                        descriptor -> {
                            var opBuilder = operationMapper.mapSetMetricStateOperationDescriptor(descriptor);
                            scoBuilder.addOperation(AbstractOperationDescriptorOneOfMsg.newBuilder()
                                    .setAbstractSetStateOperationDescriptorOneOf(
                                            AbstractSetStateOperationDescriptorOneOfMsg.newBuilder()
                                                    .setSetMetricStateOperationDescriptor(opBuilder)));
                        });

                mapOperationDescriptor(
                        mdibAccess.getChildrenByType(sco.getHandle(), SetComponentStateOperationDescriptor.class),
                        SetComponentStateOperationDescriptor.class,
                        descriptor -> {
                            var opBuilder = operationMapper.mapSetComponentStateOperationDescriptor(descriptor);
                            scoBuilder.addOperation(AbstractOperationDescriptorOneOfMsg.newBuilder()
                                    .setAbstractSetStateOperationDescriptorOneOf(
                                            AbstractSetStateOperationDescriptorOneOfMsg.newBuilder()
                                                    .setSetComponentStateOperationDescriptor(opBuilder)));
                        });

                mapOperationDescriptor(
                        mdibAccess.getChildrenByType(sco.getHandle(), SetAlertStateOperationDescriptor.class),
                        SetAlertStateOperationDescriptor.class,
                        descriptor -> {
                            var opBuilder = operationMapper.mapSetAlertStateOperationDescriptor(descriptor);
                            scoBuilder.addOperation(AbstractOperationDescriptorOneOfMsg.newBuilder()
                                    .setAbstractSetStateOperationDescriptorOneOf(
                                            AbstractSetStateOperationDescriptorOneOfMsg.newBuilder()
                                                    .setSetAlertStateOperationDescriptor(opBuilder)));
                        });

                mapOperationDescriptor(
                        mdibAccess.getChildrenByType(sco.getHandle(), SetContextStateOperationDescriptor.class),
                        SetContextStateOperationDescriptor.class,
                        descriptor -> {
                            var opBuilder = operationMapper.mapSetContextStateOperationDescriptor(descriptor);
                            scoBuilder.addOperation(AbstractOperationDescriptorOneOfMsg.newBuilder()
                                    .setAbstractSetStateOperationDescriptorOneOf(
                                            AbstractSetStateOperationDescriptorOneOfMsg.newBuilder()
                                                    .setSetContextStateOperationDescriptor(opBuilder)));
                        });

                parent.setSco(scoBuilder);
            });
            break;
        }
    }

    private <T extends AbstractDescriptor> void mapOperationDescriptor(List<MdibEntity> descriptors,
                                                                       Class<T> descrClass,
                                                                       Consumer<T> doMap) {
        for (MdibEntity descriptor : descriptors) {
            descriptor.getDescriptor(descrClass).ifPresent(doMap);
        }
    }

    private void mapSystemContext(MdsDescriptorMsg.Builder parent, List<MdibEntity> systemContexts) {
        for (MdibEntity systemContext : systemContexts) {
            systemContext.getDescriptor(SystemContextDescriptor.class).ifPresent(systemContextDescriptor -> {
                var builder = componentMapper.mapSystemContextDescriptor(systemContextDescriptor);
                mapEnsembleContextDescriptor(builder, mdibAccess.getChildrenByType(systemContext.getHandle(),
                        EnsembleContextDescriptor.class));
                mapLocationContextDescriptor(builder, mdibAccess.getChildrenByType(systemContext.getHandle(),
                        LocationContextDescriptor.class));
//                mapZeroOrOneDescriptor(
//                        systemContextDescriptorCopy,
//                        mdibAccess.getChildrenByType(parentHandle, PatientContextDescriptor.class),
//                        "setPatientContext");
//                mapZeroOrOneDescriptor(
//                        systemContextDescriptorCopy,
//                        mdibAccess.getChildrenByType(parentHandle, LocationContextDescriptor.class),
//                        "setLocationContext");
//                mapZeroOrMoreDescriptors(
//                        systemContextDescriptorCopy,
//                        mdibAccess.getChildrenByType(parentHandle, EnsembleContextDescriptor.class),
//                        "getEnsembleContext");
//                mapZeroOrMoreDescriptors(
//                        systemContextDescriptorCopy,
//                        mdibAccess.getChildrenByType(parentHandle, WorkflowContextDescriptor.class),
//                        "getWorkflowContext");
//                mapZeroOrMoreDescriptors(
//                        systemContextDescriptorCopy,
//                        mdibAccess.getChildrenByType(parentHandle, OperatorContextDescriptor.class),
//                        "getOperatorContext");
//                mapZeroOrMoreDescriptors(
//                        systemContextDescriptorCopy,
//                        mdibAccess.getChildrenByType(parentHandle, MeansContextDescriptor.class),
//                        "getMeansContext");
                parent.setSystemContext(builder);
            });
            break;
        }
    }

    private void mapEnsembleContextDescriptor(SystemContextDescriptorMsg.Builder parent, List<MdibEntity> descriptors) {
        for (MdibEntity descriptor : descriptors) {
            descriptor.getDescriptor(EnsembleContextDescriptor.class).ifPresent(ensembleContextDescriptor -> {
                        var builder = contextMapper.mapEnsembleContextDescriptor(ensembleContextDescriptor);
                        parent.addEnsembleContext(builder);
                    }
            );
        }
    }

    private void mapLocationContextDescriptor(SystemContextDescriptorMsg.Builder parent, List<MdibEntity> descriptors) {
        for (MdibEntity descriptor : descriptors) {
            descriptor.getDescriptor(LocationContextDescriptor.class).ifPresent(locationContextDescriptor -> {
                        var builder = contextMapper.mapLocationContextDescriptor(locationContextDescriptor);
                        parent.setLocationContext(builder);
                    }
            );
            return;
        }
    }

    private void mapAlertSystem(
            AbstractComplexDeviceComponentDescriptorMsg.Builder parent,
            List<MdibEntity> alertSystems) {
        if (alertSystems.size() != 1) {
            return;
        }

        for (MdibEntity alertSystem : alertSystems) {
            alertSystem.getDescriptor(AlertSystemDescriptor.class).ifPresent(alertSystemDescriptor -> {
                var builder = alertMapper.mapAlertSystemDescriptor(alertSystemDescriptor);

                mdibAccess.getChildrenByType(alertSystemDescriptor.getHandle(), AlertConditionDescriptor.class)
                        .forEach(condition ->
                                condition.getDescriptor(AlertConditionDescriptor.class).ifPresent(descriptor -> {
                                    var oneOf = AlertConditionDescriptorOneOfMsg.newBuilder();
                                    if (descriptor instanceof LimitAlertConditionDescriptor) {
                                        instanceLogger.error("LimitAlertConditionDescriptor not implemented");
                                    } else {
                                        oneOf.setAlertConditionDescriptor(alertMapper.mapAlertConditionDescriptor(descriptor));
                                    }
                                    builder.addAlertCondition(oneOf.build());
                                })
                        );
                mdibAccess.getChildrenByType(alertSystemDescriptor.getHandle(), AlertSignalDescriptor.class).forEach(signal ->
                        signal.getDescriptor(AlertSignalDescriptor.class).ifPresent(signalDesc -> builder.addAlertSignal(alertMapper.mapAlertSignalDescriptor(signalDesc)))
                );

                parent.setAlertSystem(builder);
            });
        }
    }

    private void mapZeroOrMoreDescriptors(AbstractDescriptor parentDescriptor,
                                          List<MdibEntity> entities,
                                          String getterFunctionName) {

        for (MdibEntity entity : entities) {
            try {
                AbstractDescriptor childDescriptor = objectUtil.deepCopy(entity.getDescriptor());
                Method getList = parentDescriptor.getClass().getMethod(getterFunctionName);
                final List listObject = List.class.cast(getList.invoke(parentDescriptor));
                listObject.add(childDescriptor);
            } catch (ClassCastException e) {
                instanceLogger.warn("Mapping of zero-or-many failed for descriptor {}, " +
                                "because function does not return a list object",
                        entity.getDescriptor().getHandle());
            } catch (NoSuchMethodException e) {
                instanceLogger.warn("Mapping of zero-or-many failed for descriptor {}, " +
                                "because method {} does not exist",
                        entity.getDescriptor().getHandle(),
                        getterFunctionName);
            } catch (InvocationTargetException | IllegalAccessException e) {
                instanceLogger.warn("Mapping of zero-or-many failed for descriptor {}, " +
                                "because method {} could not be invoked on object of type {}",
                        entity.getDescriptor().getHandle(),
                        getterFunctionName,
                        entity.getDescriptor().getClass());
            }
        }

    }

    private void mapZeroOrOneDescriptor(AbstractDescriptor parentDescriptor,
                                        List<MdibEntity> entities,
                                        String setterFunctionName) {
        for (MdibEntity entity : entities) {
            try {
                AbstractDescriptor childDescriptor = objectUtil.deepCopy(entity.getDescriptor());
                Method setObject =
                        parentDescriptor.getClass().getMethod(setterFunctionName, childDescriptor.getClass());
                setObject.invoke(parentDescriptor, childDescriptor);
                break;
            } catch (NoSuchMethodException e) {
                instanceLogger.warn("Mapping of zero-or-one failed for descriptor {}, " +
                                "because method {} does not exist",
                        entity.getDescriptor().getHandle(),
                        setterFunctionName);
            } catch (InvocationTargetException | IllegalAccessException e) {
                instanceLogger.warn("Mapping of zero-or-one failed for descriptor {}, " +
                                "because method {} could not be invoked on object of type {}",
                        entity.getDescriptor().getHandle(),
                        setterFunctionName,
                        entity.getDescriptor().getClass());
            }
        }
    }

}