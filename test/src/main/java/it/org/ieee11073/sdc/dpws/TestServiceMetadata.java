package it.org.ieee11073.sdc.dpws;

/**
 * Created by gregorcd on 10.05.2017.
 */
public class TestServiceMetadata {
    public static final String NAMESPACE_MSG = "http://dpws-test-service/messages/2017/05/10";
    public static final String NAMESPACE_SRV = "http://dpws-test-service/2017/05/10";

    public static final String PORT_TYPE_NAME_1 = "TestPortType1";
    public static final String PORT_TYPE_NAME_2 = "TestPortType2";
    public static final String PORT_TYPE_NAME_3 = "TestPortType3";

    public static final String PORT_TYPE_1 = NAMESPACE_SRV + "/" + PORT_TYPE_NAME_1;
    public static final String PORT_TYPE_2 = NAMESPACE_SRV + "/" + PORT_TYPE_NAME_2;
    public static final String PORT_TYPE_3 = NAMESPACE_SRV + "/" + PORT_TYPE_NAME_3;

    public static final String ACTION_OPERATION_REQUEST_1 = PORT_TYPE_1 + "/TestOperationRequest";
    public static final String ACTION_OPERATION_RESPONSE_1 = PORT_TYPE_1 + "/TestOperationResponse";
    public static final String ACTION_NOTIFICATION_1 = PORT_TYPE_1 + "/TestNotification";

    public static final String ACTION_OPERATION_REQUEST_2 = PORT_TYPE_2 + "/TestOperationRequest";
    public static final String ACTION_OPERATION_RESPONSE_2 = PORT_TYPE_2 + "/TestOperationResponse";
    public static final String ACTION_NOTIFICATION_2 = PORT_TYPE_2 + "/TestNotification";

    public static final String ACTION_OPERATION_REQUEST_3 = PORT_TYPE_3 + "/TestOperationRequest";
    public static final String ACTION_OPERATION_RESPONSE_3 = PORT_TYPE_3 + "/TestOperationResponse";
    public static final String ACTION_NOTIFICATION_3 = PORT_TYPE_3 + "/TestNotification";


    public static final String JAXB_CONTEXT_PATH = "dpws_test_service.messages._2017._05._10";
}
