package com.example.provider1;

import com.draeger.medical.t2iapi.BasicResponses;
import com.draeger.medical.t2iapi.ResponseTypes;
import com.draeger.medical.t2iapi.context.ContextRequests;
import com.draeger.medical.t2iapi.context.ContextResponses;
import com.draeger.medical.t2iapi.context.ContextTypes;
import com.draeger.medical.t2iapi.device.DeviceRequests;
import com.draeger.medical.t2iapi.device.DeviceTypes;
import com.draeger.medical.t2iapi.metric.MetricRequests;
import com.draeger.medical.t2iapi.metric.MetricTypes;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.ClockDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.NumericMetricValue;
import org.somda.sdc.biceps.model.participant.SetValueOperationDescriptor;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;


class GrpcServerTest {

    private GrpcServer.DeviceServiceImpl deviceServiceUnderTest;
    private GrpcServer.MetricServiceImpl metricServiceUnderTest;
    private GrpcServer.ContextServiceImpl contextServiceUnderTest;
    private Provider provider;
    private LocalMdibAccess mdibAccess;

    @BeforeEach
    public void setUp() throws IOException {
        this.provider = mock(Provider.class);
        this.mdibAccess = mock(LocalMdibAccess.class);
        when(provider.getMdibAccess()).thenReturn(mdibAccess);
      GrpcServer.startGrpcServer(20000, "127.0.0.1", provider);
      deviceServiceUnderTest = new GrpcServer.DeviceServiceImpl();
      metricServiceUnderTest = new GrpcServer.MetricServiceImpl();
      contextServiceUnderTest = new GrpcServer.ContextServiceImpl();
    }

    @AfterEach
    void cleanUp() {
        GrpcServer.shutdownGrpcServer();
    }

    @SuppressWarnings("unchecked")
    @Test
    void triggerEpisodicAlertReportGood() {
        final DeviceRequests.TriggerReportRequest request = DeviceRequests.TriggerReportRequest.newBuilder()
            .setReport(DeviceTypes.ReportType.REPORT_TYPE_EPISODIC_ALERT_REPORT)
            .build();
        StreamObserver<BasicResponses.BasicResponse> responseObserver = mock(StreamObserver.class);

        MdibEntity alertSignalEntity = mock(MdibEntity.class);
        when(mdibAccess.findEntitiesByType(AlertSignalDescriptor.class))
            .thenReturn(List.of(alertSignalEntity));
        AlertSignalDescriptor alertSignalDescriptor = mock(AlertSignalDescriptor.class);
        when(alertSignalEntity.getDescriptor()).thenReturn(alertSignalDescriptor);
        final String alertSignalHandle = "alertSignalHandle";
        when(alertSignalDescriptor.getHandle()).thenReturn(alertSignalHandle);
        final String conditionHandle = "conditionHandle";
        when(alertSignalDescriptor.getConditionSignaled()).thenReturn(conditionHandle);

        deviceServiceUnderTest.triggerReport(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).changeAlertSignalAndConditionPresence(alertSignalHandle, conditionHandle);

        assertEquals(ResponseTypes.Result.RESULT_SUCCESS, captor.getValue().getResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    void triggerEpisodicMetricReportGood() throws PreprocessingException {
        final DeviceRequests.TriggerReportRequest request = DeviceRequests.TriggerReportRequest.newBuilder()
            .setReport(DeviceTypes.ReportType.REPORT_TYPE_EPISODIC_METRIC_REPORT)
            .build();
        StreamObserver<BasicResponses.BasicResponse> responseObserver = mock(StreamObserver.class);

        MdibEntity stringMetricEntity = mock(MdibEntity.class);
        when(mdibAccess.findEntitiesByType(StringMetricDescriptor.class))
            .thenReturn(List.of(stringMetricEntity));
        StringMetricDescriptor alertSignalDescriptor = mock(StringMetricDescriptor.class);
        when(stringMetricEntity.getDescriptor()).thenReturn(alertSignalDescriptor);
        final String metricHandle = "metricHandle";
        when(alertSignalDescriptor.getHandle()).thenReturn(metricHandle);

        deviceServiceUnderTest.triggerReport(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).changeStringMetric(metricHandle);

        assertEquals(ResponseTypes.Result.RESULT_SUCCESS, captor.getValue().getResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    void triggerEpisodicContextReportGood() throws PreprocessingException {
        final DeviceRequests.TriggerReportRequest request = DeviceRequests.TriggerReportRequest.newBuilder()
            .setReport(DeviceTypes.ReportType.REPORT_TYPE_EPISODIC_CONTEXT_REPORT)
            .build();
        StreamObserver<BasicResponses.BasicResponse> responseObserver = mock(StreamObserver.class);

        MdibEntity locationContextEntity = mock(MdibEntity.class);
        when(mdibAccess.findEntitiesByType(LocationContextDescriptor.class))
            .thenReturn(List.of(locationContextEntity));
        LocationContextDescriptor locationContextDescriptor = mock(LocationContextDescriptor.class);
        when(locationContextEntity.getDescriptor()).thenReturn(locationContextDescriptor);
        final String locationContextHandle = "locationContextHandle";
        when(locationContextDescriptor.getHandle()).thenReturn(locationContextHandle);
        LocationContextState locationContextState = mock(LocationContextState.class);
        when(locationContextEntity.getStates()).thenReturn(List.of(locationContextState));
        LocationDetail locationDetail = mock(LocationDetail.class);
        when(locationContextState.getLocationDetail()).thenReturn(locationDetail);
        when(locationDetail.getBuilding()).thenReturn("building");

        deviceServiceUnderTest.triggerReport(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).setLocation(any());

        assertEquals(ResponseTypes.Result.RESULT_SUCCESS, captor.getValue().getResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    void triggerEpisodicComponentReportGood() {
        final DeviceRequests.TriggerReportRequest request = DeviceRequests.TriggerReportRequest.newBuilder()
            .setReport(DeviceTypes.ReportType.REPORT_TYPE_EPISODIC_COMPONENT_REPORT)
            .build();
        StreamObserver<BasicResponses.BasicResponse> responseObserver = mock(StreamObserver.class);

        MdibEntity clockEntity = mock(MdibEntity.class);
        when(mdibAccess.findEntitiesByType(ClockDescriptor.class))
            .thenReturn(List.of(clockEntity));
        final String clockHandle = "clockHandle";
        when(clockEntity.getHandle()).thenReturn(clockHandle);

        deviceServiceUnderTest.triggerReport(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).updateClockState(clockHandle);

        assertEquals(ResponseTypes.Result.RESULT_SUCCESS, captor.getValue().getResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    void triggerEpisodicOperationalStateReportGood() {
        final DeviceRequests.TriggerReportRequest request = DeviceRequests.TriggerReportRequest.newBuilder()
            .setReport(DeviceTypes.ReportType.REPORT_TYPE_EPISODIC_OPERATIONAL_STATE_REPORT)
            .build();
        StreamObserver<BasicResponses.BasicResponse> responseObserver = mock(StreamObserver.class);

        MdibEntity operationEntity = mock(MdibEntity.class);
        when(mdibAccess.findEntitiesByType(AbstractOperationDescriptor.class))
            .thenReturn(List.of(operationEntity));
        final String opHandle = "opHandle";
        when(operationEntity.getHandle()).thenReturn(opHandle);

        deviceServiceUnderTest.triggerReport(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).switchAbstractOperationStateOperatingMode(opHandle);

        assertEquals(ResponseTypes.Result.RESULT_SUCCESS, captor.getValue().getResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    void triggerOperationInvokedReportGood()
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final DeviceRequests.TriggerReportRequest request = DeviceRequests.TriggerReportRequest.newBuilder()
            .setReport(DeviceTypes.ReportType.REPORT_TYPE_OPERATION_INVOKED_REPORT)
            .build();
        StreamObserver<BasicResponses.BasicResponse> responseObserver = mock(StreamObserver.class);

        MdibEntity operationEntity = mock(MdibEntity.class);
        final SetValueOperationDescriptor opDescriptor = mock(SetValueOperationDescriptor.class);
        when(mdibAccess.findEntitiesByType(SetValueOperationDescriptor.class))
            .thenReturn(List.of(operationEntity));
        final String opHandle = "opHandle";
        final String metricHandle = "metricHandle";
        final NumericMetricState state = mock(NumericMetricState.class);
        final NumericMetricValue metricValue = mock(NumericMetricValue.class);
        when(operationEntity.getHandle()).thenReturn(opHandle);
        when(operationEntity.getDescriptor()).thenReturn(opDescriptor);
        when(opDescriptor.getOperationTarget()).thenReturn(metricHandle);
        when(mdibAccess.getState(metricHandle, NumericMetricState.class)).thenReturn(
            Optional.of(state));
        when(state.getMetricValue()).thenReturn(metricValue);
        final BigDecimal value = BigDecimal.ONE;
        when(metricValue.getValue()).thenReturn(value);

        deviceServiceUnderTest.triggerReport(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).invokeNumericSetValueOperation(opHandle, value);

        assertEquals(ResponseTypes.Result.RESULT_SUCCESS, captor.getValue().getResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    void triggerUnknownReportBad() {
        final DeviceRequests.TriggerReportRequest request = DeviceRequests.TriggerReportRequest.newBuilder()
            .setReport(DeviceTypes.ReportType.REPORT_TYPE_OBSERVED_VALUE_STREAM)
            .build();
        StreamObserver<BasicResponses.BasicResponse> responseObserver = mock(StreamObserver.class);

        MdibEntity alertSignalEntity = mock(MdibEntity.class);
        when(mdibAccess.findEntitiesByType(AlertSignalDescriptor.class))
            .thenReturn(List.of(alertSignalEntity));
        AlertSignalDescriptor alertSignalDescriptor = mock(AlertSignalDescriptor.class);
        when(alertSignalEntity.getDescriptor()).thenReturn(alertSignalDescriptor);
        final String alertSignalHandle = "alertSignalHandle";
        when(alertSignalDescriptor.getHandle()).thenReturn(alertSignalHandle);
        final String conditionHandle = "conditionHandle";
        when(alertSignalDescriptor.getConditionSignaled()).thenReturn(conditionHandle);

        deviceServiceUnderTest.triggerReport(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verifyNoInteractions(this.provider);

        assertEquals(ResponseTypes.Result.RESULT_NOT_IMPLEMENTED, captor.getValue().getResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    void setMetricQualityValidityGood() {
        String handle = "metricHandle";
        MetricTypes.MeasurementValidity validity = MetricTypes.MeasurementValidity.MEASUREMENT_VALIDITY_VALID;
        final MetricRequests.SetMetricQualityValidityRequest request = MetricRequests.SetMetricQualityValidityRequest.newBuilder()
            .setHandle(handle)
            .setValidity(validity)
            .build();
        StreamObserver<BasicResponses.BasicResponse> responseObserver = mock(StreamObserver.class);

        when(provider.setMetricQualityValidity(any(), any())).thenReturn(ResponseTypes.Result.RESULT_SUCCESS);

        metricServiceUnderTest.setMetricQualityValidity(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        // verify(this.provider).updateClockState(clockHandle);

        assertEquals(ResponseTypes.Result.RESULT_SUCCESS, captor.getValue().getResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    void createContextStateWithAssociationGood()
        throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        String contextHandle = "contextHandle";
        final ContextTypes.ContextAssociation contextAssociation =
            ContextTypes.ContextAssociation.CONTEXT_ASSOCIATION_ASSOCIATED;
        final ContextRequests.CreateContextStateWithAssociationRequest request =
            ContextRequests.CreateContextStateWithAssociationRequest.newBuilder()
                .setContextAssociation(contextAssociation)
                .setDescriptorHandle(contextHandle)
                .build();
        StreamObserver<ContextResponses.CreateContextStateWithAssociationResponse>
            responseObserver = mock(StreamObserver.class);

        String contextStateHandle = "newContextStateHandle";
        when(provider.createContextStateWithAssociation(any(), any())).thenReturn(contextStateHandle);

        contextServiceUnderTest.createContextStateWithAssociation(request, responseObserver);

        final ArgumentCaptor<ContextResponses.CreateContextStateWithAssociationResponse> captor =
            ArgumentCaptor.forClass(ContextResponses.CreateContextStateWithAssociationResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).createContextStateWithAssociation(contextHandle, contextAssociation);

        assertEquals(ResponseTypes.Result.RESULT_SUCCESS, captor.getValue().getStatus().getResult());
        assertEquals(contextStateHandle, captor.getValue().getContextStateHandle());
    }

    @SuppressWarnings("unchecked")
    @Test
    void createContextStateWithAssociationBad()
        throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        String contextHandle = "contextHandle";
        final ContextTypes.ContextAssociation contextAssociation =
            ContextTypes.ContextAssociation.CONTEXT_ASSOCIATION_ASSOCIATED;
        final ContextRequests.CreateContextStateWithAssociationRequest request =
            ContextRequests.CreateContextStateWithAssociationRequest.newBuilder()
                .setContextAssociation(contextAssociation)
                .setDescriptorHandle(contextHandle)
                .build();
        StreamObserver<ContextResponses.CreateContextStateWithAssociationResponse>
            responseObserver = mock(StreamObserver.class);

        when(provider.createContextStateWithAssociation(any(), any())).thenReturn(null);

        contextServiceUnderTest.createContextStateWithAssociation(request, responseObserver);

        final ArgumentCaptor<ContextResponses.CreateContextStateWithAssociationResponse> captor =
            ArgumentCaptor.forClass(ContextResponses.CreateContextStateWithAssociationResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).createContextStateWithAssociation(contextHandle, contextAssociation);

        assertEquals(ResponseTypes.Result.RESULT_FAIL, captor.getValue().getStatus().getResult());
        assertEquals("", captor.getValue().getContextStateHandle());
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendHelloGood()
        throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        String contextHandle = "contextHandle";
        final ContextTypes.ContextAssociation contextAssociation =
            ContextTypes.ContextAssociation.CONTEXT_ASSOCIATION_ASSOCIATED;
        final Empty request = Empty.newBuilder().build();
        StreamObserver<BasicResponses.BasicResponse>
            responseObserver = mock(StreamObserver.class);

        when(provider.createContextStateWithAssociation(any(), any())).thenReturn(null);

        deviceServiceUnderTest.sendHello(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).sendHello();

        assertEquals(ResponseTypes.Result.RESULT_SUCCESS, captor.getValue().getResult());
    }

}