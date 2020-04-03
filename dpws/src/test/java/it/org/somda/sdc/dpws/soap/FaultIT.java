package it.org.somda.sdc.dpws.soap;

import dpws_test_service.messages._2017._05._10.ObjectFactory;
import it.org.somda.sdc.dpws.IntegrationTestUtil;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedQNameType;
import org.somda.sdc.dpws.soap.wsaddressing.model.ProblemActionType;
import test.org.somda.common.LoggingTestWatcher;
import test.org.somda.common.TestLogging;

import javax.xml.bind.JAXBElement;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
public class FaultIT {
    private static final Duration MAX_WAIT_TIME = IntegrationTestUtil.MAX_WAIT_TIME;

    private final IntegrationTestUtil IT = new IntegrationTestUtil();

    private BasicPopulatedDevice devicePeer;
    private ClientPeer clientPeer;

    private HostingServiceProxy hostingServiceProxy;
    private ObjectFactory factory;
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);

    FaultIT() {
        IntegrationTestUtil.preferIpV4Usage();
    }

    @BeforeEach
    void setUp() throws Exception {
        TestLogging.configure();

        factory = new ObjectFactory();

        devicePeer = new BasicPopulatedDevice(new MockedUdpBindingModule());
        clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        }, new MockedUdpBindingModule());
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
        this.devicePeer = null;
        this.clientPeer = null;
    }

    @Test
    void invalidActions() throws Exception {
        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        assertNotNull(srv1);

        {
            // Test missing action
            final SoapMessage reqMsg = soapUtil.createMessage();
            try {
                srv1.sendRequestResponse(reqMsg);
                fail("Expected a SoapFaultException to be thrown in case of a missing action");
            } catch (SoapFaultException e) {
                assertEquals(SoapConstants.SENDER, e.getFault().getCode().getValue());
                assertEquals(WsAddressingConstants.MESSAGE_ADDRESSING_HEADER_REQUIRED, e.getFault().getCode().getSubcode().getValue());
                assertEquals(1, e.getFault().getDetail().getAny().size());
                var detail = (JAXBElement<AttributedQNameType>)e.getFault().getDetail().getAny().get(0);
                assertEquals(WsAddressingConstants.QNAME_ACTION, detail.getValue().getValue());
            }
        }

        {
            // Test unknown action
            var expectedUnknownAction = "http://unknown-action";
            final SoapMessage reqMsg = soapUtil.createMessage(expectedUnknownAction);
            try {
                srv1.sendRequestResponse(reqMsg);
                fail("Expected a SoapFaultException to be thrown in case of an unknown action");
            } catch (SoapFaultException e) {
                assertEquals(SoapConstants.SENDER, e.getFault().getCode().getValue());
                assertEquals(WsAddressingConstants.ACTION_NOT_SUPPORTED, e.getFault().getCode().getSubcode().getValue());
                assertEquals(1, e.getFault().getDetail().getAny().size());
                var detail = (JAXBElement<ProblemActionType>)e.getFault().getDetail().getAny().get(0);
                assertEquals(expectedUnknownAction, detail.getValue().getAction().getValue());
            }
        }
    }
}
