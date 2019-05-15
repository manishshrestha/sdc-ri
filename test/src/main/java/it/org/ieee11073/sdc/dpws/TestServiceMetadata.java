package it.org.ieee11073.sdc.dpws;

/**
 * Created by gregorcd on 10.05.2017.
 */
public class TestServiceMetadata {
    public static final String NAMESPACE_MSG = "http://dpws-test-service/messages/2017/05/10";
    public static final String NAMESPACE_SRV = "http://dpws-test-service/2017/05/10";

    public static final String ACTION_OPERATION_REQUEST = NAMESPACE_MSG + "/TestOperationRequest";
    public static final String ACTION_OPERATION_RESPONSE = NAMESPACE_MSG + "/TestOperationResponse";
    public static final String ACTION_NOTIFICATION = NAMESPACE_MSG + "/TestNotification";
    public static final String OPERATION_NOTIFICATION = NAMESPACE_SRV + "/TestPortType/TestNotification";
    public static final String PORT_TYPE_NAME = "TestPortType";

    public static final String JAXB_CONTEXT_PATH = "dpws_test_service.messages._2017._05._10";
}
