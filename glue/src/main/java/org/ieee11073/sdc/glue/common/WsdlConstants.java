package org.ieee11073.sdc.glue.common;

/**
 * Constants used throughout high priority and low priority WSDL files.
 * <p>
 * Final action URIs are excluded from this file.
 *
 * @see ActionConstants
 */
public class WsdlConstants {
    private static final String SLASH = "/";

    public static final String TARGET_NAMESPACE = "http://standards.ieee.org/downloads/11073/11073-20701-2018";

    public static final String ACTION_PREFIX = TARGET_NAMESPACE + SLASH;

    public static final String SERVICE_GET = "GetService";
    public static final String SERVICE_SET = "SetService";
    public static final String SERVICE_DESCRIPTION_EVENT = "DescriptionEventService";
    public static final String SERVICE_STATE_EVENT = "StateEventService";
    public static final String SERVICE_CONTEXT = "ContextService";
    public static final String SERVICE_WAVEFORM = "WaveformService";
    public static final String SERVICE_CONTAINMENT_TREE = "ContainmentTreeService";
    public static final String SERVICE_ARCHIVE = "ArchiveService";
    public static final String SERVICE_LOCALIZATION = "LocalizationService";


    public static final String ACTION_GET_PREFIX = ACTION_PREFIX + SERVICE_GET + SLASH;
    public static final String ACTION_SET_PREFIX = ACTION_PREFIX + SERVICE_SET + SLASH;
    public static final String ACTION_DESCRIPTION_EVENT_PREFIX = ACTION_PREFIX + SERVICE_DESCRIPTION_EVENT + SLASH;
    public static final String ACTION_STATE_EVENT_PREFIX = ACTION_PREFIX + SERVICE_STATE_EVENT + SLASH;
    public static final String ACTION_CONTEXT_PREFIX = ACTION_PREFIX + SERVICE_CONTEXT + SLASH;
    public static final String ACTION_WAVEFORM_PREFIX = ACTION_PREFIX + SERVICE_WAVEFORM + SLASH;
    public static final String ACTION_CONTAINMENT_TREE_PREFIX = ACTION_PREFIX + SERVICE_CONTAINMENT_TREE + SLASH;
    public static final String ACTION_ARCHIVE_PREFIX = ACTION_PREFIX + SERVICE_ARCHIVE + SLASH;
    public static final String ACTION_LOCALIZATION_PREFIX = ACTION_PREFIX + SERVICE_LOCALIZATION + SLASH;

    public static final String OPERATION_GET_MDIB = "GetMdib";
    public static final String OPERATION_GET_MD_DESCRIPTION = "GetMdDescription";
    public static final String OPERATION_GET_MD_STATE = "GetMdState";

    public static final String OPERATION_SET_VALUE = "SetValue";
    public static final String OPERATION_SET_STRING = "SetString";
    public static final String OPERATION_ACTIVATE = "Activate";
    public static final String OPERATION_SET_ALERT_STATE = "SetAlertState";
    public static final String OPERATION_SET_COMPONEN_TSTATE = "SetComponentState";
    public static final String OPERATION_SET_METRIC_STATE = "SetMetricState";
    public static final String OPERATION_OPERATION_INVOKED_REPORT = "OperationInvokedReport";

    public static final String OPERATION_DESCRIPTION_MODIFICATION_REPORT = "DescriptionModificationReport";

    public static final String OPERATION_EPISODIC_ALERT_REPORT = "EpisodicAlertReport";
    public static final String OPERATION_PERIODIC_ALERT_REPORT = "PeriodicAlertReport";
    public static final String OPERATION_EPISODIC_COMPONENT_REPORT = "EpisodicComponentReport";
    public static final String OPERATION_PERIODIC_COMPONENT_REPORT = "PeriodicComponentReport";
    public static final String OPERATION_EPISODIC_METRIC_REPORT = "EpisodicMetricReport";
    public static final String OPERATION_PERIODIC_METRIC_REPORT = "PeriodicMetricReport";
    public static final String OPERATION_EPISODIC_OPERATIONAL_STATE_REPORT = "EpisodicOperationalStateReport";
    public static final String OPERATION_PERIODIC_OPERATIONAL_STATE_REPORT = "PeriodicOperationalStateReport";
    public static final String OPERATION_SYSTEM_ERROR_REPORT = "SystemErrorReport";

    public static final String OPERATION_GET_CONTEXT_STATES = "GetContextStates";
    public static final String OPERATION_SET_CONTEXT_STATE = "SetContextState";
    public static final String OPERATION_GET_CONTEXT_STATES_BY_IDENTIFICATION = "GetContextStatesByIdentification";
    public static final String OPERATION_GET_CONTEXT_STATES_BY_FILTER = "GetContextStatesByFilter";
    public static final String OPERATION_EPISODIC_CONTEXT_REPORT = "EpisodicContextReport";
    public static final String OPERATION_PERIODIC_CONTEXT_REPORT = "PeriodicContextReport";

    public static final String OPERATION_WAVEFORM_SERVICE = "WaveformService";
    public static final String OPERATION_OBSERVED_VALUE_STREAM = "ObservedValueStream";

    public static final String OPERATION_GET_CONTAINMENT_TREE = "GetContainmentTree";
    public static final String OPERATION_GET_DESCRIPTOR = "GetDescriptor";

    public static final String OPERATION_GET_DESCRIPTORS_FROM_ARCHIVE = "GetDescriptorsFromArchive";
    public static final String OPERATION_GET_STATES_FROM_ARCHIVE = "GetStatesFromArchive";

    public static final String OPERATION_GET_LOCALIZED_TEXT = "GetLocalizedText";
    public static final String OPERATION_GET_SUPPORTED_LANGUAGES = "GetSupportedLanguages";
}
