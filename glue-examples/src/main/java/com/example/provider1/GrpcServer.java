package com.example.provider1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.draeger.medical.t2iapi.BasicResponses;
import com.draeger.medical.t2iapi.ResponseTypes;
import com.draeger.medical.t2iapi.activation_state.ActivationStateRequests;
import com.draeger.medical.t2iapi.activation_state.ActivationStateServiceGrpc;
import com.draeger.medical.t2iapi.alert.AlertRequests;
import com.draeger.medical.t2iapi.alert.AlertServiceGrpc;
import com.draeger.medical.t2iapi.context.ContextRequests;
import com.draeger.medical.t2iapi.context.ContextServiceGrpc;
import com.draeger.medical.t2iapi.context.ContextTypes;

import com.draeger.medical.t2iapi.device.DeviceRequests;
import com.draeger.medical.t2iapi.device.DeviceServiceGrpc;
import com.draeger.medical.t2iapi.metric.MetricRequests;
import com.draeger.medical.t2iapi.metric.MetricServiceGrpc;
import com.draeger.medical.t2iapi.metric.MetricTypes;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.ClockDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;

class GrpcServer {

    static Provider provider;
    private static final Logger LOG = LogManager.getLogger(GrpcServer.class);
    private static Server server;

    private static class ActivationServiceImpl extends ActivationStateServiceGrpc.ActivationStateServiceImplBase {

        /**
         * Set the ActivationState of the DeviceComponent or Metric
         *   with the given handle to the given ComponentActivation value.
         *
         * @param request           the Request received via GRPC.
         * @param responseObserver  the Observer taking the Response.
         */
        @Override
        public void setComponentActivation(ActivationStateRequests.SetComponentActivationRequest request,
                                           StreamObserver<BasicResponses.BasicResponse> responseObserver) {
            // TODO: remove
            System.out.println("DEBUG: setComponentActivation() called via GRPC!");

            // TODO: extract the handle and the activation from the request
//            request.getActivation()
//            GrpcServer.provider.setComponentActivation();

            // TODO: what is the difference between request.getActivation() and request.getActivationValue() ?
            // TODO: is request.getHandleBytes() the right way to retrieve the handle in question?
        }

//        @Override
//        public void setAlertActivation(ActivationStateRequests.SetAlertActivationRequest request,
//                                       StreamObserver<BasicResponses.BasicResponse> responseObserver) {
//
//            final String handle = request.getHandle();
//            final ActivationStateTypes.AlertActivation activation = request.getActivation();
//            GrpcServer.provider.setAlertActivation(handle, activation);
//
//            responseObserver.onNext(BasicResponses.BasicResponse.newBuilder().setResult(ResponseTypes.Result.RESULT_SUCCESS).build());
//            responseObserver.onCompleted();
//        }
    }

    private static class ContextServiceImpl extends ContextServiceGrpc.ContextServiceImplBase {

        /**
         * Use the given LocationDetail in a currently associated LocationContextState or associate a new LocationContextState
         *   with the LocationDetail.
         *   The goal of this rpc is to have an associated LocationContextState, that contains the given LocationDetail.
         *   This can for example be achieved by changing the location of the device in its settings UI.
         *
         * @param request           the Request received via GRPC.
         * @param responseObserver  the Observer taking the Response.
         */
        @Override
        public void setLocationDetail(ContextRequests.SetLocationDetailRequest request,
                                      StreamObserver<BasicResponses.BasicResponse> responseObserver) {
            // TODO: remove
            System.out.println("DEBUG: setLocationDetail() called via GRPC!");

            final LocationDetail newLocation = new LocationDetail();
            final ContextTypes.LocationDetail locationFromRequest = request.getLocation();
            newLocation.setRoom(locationFromRequest.getRoom().getValue());
            newLocation.setPoC(locationFromRequest.getPoc().getValue());
            newLocation.setBed(locationFromRequest.getBed().getValue());
            newLocation.setFloor(locationFromRequest.getFloor().getValue());
            newLocation.setFacility(locationFromRequest.getFacility().getValue());
            newLocation.setBuilding(locationFromRequest.getBuilding().getValue());
            try {
                GrpcServer.provider.setLocation(newLocation);
            } catch (PreprocessingException e) {
                throw new RuntimeException("Provider.setLocation failed.", e);
            }
            responseObserver.onNext(BasicResponses.BasicResponse.newBuilder().setResult(ResponseTypes.Result.RESULT_SUCCESS).build());
            responseObserver.onCompleted();
        }
    }

    protected static class DeviceServiceImpl extends DeviceServiceGrpc.DeviceServiceImplBase {

        @Override
        public void triggerReport(DeviceRequests.TriggerReportRequest request,
                                  StreamObserver<BasicResponses.BasicResponse> responseObserver) {
            // TODO: add other Reports
            switch (request.getReport()) {
                case REPORT_TYPE_EPISODIC_ALERT_REPORT: responseObserver.onNext(
                    BasicResponses.BasicResponse.newBuilder().setResult(triggerEpisodicAlertReport()).build());
                    break;
                case REPORT_TYPE_EPISODIC_METRIC_REPORT: responseObserver.onNext(
                    BasicResponses.BasicResponse.newBuilder().setResult(triggerEpisodicMetricReport()).build());
                    break;
                case REPORT_TYPE_EPISODIC_CONTEXT_REPORT: responseObserver.onNext(
                    BasicResponses.BasicResponse.newBuilder().setResult(triggerEpisodicContextReport()).build());
                    break;
                case REPORT_TYPE_EPISODIC_COMPONENT_REPORT: responseObserver.onNext(
                    BasicResponses.BasicResponse.newBuilder().setResult(triggerEpisodicComponentReport()).build());
                    break;
                case REPORT_TYPE_EPISODIC_OPERATIONAL_STATE_REPORT: responseObserver.onNext(
                    BasicResponses.BasicResponse.newBuilder().setResult(triggerEpisodicOperationalStateReport()).build());
                    break;
                default:responseObserver.onNext(
                    BasicResponses.BasicResponse.newBuilder().setResult(ResponseTypes.Result.RESULT_NOT_IMPLEMENTED).build());
            }
            responseObserver.onCompleted();
        }

        private ResponseTypes.Result triggerEpisodicOperationalStateReport() {
            final LocalMdibAccess mdibAccess = GrpcServer.provider.getMdibAccess();

            final List<MdibEntity> operations =
                new ArrayList<>(
                    mdibAccess.findEntitiesByType(AbstractOperationDescriptor.class));

            GrpcServer.provider.switchAbstractOperationStateOperatingMode(operations.get(0).getHandle());

            return ResponseTypes.Result.RESULT_SUCCESS;
        }

        private ResponseTypes.Result triggerEpisodicComponentReport() {
            final LocalMdibAccess mdibAccess = GrpcServer.provider.getMdibAccess();

            final MdibEntity clockEntity =
                new ArrayList<>(mdibAccess.findEntitiesByType(ClockDescriptor.class)).get(0);

            GrpcServer.provider.updateClockState(clockEntity.getHandle());

            return ResponseTypes.Result.RESULT_SUCCESS;
        }

        private ResponseTypes.Result triggerEpisodicContextReport() {
            final LocalMdibAccess mdibAccess = GrpcServer.provider.getMdibAccess();

            final ArrayList<MdibEntity> locationContexts =
                new ArrayList<>(mdibAccess.findEntitiesByType(LocationContextDescriptor.class));
            final LocationContextState locState = (LocationContextState) locationContexts.get(0).getStates().get(0);

            final LocationDetail locationDetail = locState.getLocationDetail();
            switchBuilding(locationDetail);
            try {
                GrpcServer.provider.setLocation(locationDetail);
            } catch (PreprocessingException ppe) {
                LOG.warn("Encountered an Exception trying to trigger an EpisodicContextReport:", ppe);
                return ResponseTypes.Result.RESULT_FAIL;
            }

            return ResponseTypes.Result.RESULT_SUCCESS;
        }

        private void switchBuilding(LocationDetail locationDetail) {
            String oldValue = locationDetail.getBuilding();
            if (oldValue == null) {
                oldValue = "defaultBuilding";
            }
            if (oldValue.toUpperCase(Locale.ROOT).equals(oldValue)) {
                locationDetail.setBuilding(oldValue.toLowerCase(Locale.ROOT));
            } else {
                locationDetail.setBuilding(oldValue.toUpperCase(Locale.ROOT));
            }
        }

        private ResponseTypes.Result triggerEpisodicMetricReport() {
            final LocalMdibAccess mdibAccess = GrpcServer.provider.getMdibAccess();

            List<MdibEntity> metrics = new ArrayList<>(
                mdibAccess.findEntitiesByType(StringMetricDescriptor.class));
            final AbstractDescriptor stringMetric = metrics.get(0).getDescriptor();

            try {
                GrpcServer.provider.changeStringMetric(stringMetric.getHandle());
            } catch (PreprocessingException ppe) {
                LOG.warn("Encountered an Exception trying to trigger an EpisodicMetricReport:", ppe);
                return ResponseTypes.Result.RESULT_FAIL;
            }
            return ResponseTypes.Result.RESULT_SUCCESS;
        }

        private ResponseTypes.Result triggerEpisodicAlertReport() {
            final LocalMdibAccess mdibAccess = GrpcServer.provider.getMdibAccess();

            // 1. find an AlertSignalDescriptor and its AlertConditionDescriptor
            final List<MdibEntity> alertSignalDescriptors = new ArrayList<>(mdibAccess
                .findEntitiesByType(AlertSignalDescriptor.class));
            AlertSignalDescriptor alertSignal = (AlertSignalDescriptor) alertSignalDescriptors.get(0).getDescriptor();
            String signalHandle = alertSignal.getHandle();
            String conditionHandle = alertSignal.getConditionSignaled();
            // 2. change their Presence to trigger an EpisodicAlertReport
            GrpcServer.provider.changeAlertSignalAndConditionPresence(signalHandle, conditionHandle);

            return ResponseTypes.Result.RESULT_SUCCESS;
        }
    }

    protected static class AlertServiceImpl extends AlertServiceGrpc.AlertServiceImplBase {

        @Override
        public void setAlertConditionPresence(AlertRequests.SetAlertConditionPresenceRequest request,
                                              StreamObserver<BasicResponses.BasicResponse> responseObserver) {

            String handle = request.getHandle();
            Boolean newValue = request.getPresence();
            LOG.info("Manipulation setAlertConditionPresence called with handle={} and newValue={}.",
                handle, newValue);
            GrpcServer.provider.setAlertConditionPresence(handle, newValue);

            responseObserver.onNext(
                BasicResponses.BasicResponse.newBuilder().setResult(ResponseTypes.Result.RESULT_SUCCESS).build());
            responseObserver.onCompleted();
        }
    }

    protected static class MetricServiceImpl extends MetricServiceGrpc.MetricServiceImplBase {

        @Override
        public void setMetricQualityValidity(MetricRequests.SetMetricQualityValidityRequest request,
                                             StreamObserver<BasicResponses.BasicResponse> responseObserver) {
            final String metricHandle = request.getHandle();
            final MetricTypes.MeasurementValidity validity = request.getValidity();

            final ResponseTypes.Result result = GrpcServer.provider.setMetricQualityValidity(metricHandle, validity);
            responseObserver.onNext(
                BasicResponses.BasicResponse.newBuilder().setResult(result).build());
            responseObserver.onCompleted();
        }
    }

    static void startGrpcServer(int port, String host, Provider provider) throws IOException {
        server = NettyServerBuilder.forAddress(new InetSocketAddress(host, port))
            .addService(new ActivationServiceImpl())
            .addService(new ContextServiceImpl())
            .addService(new DeviceServiceImpl())
            .addService(new AlertServiceImpl())
            .addService(new MetricServiceImpl())
            .build();

        server.start();

        GrpcServer.provider = provider;
    }

    static void shutdownGrpcServer() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }
}
