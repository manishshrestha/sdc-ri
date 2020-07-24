package org.somda.sdc.proto.mapping;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
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
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MeansContextDescriptor;
import org.somda.sdc.biceps.model.participant.ObjectFactory;
import org.somda.sdc.biceps.model.participant.OperatorContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.model.participant.WorkflowContextDescriptor;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.ObjectUtil;
import org.somda.sdc.glue.common.ModificationsBuilder;
import org.somda.sdc.proto.model.biceps.MdDescriptionType;
import org.somda.sdc.proto.model.biceps.MdStateType;
import org.somda.sdc.proto.model.biceps.MdibType;
import org.somda.sdc.proto.model.biceps.MdibVersionGroupType;

import javax.annotation.Nullable;
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
public class JaxbToProtoTreeMapper {
    private static final Logger LOG = LogManager.getLogger(JaxbToProtoTreeMapper.class);
    private final MdibAccess mdibAccess;
    private final ObjectFactory participantModelFactory;
    private final ObjectUtil objectUtil;
    private final JaxbToProtoNodeMapper nodeMapper;
    private final Logger instanceLogger;

    @AssistedInject
    JaxbToProtoTreeMapper(@Assisted MdibAccess mdibAccess,
                          ObjectFactory participantModelFactory,
                          ObjectUtil objectUtil,
                          JaxbToProtoNodeMapper nodeMapper,
                          @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.nodeMapper = nodeMapper;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.mdibAccess = mdibAccess;
        this.participantModelFactory = participantModelFactory;
        this.objectUtil = objectUtil;
    }

    /**
     * Maps to an {@linkplain Mdib} instance.
     * <p>
     * All information is copied from the {@link MdibAccess} given to the {@linkplain JaxbToProtoTreeMapper} on construction.
     *
     * @return a fully populated {@link Mdib} instance.
     */
    public MdibType.Mdib mapMdib() {
        var mdib = MdibType.Mdib.newBuilder();

        var mdibVersion = mdibAccess.getMdibVersion();
        var mdibVersionGroup = MdibVersionGroupType.MdibVersionGroup.newBuilder();
        setOptional(mdibVersion.getInstanceId().longValue(), mdibVersionGroup::setAInstanceId);
        setOptional(mdibVersion.getSequenceId(), mdibVersionGroup::setASequenceId);
        setOptional(mdibVersion.getVersion().longValue(), mdibVersionGroup::setAMdibVersion);

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
    public MdStateType.MdState mapMdState(List<String> handleFilter) {

        var mdState = MdStateType.MdState.newBuilder();
        mdState.setAStateVersion(mdibAccess.getMdStateVersion().longValue());

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
    public MdDescriptionType.MdDescription mapMdDescription(List<String> handleFilter) {
        // todo
        return null;
//        Set<String> handleFilterCopy = new HashSet<>(handleFilter);
//        final MdDescription mdDescription = participantModelFactory.createMdDescription();
//        mdDescription.setDescriptionVersion(mdibAccess.getMdDescriptionVersion());
//
//        List<MdibEntity> rootEntities;
//        if (handleFilter.isEmpty()) {
//            rootEntities = mdibAccess.getRootEntities();
//        } else {
//            List<MdibEntity> allRootEntities = mdibAccess.getRootEntities();
//            rootEntities = allRootEntities.stream()
//                    .filter(mdibEntity -> handleFilterCopy.stream()
//                            .filter(handle -> mdibEntity.getHandle().equals(handle)).findAny().isPresent())
//                    .collect(Collectors.toList());
//            rootEntities.forEach(mdibEntity -> {
//                handleFilterCopy.remove(mdibEntity.getHandle());
//                allRootEntities.remove(mdibEntity);
//            });
//
//            for (MdibEntity entity : allRootEntities) {
//                for (String handle : handleFilterCopy) {
//                    if (findHandleInSubtree(handle, entity)) {
//                        rootEntities.add(entity);
//                        handleFilterCopy.remove(handle);
//                        break;
//                    }
//                }
//            }
//        }
//
//        for (MdibEntity rootEntity : rootEntities) {
//            mapMds(mdDescription, rootEntity);
//        }
//
//        return mdDescription;
    }


    private void appendStates(MdStateType.MdState.Builder mdState, MdibEntity entity) {
        mdState.addAllState(entity.getStates().stream()
                .map(nodeMapper::mapAbstractStateOneOf)
                .collect(Collectors.toList()));
        for (String childHandle : entity.getChildren()) {
            mdibAccess.getEntity(childHandle).ifPresent(childEntity ->
                    appendStates(mdState, childEntity));
        }
    }

    private void appendStatesIfMatch(MdStateType.MdState.Builder mdState, MdibEntity entity, Set<String> filterSet) {
        if (filterSet.contains(entity.getHandle())) {
            filterSet.remove(entity.getHandle());
            mdState.addAllState(entity.getStates().stream()
                    .map(nodeMapper::mapAbstractStateOneOf)
                    .collect(Collectors.toList()));
            entity.doIfMultiState(multiStates ->
                    multiStates.forEach(state -> filterSet.remove(state.getHandle())));

        }

        entity.doIfMultiState(multiStates ->
                multiStates.forEach(state -> {
                    if (filterSet.contains(state.getHandle())) {
                        mdState.addState(nodeMapper.mapAbstractStateOneOf(state));
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

    private void mapMds(MdDescription mdDescription, MdibEntity mds) {
        final Optional<MdsDescriptor> descriptor = mds.getDescriptor(MdsDescriptor.class);
        if (descriptor.isEmpty()) {
            return;
        }

        MdsDescriptor descriptorCopy = objectUtil.deepCopy(descriptor.get());

        mapZeroOrMoreDescriptors(
                descriptorCopy,
                mdibAccess.getChildrenByType(mds.getHandle(), BatteryDescriptor.class),
                "getBattery");
        mapZeroOrOneDescriptor(
                descriptorCopy,
                mdibAccess.getChildrenByType(mds.getHandle(), ClockDescriptor.class),
                "setClock");
        mapAlertSystem(descriptorCopy, mdibAccess.getChildrenByType(mds.getHandle(),
                AlertSystemDescriptor.class));
        mapSco(descriptorCopy, mdibAccess.getChildrenByType(mds.getHandle(),
                ScoDescriptor.class));
        mapSystemContext(descriptorCopy, mdibAccess.getChildrenByType(mds.getHandle(),
                SystemContextDescriptor.class));
        mapVmds(descriptorCopy, mdibAccess.getChildrenByType(mds.getHandle(),
                VmdDescriptor.class));

        mdDescription.getMds().add(descriptorCopy);
    }

    private void mapVmds(MdsDescriptor parent, List<MdibEntity> vmds) {
        for (MdibEntity vmd : vmds) {
            vmd.getDescriptor(VmdDescriptor.class).ifPresent(vmdDescriptor -> {
                VmdDescriptor vmdDescriptorCopy = objectUtil.deepCopy(vmdDescriptor);
                parent.getVmd().add(vmdDescriptorCopy);
                mapAlertSystem(vmdDescriptorCopy, mdibAccess.getChildrenByType(vmdDescriptorCopy.getHandle(),
                        AlertSystemDescriptor.class));
                mapSco(vmdDescriptorCopy, mdibAccess.getChildrenByType(vmdDescriptorCopy.getHandle(),
                        ScoDescriptor.class));
                mapChannels(vmdDescriptorCopy, mdibAccess.getChildrenByType(vmdDescriptorCopy.getHandle(),
                        ChannelDescriptor.class));
            });
        }
    }

    private void mapChannels(VmdDescriptor parent, List<MdibEntity> channels) {
        for (MdibEntity channel : channels) {
            channel.getDescriptor(ChannelDescriptor.class).ifPresent(channelDescriptor -> {
                ChannelDescriptor channelDescriptorCopy = objectUtil.deepCopy(channelDescriptor);
                parent.getChannel().add(channelDescriptorCopy);
                mapZeroOrMoreDescriptors(
                        channelDescriptorCopy,
                        mdibAccess.getChildrenByType(channelDescriptorCopy.getHandle(), AbstractMetricDescriptor.class),
                        "getMetric");
            });
        }
    }

    private void mapSco(AbstractComplexDeviceComponentDescriptor parent, List<MdibEntity> scos) {
        for (MdibEntity sco : scos) {
            sco.getDescriptor(ScoDescriptor.class).ifPresent(scoDescriptor -> {
                ScoDescriptor scoDescriptorCopy = objectUtil.deepCopy(scoDescriptor);
                parent.setSco(scoDescriptorCopy);
                mapZeroOrMoreDescriptors(
                        scoDescriptorCopy,
                        mdibAccess.getChildrenByType(sco.getHandle(), AbstractOperationDescriptor.class),
                        "getOperation");
            });
            break;
        }
    }

    private void mapSystemContext(MdsDescriptor parent, List<MdibEntity> systemContexts) {
        for (MdibEntity systemContext : systemContexts) {
            systemContext.getDescriptor(SystemContextDescriptor.class).ifPresent(systemContextDescriptor -> {
                SystemContextDescriptor systemContextDescriptorCopy = objectUtil.deepCopy(systemContextDescriptor);
                parent.setSystemContext(systemContextDescriptorCopy);
                String parentHandle = systemContextDescriptorCopy.getHandle();
                mapZeroOrOneDescriptor(
                        systemContextDescriptorCopy,
                        mdibAccess.getChildrenByType(parentHandle, PatientContextDescriptor.class),
                        "setPatientContext");
                mapZeroOrOneDescriptor(
                        systemContextDescriptorCopy,
                        mdibAccess.getChildrenByType(parentHandle, LocationContextDescriptor.class),
                        "setLocationContext");
                mapZeroOrMoreDescriptors(
                        systemContextDescriptorCopy,
                        mdibAccess.getChildrenByType(parentHandle, EnsembleContextDescriptor.class),
                        "getEnsembleContext");
                mapZeroOrMoreDescriptors(
                        systemContextDescriptorCopy,
                        mdibAccess.getChildrenByType(parentHandle, WorkflowContextDescriptor.class),
                        "getWorkflowContext");
                mapZeroOrMoreDescriptors(
                        systemContextDescriptorCopy,
                        mdibAccess.getChildrenByType(parentHandle, OperatorContextDescriptor.class),
                        "getOperatorContext");
                mapZeroOrMoreDescriptors(
                        systemContextDescriptorCopy,
                        mdibAccess.getChildrenByType(parentHandle, MeansContextDescriptor.class),
                        "getMeansContext");
            });
            break;
        }
    }

    private void mapAlertSystem(AbstractComplexDeviceComponentDescriptor parent, List<MdibEntity> alertSystems) {
        if (alertSystems.isEmpty()) {
            return;
        }

        final Optional<AlertSystemDescriptor> descriptor =
                alertSystems.get(0).getDescriptor(AlertSystemDescriptor.class);
        if (descriptor.isEmpty()) {
            return;
        }

        AlertSystemDescriptor descriptorCopy = objectUtil.deepCopy(descriptor.get());

        parent.setAlertSystem(descriptorCopy);
        mapZeroOrMoreDescriptors(
                descriptorCopy,
                mdibAccess.getChildrenByType(descriptorCopy.getHandle(), AlertConditionDescriptor.class),
                "getAlertCondition");
        mapZeroOrMoreDescriptors(
                descriptorCopy,
                mdibAccess.getChildrenByType(descriptorCopy.getHandle(), AlertSignalDescriptor.class),
                "getAlertSignal");
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

    private <T> void setOptional(@Nullable T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }
}
