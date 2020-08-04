package it.org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import dpws_test_service.messages._2017._05._10.ObjectFactory;
import dpws_test_service.messages._2017._05._10.TestNotification;
import dpws_test_service.messages._2017._05._10.TestOperationRequest;
import dpws_test_service.messages._2017._05._10.TestOperationResponse;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.RequestResponseObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;


public class DpwsTestService1 extends WebService {

    private final WsAddressingUtil wsaUtil;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory objectFactory;
    private TransportInfoCallback transportInfoCallback;

    @Inject
    DpwsTestService1(WsAddressingUtil wsaUtil,
                     SoapUtil soapUtil,
                     SoapFaultFactory soapFaultFactory,
                     ObjectFactory objectFactory) {
        this.wsaUtil = wsaUtil;

        this.soapUtil = soapUtil;
        this.soapFaultFactory = soapFaultFactory;
        this.objectFactory = objectFactory;

        this.transportInfoCallback = null;
    }

    public void sendNotification(TestNotification notificationToSend) throws MarshallingException, TransportException {
        sendNotification(TestServiceMetadata.ACTION_NOTIFICATION_1, notificationToSend);
        sendNotification(TestServiceMetadata.ACTION_NOTIFICATION_2, notificationToSend);
    }

    public void setTransportInfoCallback(TransportInfoCallback transportInfoCallback) {
        this.transportInfoCallback = transportInfoCallback;
    }

    @MessageInterceptor(TestServiceMetadata.ACTION_OPERATION_REQUEST_1)
    void onTestOperation1(RequestResponseObject rrObj) throws SoapFaultException {
        rrObj.getResponse().getWsAddressingHeader()
                .setAction(wsaUtil.createAttributedURIType(TestServiceMetadata.ACTION_OPERATION_RESPONSE_1));

        TestOperationRequest req = soapUtil.getBody(rrObj.getRequest(), TestOperationRequest.class).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("SOAP body is malformed."),
                        rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null)));

        if (transportInfoCallback != null && rrObj.getCommunicationContext().isPresent()) {
            transportInfoCallback.onRequest(rrObj.getCommunicationContext().get().getTransportInfo());
        }

        TestOperationResponse res = objectFactory.createTestOperationResponse();
        res.setResult1(new StringBuilder(req.getParam1()).reverse().toString());
        res.setResult2(req.getParam2() * 2);

        soapUtil.setBody(res, rrObj.getResponse());
    }

    @MessageInterceptor(TestServiceMetadata.ACTION_OPERATION_REQUEST_2)
    void onTestOperation2(RequestResponseObject rrObj) throws SoapFaultException {
        rrObj.getResponse().getWsAddressingHeader()
                .setAction(wsaUtil.createAttributedURIType(TestServiceMetadata.ACTION_OPERATION_RESPONSE_2));

        TestOperationRequest req = soapUtil.getBody(rrObj.getRequest(), TestOperationRequest.class).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("SOAP body is malformed."),
                        rrObj.getRequest().getWsAddressingHeader().getMessageId().orElse(null)));

        if (transportInfoCallback != null && rrObj.getCommunicationContext().isPresent()) {
            transportInfoCallback.onRequest(rrObj.getCommunicationContext().get().getTransportInfo());
        }

        TestOperationResponse res = objectFactory.createTestOperationResponse();
        res.setResult1(req.getParam1() + req.getParam1());
        res.setResult2(req.getParam2() * 4);

        soapUtil.setBody(res, rrObj.getResponse());
    }

    public interface TransportInfoCallback {
        void onRequest(TransportInfo transportInfo);
    }
}
