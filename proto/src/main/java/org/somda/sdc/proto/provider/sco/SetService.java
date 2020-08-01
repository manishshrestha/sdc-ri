package org.somda.sdc.proto.provider.sco;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import org.somda.sdc.proto.mapping.participant.PojoToProtoTreeMapper;
import org.somda.sdc.proto.model.SdcMessages;
import org.somda.sdc.proto.model.SetServiceGrpc;
import org.somda.sdc.proto.model.biceps.GetMdDescriptionResponseMsg;
import org.somda.sdc.proto.model.biceps.GetMdStateResponseMsg;
import org.somda.sdc.proto.model.biceps.GetMdibResponseMsg;

import java.util.Collections;

class SetService extends SetServiceGrpc.SetServiceImplBase {

    private final PojoToProtoTreeMapper treeMapper;

    @Inject
    SetService(PojoToProtoTreeMapper treeMapper) {
        this.treeMapper = treeMapper;
    }

    @Override
    public void activate(SdcMessages.ActivateRequest request,
                         StreamObserver<SdcMessages.ActivateResponse> responseObserver) {
        super.activate(request, responseObserver);
    }

    @Override
    public void setMetricState(SdcMessages.ActivateRequest request,
                               StreamObserver<SdcMessages.ActivateResponse> responseObserver) {
        super.setMetricState(request, responseObserver);
    }

    @Override
    public void setComponentState(SdcMessages.SetComponentStateRequest request,
                                  StreamObserver<SdcMessages.SetComponentStateResponse> responseObserver) {
        super.setComponentState(request, responseObserver);
    }

    @Override
    public void setContextState(SdcMessages.SetContextStateRequest request,
                                StreamObserver<SdcMessages.SetContextStateResponse> responseObserver) {
        super.setContextState(request, responseObserver);
    }

    @Override
    public void setAlertState(SdcMessages.SetAlertStateRequest request,
                              StreamObserver<SdcMessages.SetAlertStateResponse> responseObserver) {
        super.setAlertState(request, responseObserver);
    }

    @Override
    public void setString(SdcMessages.SetStringRequest request,
                          StreamObserver<SdcMessages.SetStringResponse> responseObserver) {
        super.setString(request, responseObserver);
    }

    @Override
    public void setValue(SdcMessages.SetValueRequest request,
                         StreamObserver<SdcMessages.SetValueResponse> responseObserver) {
        super.setValue(request, responseObserver);
    }

    @Override
    public void operationInvokedReport(SdcMessages.OperationInvokedReportRequest request,
                                       StreamObserver<SdcMessages.OperationInvokedReportStream> responseObserver) {
        super.operationInvokedReport(request, responseObserver);
    }
}