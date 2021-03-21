package org.somda.sdc.proto.mapping.participant;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.google.protobuf.MessageOrBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.glue.common.DefaultStateValues;
import org.somda.sdc.glue.common.MdibMapper;
import org.somda.sdc.glue.common.RequiredDefaultStateValues;
import org.somda.sdc.glue.common.helper.DefaultStateValuesDispatcher;
import org.somda.protosdc.proto.model.biceps.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class to create an {@linkplain MdibDescriptionModifications} object from an {@linkplain Mdib} container.
 * <p>
 * <em>Important note: the MDIB passed to the {@linkplain ProtoToPojoModificationsBuilder} will be modified. Make sure to pass a
 * copy if necessary.</em>
 * <p>
 * Use {@link MdibMapper} to map from {@linkplain MdibAccess} to an {@linkplain Mdib} object.
 */
public class ProtoToPojoModificationsBuilder {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoModificationsBuilder.class);

    private final ArrayListMultimap<String, AbstractState> states;
    private final MdibDescriptionModifications modifications;

    private final Boolean createSingleStateIfMissing;
    private final MdibTypeValidator typeValidator;
    private final DefaultStateValuesDispatcher defaultStateValuesDispatcher;
    private final Logger instanceLogger;
    private final ProtoToPojoOneOfMapper nodeMapper;

    @AssistedInject
    ProtoToPojoModificationsBuilder(@Assisted MdibMsg mdib,
                                    MdibTypeValidator typeValidator,
                                    ProtoToPojoOneOfMapper nodeMapper,
                                    @Named(org.somda.sdc.common.CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this(mdib, false, null, typeValidator, nodeMapper, frameworkIdentifier);
    }

    @AssistedInject
    ProtoToPojoModificationsBuilder(@Assisted MdibMsg mdib,
                                    @Assisted Boolean createSingleStateIfMissing,
                                    MdibTypeValidator typeValidator,
                                    ProtoToPojoOneOfMapper nodeMapper,
                                    @Named(org.somda.sdc.common.CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this(mdib, createSingleStateIfMissing, null, typeValidator, nodeMapper, frameworkIdentifier);
    }

    @AssistedInject
    ProtoToPojoModificationsBuilder(@Assisted MdibMsg mdib,
                                    @Assisted Boolean createSingleStateIfMissing,
                                    @Assisted @Nullable DefaultStateValues defaultStateValues,
                                    MdibTypeValidator typeValidator,
                                    ProtoToPojoOneOfMapper nodeMapper,
                                    @Named(org.somda.sdc.common.CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.createSingleStateIfMissing = createSingleStateIfMissing;
        this.typeValidator = typeValidator;
        this.nodeMapper = nodeMapper;
        var copyDefaultStateValues = defaultStateValues;
        if (copyDefaultStateValues == null) {
            copyDefaultStateValues = new RequiredDefaultStateValues();
        }
        this.defaultStateValuesDispatcher = new DefaultStateValuesDispatcher(copyDefaultStateValues);

        if (!createSingleStateIfMissing && !mdib.hasMdState()) {
            throw new RuntimeException("No states found but required. " +
                    "Try using createSingleStateIfMissing=false to auto-create states");
        }

        this.states = ArrayListMultimap.create();
        if (mdib.hasMdState()) {
            mdib.getMdState().getStateList().forEach(state -> {
                var mappedState = nodeMapper.map(state);
                states.put(mappedState.getDescriptorHandle(), mappedState);
            });
        }

        this.modifications = MdibDescriptionModifications.create();
        mdib.getMdDescription().getMdsList().forEach(this::build);
    }

    /**
     * Gets the precompiled {@linkplain MdibDescriptionModifications}.
     *
     * @return the built {@linkplain MdibDescriptionModifications}. As the build process takes place right after object
     * construction, this method returns a pre-assembled object structure.
     */
    public MdibDescriptionModifications get() {
        return modifications;
    }

    private void build(MdsDescriptorMsg mds) {
        var parent = insert(mds, MdsDescriptor.class, null);

        // Order of insertion shall be the same as the order from the MDIB XML Schema
        instanceLogger.error("Missing mapping for MDS alert system");
        build(mds.getAbstractComplexDeviceComponentDescriptor(), parent);
        instanceLogger.error("Missing mapping for system context");
        if (mds.hasSystemContext()) {
            build(mds.getSystemContext(), parent);
        }
        parent.setSystemContext(null);
        instanceLogger.error("Missing mapping for clock");
//        buildLeaf(mds.getClock(), mds);
//        mds.setClock(null);
        instanceLogger.error("Missing mapping for battery");
//        mds.getBattery().forEach(descr -> buildLeaf(descr, mds));
//        mds.setBattery(null);

        mds.getVmdList().forEach(descr -> build(descr, parent));
        parent.setVmd(Collections.emptyList());
    }

    private void build(AbstractComplexDeviceComponentDescriptorMsg protoMsg, AbstractComplexDeviceComponentDescriptor descriptor) {
        if (protoMsg.hasAlertSystem()) {
            build(protoMsg.getAlertSystem(), descriptor);
        }
        descriptor.setAlertSystem(null);

        if (protoMsg.hasSco()) {
            build(protoMsg.getSco(), descriptor);
        }
        descriptor.setSco(null);
    }

    private void build(AlertSystemDescriptorMsg alertSystem, AbstractComplexDeviceComponentDescriptor parent) {
        var addedDesc = insert(alertSystem, AlertSystemDescriptor.class, parent.getHandle());

        alertSystem.getAlertConditionList().forEach(condition -> build(condition, addedDesc));
        alertSystem.getAlertSignalList().forEach(signal -> build(signal, addedDesc));

        addedDesc.setAlertSignal(Collections.emptyList());
        addedDesc.setAlertCondition(Collections.emptyList());
    }

    private void build(ScoDescriptorMsg sco, AbstractComplexDeviceComponentDescriptor parent) {
        var addedDesc = insert(sco, ScoDescriptor.class, parent.getHandle());

        sco.getOperationList().forEach(it -> {
            var type = it.getAbstractOperationDescriptorOneOfCase();
            switch (type) {
                case SET_STRING_OPERATION_DESCRIPTOR:
                    insert(it.getSetStringOperationDescriptor(),
                            SetStringOperationDescriptor.class,
                            addedDesc.getHandle());
                    break;
                case SET_ALERT_STATE_OPERATION_DESCRIPTOR:
                    insert(it.getSetAlertStateOperationDescriptor(),
                            SetAlertStateOperationDescriptor.class,
                            addedDesc.getHandle());
                    break;
                case SET_COMPONENT_STATE_OPERATION_DESCRIPTOR:
                    insert(it.getSetComponentStateOperationDescriptor(),
                            SetComponentStateOperationDescriptor.class,
                            addedDesc.getHandle());
                    break;
                case SET_METRIC_STATE_OPERATION_DESCRIPTOR:
                    insert(it.getSetMetricStateOperationDescriptor(),
                            SetMetricStateOperationDescriptor.class,
                            addedDesc.getHandle());
                    break;
                case SET_CONTEXT_STATE_OPERATION_DESCRIPTOR:
                    insert(it.getSetContextStateOperationDescriptor(),
                            SetContextStateOperationDescriptor.class,
                            addedDesc.getHandle());
                    break;
                case SET_VALUE_OPERATION_DESCRIPTOR:
                    insert(it.getSetValueOperationDescriptor(),
                            SetValueOperationDescriptor.class,
                            addedDesc.getHandle());
                    break;
                case ACTIVATE_OPERATION_DESCRIPTOR:
                    insert(it.getActivateOperationDescriptor(),
                            ActivateOperationDescriptor.class,
                        addedDesc.getHandle());
                case ABSTRACT_SET_STATE_OPERATION_DESCRIPTOR:
                case ABSTRACT_OPERATION_DESCRIPTOR:
                case ABSTRACTOPERATIONDESCRIPTORONEOF_NOT_SET:
                default:
                    instanceLogger.error("Case not implemented {}", type);
            }
        });
    }

    private void build(AbstractSetStateOperationDescriptorOneOfMsg setStateOnOf, String parentHandle) {
        var type = setStateOnOf.getAbstractSetStateOperationDescriptorOneOfCase();
        switch (type) {
            case SET_COMPONENT_STATE_OPERATION_DESCRIPTOR:
                insert(setStateOnOf.getSetComponentStateOperationDescriptor(),
                        SetComponentStateOperationDescriptor.class,
                        parentHandle);
                break;
            case SET_ALERT_STATE_OPERATION_DESCRIPTOR:
                insert(setStateOnOf.getSetAlertStateOperationDescriptor(), SetAlertStateOperationDescriptor.class,
                        parentHandle);
                break;
            case SET_METRIC_STATE_OPERATION_DESCRIPTOR:
                insert(setStateOnOf.getSetMetricStateOperationDescriptor(), SetMetricStateOperationDescriptor.class,
                        parentHandle);
                break;
            case SET_CONTEXT_STATE_OPERATION_DESCRIPTOR:
                insert(setStateOnOf.getSetContextStateOperationDescriptor(), SetContextStateOperationDescriptor.class,
                        parentHandle);
                break;
            case ACTIVATE_OPERATION_DESCRIPTOR:
                insert(setStateOnOf.getActivateOperationDescriptor(), ActivateOperationDescriptor.class,
                        parentHandle);
                break;
            case ABSTRACT_SET_STATE_OPERATION_DESCRIPTOR:
            case ABSTRACTSETSTATEOPERATIONDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Case not implemented {}", type);
        }
    }

    private void build(AlertSignalDescriptorMsg signalMsg, AlertSystemDescriptor parent) {
        var addedDescr = insert(signalMsg, AlertSignalDescriptor.class, parent.getHandle());
    }

    private void build(AlertConditionDescriptorOneOfMsg conditionMsg, AlertSystemDescriptor parent) {
        var type = conditionMsg.getAlertConditionDescriptorOneOfCase();
        switch (type) {
            case ALERT_CONDITION_DESCRIPTOR:
                build(conditionMsg.getAlertConditionDescriptor(), parent);
                break;
            case LIMIT_ALERT_CONDITION_DESCRIPTOR:
                build(conditionMsg.getLimitAlertConditionDescriptor(), parent);
                break;
            default:
                instanceLogger.error("Case not implemented {}", type);
                break;
        }
    }

    private void build(AlertConditionDescriptorMsg conditionMsg, AlertSystemDescriptor parent) {
        var addedDescr = insert(conditionMsg, AlertConditionDescriptor.class, parent.getHandle());
    }

    private void build(LimitAlertConditionDescriptorMsg conditionMsg, AlertSystemDescriptor parent) {
        var addedDescr = insert(conditionMsg, LimitAlertConditionDescriptor.class, parent.getHandle());
    }



    private void build(SystemContextDescriptorMsg systemContext, MdsDescriptor parent) {
        var addedDescr = insert(systemContext, SystemContextDescriptor.class, parent.getHandle());

        insertMulti(systemContext.getLocationContext(), LocationContextDescriptor.class, addedDescr.getHandle());
        insertMulti(systemContext.getPatientContext(), PatientContextDescriptor.class, addedDescr.getHandle());
        addedDescr.setLocationContext(null);
//        buildMultiStateLeaf(systemContext.getPatientContext(), systemContext);
        addedDescr.setPatientContext(null);
//        systemContext.getEnsembleContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        systemContext.getEnsembleContextList().forEach(it ->
                insertMulti(it, EnsembleContextDescriptor.class, addedDescr.getHandle()));
        addedDescr.setEnsembleContext(null);
//        systemContext.getWorkflowContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        addedDescr.setWorkflowContext(null);
//        systemContext.getOperatorContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        addedDescr.setOperatorContext(null);
//        addedDescr.getMeansContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        addedDescr.setMeansContext(null);
    }

//
//    private void build(@Nullable AlertSystemDescriptor alertSystem, AbstractDescriptor parent) {
//        if (alertSystem == null) {
//            return;
//        }
//        insert(alertSystem, parent);
//
//        alertSystem.getAlertCondition().forEach(descr -> buildLeaf(descr, alertSystem));
//        alertSystem.setAlertCondition(null);
//        alertSystem.getAlertSignal().forEach(descr -> buildLeaf(descr, alertSystem));
//        alertSystem.setAlertSignal(null);
//    }

    private void build(VmdDescriptorMsg vmd, MdsDescriptor parent) {
        var addedDescr = insert(vmd, VmdDescriptor.class, parent.getHandle());

        instanceLogger.error("Missing mapping for VMD SCO");
//        build(vmd.getSco(), vmd);
//        vmd.setSco(null);
        build(vmd.getAbstractComplexDeviceComponentDescriptor(), parent);

        vmd.getChannelList().forEach(descr -> build(descr, addedDescr));
        addedDescr.setChannel(Collections.emptyList());
    }

    private void build(ChannelDescriptorMsg channel, VmdDescriptor parent) {
        var addedDescr = insert(channel, ChannelDescriptor.class, parent.getHandle());

        channel.getMetricList().forEach(metric -> build(metric, addedDescr));
        addedDescr.setMetric(Collections.emptyList());
    }

    private void build(AbstractMetricDescriptorOneOfMsg metric, ChannelDescriptor parent) {
        var type = metric.getAbstractMetricDescriptorOneOfCase();
        switch (type) {
            case ENUM_STRING_METRIC_DESCRIPTOR:
                build(metric.getEnumStringMetricDescriptor(), parent);
                break;
            case STRING_METRIC_DESCRIPTOR:
                build(metric.getStringMetricDescriptor(), parent);
                break;
            case NUMERIC_METRIC_DESCRIPTOR:
                build(metric.getNumericMetricDescriptor(), parent);
                break;
            case REAL_TIME_SAMPLE_ARRAY_METRIC_DESCRIPTOR:
                build(metric.getRealTimeSampleArrayMetricDescriptor(), parent);
                break;
            case DISTRIBUTION_SAMPLE_ARRAY_METRIC_DESCRIPTOR:
            default:
                instanceLogger.error("Missing mapping for {}", type);
                break;
        }

    }

    private void build(StringMetricDescriptorOneOfMsg metric, ChannelDescriptor parent) {
        var type = metric.getStringMetricDescriptorOneOfCase();
        switch (type) {
            case STRING_METRIC_DESCRIPTOR:
                build(metric.getStringMetricDescriptor(), parent);
                break;
            case ENUM_STRING_METRIC_DESCRIPTOR:
                build(metric.getEnumStringMetricDescriptor(), parent);
                break;
        }
    }

    private void build(RealTimeSampleArrayMetricDescriptorMsg metric, ChannelDescriptor parent) {
        var addedDesc = insert(metric, RealTimeSampleArrayMetricDescriptor.class, parent.getHandle());
    }

    private void build(NumericMetricDescriptorMsg metric, ChannelDescriptor parent) {
        var addedDesc = insert(metric, NumericMetricDescriptor.class, parent.getHandle());
    }

    private void build(EnumStringMetricDescriptorMsg metric, ChannelDescriptor parent) {
        var addedDesc = insert(metric, EnumStringMetricDescriptor.class, parent.getHandle());
    }

    private void build(StringMetricDescriptorMsg metric, ChannelDescriptor parent) {
        var addedDesc = insert(metric, StringMetricDescriptor.class, parent.getHandle());
    }

    private <T extends AbstractDescriptor> void buildLeaf(@Nullable T descriptor, AbstractDescriptor parent) {
        if (descriptor != null) {
            modifications.insert(descriptor, singleState(descriptor), parent.getHandle());
        }
    }

    private <T extends AbstractDescriptor> void buildMultiStateLeaf(@Nullable T descriptor, AbstractDescriptor parent) {
        if (descriptor != null) {
            modifications.insert(descriptor, multiStates(descriptor), parent.getHandle());
        }
    }

    private AbstractState singleState(AbstractDescriptor descriptor) {
        var statesCollection = states.get(descriptor.getHandle());
        if (statesCollection.isEmpty()) {
            if (createSingleStateIfMissing) {
                try {
                    var state = typeValidator.resolveStateType(descriptor.getClass()).getConstructor().newInstance();
                    defaultStateValuesDispatcher.dispatchDefaultStateValues(state);
                    return state;
                } catch (Exception e) {
                    instanceLogger.warn(
                            "Could not create state for {} with handle {}",
                            descriptor.getClass().getSimpleName(), descriptor.getHandle(), e
                    );
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("No state found for descriptor handle %s",
                        descriptor.getHandle()));
            }
        }

        return statesCollection.get(0);
    }

    private List<AbstractMultiState> multiStates(AbstractDescriptor descriptor) {
        final List<AbstractState> abstractStates = states.get(descriptor.getHandle());
        ArrayList<AbstractMultiState> abstractMultiStates = new ArrayList<>(abstractStates.size());
        abstractStates.forEach(abstractState -> {
            if (abstractState instanceof AbstractMultiState) {
                abstractMultiStates.add((AbstractMultiState) abstractState);
            }
        });

        return abstractMultiStates;
    }

    private <T extends AbstractDescriptor> T insert(MessageOrBuilder descriptor,
                                                    Class<T> resultType,
                                                    @Nullable String parentHandle) {
        var descriptorToInsert = resultType.cast(nodeMapper.mapDescriptor(descriptor));
        modifications.insert(descriptorToInsert, singleState(descriptorToInsert), parentHandle);
        return descriptorToInsert;
    }

    private <T extends AbstractDescriptor> T insertMulti(MessageOrBuilder descriptor,
                                                         Class<T> resultType,
                                                         @Nullable String parentHandle) {
        var descriptorToInsert = resultType.cast(nodeMapper.mapDescriptor(descriptor));
        modifications.insert(descriptorToInsert, multiStates(descriptorToInsert), parentHandle);
        return descriptorToInsert;
    }
}