package com.example.provider1;

import com.draeger.medical.t2iapi.BasicResponses;
import com.draeger.medical.t2iapi.ResponseTypes;
import com.draeger.medical.t2iapi.device.DeviceRequests;
import com.draeger.medical.t2iapi.device.DeviceTypes;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;


class GrpcServerTest {

    private GrpcServer.DeviceServiceImpl classUnderTest;
    private Provider provider;
    private LocalMdibAccess mdibAccess;

    @BeforeEach
    public void setUp() throws IOException {
        this.provider = mock(Provider.class);
        this.mdibAccess = mock(LocalMdibAccess.class);
        when(provider.getMdibAccess()).thenReturn(mdibAccess);
      GrpcServer.startGrpcServer(20000, "127.0.0.1", provider);
      classUnderTest = new GrpcServer.DeviceServiceImpl();
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

        classUnderTest.triggerReport(request, responseObserver);

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

        classUnderTest.triggerReport(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verify(this.provider).changeStringMetric(metricHandle);

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

        classUnderTest.triggerReport(request, responseObserver);

        final ArgumentCaptor<BasicResponses.BasicResponse> captor =
            ArgumentCaptor.forClass(BasicResponses.BasicResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        verifyNoInteractions(this.provider);

        assertEquals(ResponseTypes.Result.RESULT_NOT_IMPLEMENTED, captor.getValue().getResult());
    }

}