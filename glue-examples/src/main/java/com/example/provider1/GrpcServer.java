package com.example.provider1;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.draeger.medical.t2iapi.BasicResponses;
import com.draeger.medical.t2iapi.ResponseTypes;
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

    private static class ContextServiceImpl extends ContextServiceGrpc.ContextServiceImplBase {
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
            .addService(new ContextServiceImpl())
            .build();

        server.start();

        GrpcServer.provider = provider;
    }
}
