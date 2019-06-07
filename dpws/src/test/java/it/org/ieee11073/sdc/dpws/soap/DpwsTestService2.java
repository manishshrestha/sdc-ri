package it.org.ieee11073.sdc.dpws.soap;

import com.google.inject.Inject;
import dpws_test_service.messages._2017._05._10.ObjectFactory;
import dpws_test_service.messages._2017._05._10.TestNotification;
import dpws_test_service.messages._2017._05._10.TestOperationRequest;
import dpws_test_service.messages._2017._05._10.TestOperationResponse;
import it.org.ieee11073.sdc.dpws.TestServiceMetadata;
import org.ieee11073.sdc.dpws.device.WebService;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.factory.SoapFaultFactory;
import org.ieee11073.sdc.dpws.soap.interception.MessageInterceptor;
import org.ieee11073.sdc.dpws.soap.interception.RequestResponseObject;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wseventing.EventSource;

import java.util.List;


public class DpwsTestService2 extends WebService {

    private final WsAddressingUtil wsaUtil;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory objectFactory;

    @Inject
    DpwsTestService2(WsAddressingUtil wsaUtil,
                     SoapUtil soapUtil,
                     SoapFaultFactory soapFaultFactory,
                     ObjectFactory objectFactory) {
        this.wsaUtil = wsaUtil;

        this.soapUtil = soapUtil;
        this.soapFaultFactory = soapFaultFactory;
        this.objectFactory = objectFactory;
    }

    public void sendNotifications(List<TestNotification> notificationsToSend) {
        EventSource evtSrc = getEventSource();
        notificationsToSend.stream().forEach(testNotification ->
                evtSrc.sendNotification(
                        TestServiceMetadata.ACTION_NOTIFICATION_3,
                        testNotification));
    }

    @MessageInterceptor(TestServiceMetadata.ACTION_OPERATION_REQUEST_3)
    void onTestOperation(RequestResponseObject rrObj) throws SoapFaultException {
        rrObj.getResponse().getWsAddressingHeader()
                .setAction(wsaUtil.createAttributedURIType(TestServiceMetadata.ACTION_OPERATION_RESPONSE_3));

        TestOperationRequest req = soapUtil.getBody(rrObj.getRequest(), TestOperationRequest.class).orElseThrow(() ->
                new SoapFaultException(soapFaultFactory.createSenderFault("SOAP body is malformed.")));

        TestOperationResponse res = objectFactory.createTestOperationResponse();
        res.setResult1(new StringBuilder(req.getParam1()).append(req.getParam1()).reverse().toString());
        res.setResult2(req.getParam2() * 8);

        soapUtil.setBody(res, rrObj.getResponse());
    }
}
