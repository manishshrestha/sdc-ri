package com.example.provider1;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.draeger.medical.t2iapi.BasicResponses;
import com.draeger.medical.t2iapi.ResponseTypes;
import com.draeger.medical.t2iapi.activation_state.ActivationStateRequests;
import com.draeger.medical.t2iapi.activation_state.ActivationStateServiceGrpc;
import com.draeger.medical.t2iapi.activation_state.ActivationStateTypes;
import com.draeger.medical.t2iapi.context.ContextRequests;
import com.draeger.medical.t2iapi.context.ContextServiceGrpc;
import com.draeger.medical.t2iapi.context.ContextTypes;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.LocationDetail;

class GrpcServer {

    static Provider provider;

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

//            request.getActivation()
//            GrpcServer.provider.setComponentActivation();

            // TODO: what is the difference between request.getActivation() and request.getActivationValue() ?
            // TODO: is request.getHandleBytes() the right way to retrieve the handle in question?


        }
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

    static void startGrpcServer(int port, String host, Provider provider) throws IOException {
        Server server = NettyServerBuilder.forAddress(new InetSocketAddress(host, port))
            .addService(new ActivationServiceImpl())
            .addService(new ContextServiceImpl())
            .build();

        server.start();

        GrpcServer.provider = provider;
    }
}
