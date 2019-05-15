package it.org.ieee11073.sdc.dpws;


import dpws_test_service.messages._2017._05._10.ObjectFactory;
import dpws_test_service.messages._2017._05._10.TestNotification;
import org.ieee11073.sdc.dpws.DpwsFramework;
import org.ieee11073.sdc.dpws.DpwsTest;
import org.ieee11073.sdc.dpws.DpwsUtil;
import org.ieee11073.sdc.dpws.ThisDeviceBuilder;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.dpws.device.DeviceConfiguration;
import org.ieee11073.sdc.dpws.device.WebService;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CommunicationIT extends DpwsTest {
    private IntegrationTestPeer testPeer;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        testPeer = new IntegrationTestPeer();
        Device device = testPeer.getInjector().getInstance(Device.class);
        device.setConfiguration(new DeviceConfiguration() {
            @Override
            public EndpointReferenceType getEndpointReference() {
                return getInjector().getInstance(WsAddressingUtil.class)
                        .createEprWithAddress("urn:uuid:cd4cb146-a587-4450-b5e6-e563a0158794");
            }

            @Override
            public List<URI> getHostingServiceBindings() {
                return Arrays.asList(URI.create("http://localhost:8080"));
            }
        });

        device.getDiscoveryAccess().setScopes(Arrays.asList(
                URI.create("http://scope1"),
                URI.create("http://scope2")
        ));
        device.getDiscoveryAccess().setTypes(Arrays.asList(
                new QName("http://type-ns", "type1"),
                new QName("http://type-ns", "type2")
        ));

        DpwsUtil dpwsUtil = getInjector().getInstance(DpwsUtil.class);

        device.getHostingServiceAccess().setThisDevice(dpwsUtil.createThisDevice(
                dpwsUtil.createLocalizedStrings("en", "TestDevice peer").get(), null, null));

        HostedServiceFactory hostedServiceFactory = getInjector().getInstance(HostedServiceFactory.class);
        DpwsTestService service = getInjector().getInstance(DpwsTestService.class);

        ObjectFactory factory = new ObjectFactory();

        Thread t = new Thread(() -> {
            int count = 0;
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(1000);
                    TestNotification notification = factory.createTestNotification();
                    notification.setParam1("Event " + count);
                    notification.setParam2(count++);
                    service.sendNotifications(Arrays.asList(notification));

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        t.start();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream wsdlResource = classLoader.getResourceAsStream("it/org/ieee11073/sdc/dpws/TestService.wsdl");

        device.getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                "TestService1",
                Arrays.asList(new QName(TestServiceMetadata.NAMESPACE_SRV, TestServiceMetadata.PORT_TYPE_NAME)),
                service,
                wsdlResource));
        testPeer.addService(device);
    }

    @Test
    public void startDevice() throws Exception {
        DpwsFramework dpwsFramework = testPeer.getInjector().getInstance(DpwsFramework.class);
        dpwsFramework.startAsync().awaitRunning();
        //Runtime.getRuntime().addShutdownHook(new Thread(() -> dpwsFramework.stopAsync().awaitTerminated()));
        testPeer.startAsync().awaitRunning();
        Thread.sleep(1000000000);
        assertTrue(false);

    }
}
