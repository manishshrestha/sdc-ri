package it.org.ieee11073.sdc.dpws.soap;

import dpws_test_service.messages._2017._05._10.ObjectFactory;
import dpws_test_service.messages._2017._05._10.TestNotification;
import it.org.ieee11073.sdc.dpws.IntegrationTestPeer;
import org.ieee11073.sdc.dpws.DpwsFramework;
import org.ieee11073.sdc.dpws.DpwsUtil;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.dpws.device.DeviceConfiguration;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class DevicePeer extends IntegrationTestPeer {
    private Device device;

    public DevicePeer() {
        this.device = getInjector().getInstance(Device.class);
    }

    public Device getDevice() {
        return device;
    }

    @Override
    protected void startUp() {
        this.device.setConfiguration(new DeviceConfiguration() {
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

        this.device.getDiscoveryAccess().setScopes(Arrays.asList(
                URI.create("http://integration-test-scope1"),
                URI.create("http://integration-test-scope2")
        ));
        this.device.getDiscoveryAccess().setTypes(Arrays.asList(
                new QName("http://type-ns", "integration-test-type1"),
                new QName("http://type-ns", "integration-test-type2")
        ));

        DpwsUtil dpwsUtil = getInjector().getInstance(DpwsUtil.class);

        device.getHostingServiceAccess().setThisDevice(dpwsUtil.createThisDevice(
                dpwsUtil.createLocalizedStrings("en", "TestDevice peer").get(), null, null));

        HostedServiceFactory hostedServiceFactory = getInjector().getInstance(HostedServiceFactory.class);
        DpwsTestService1 service1 = getInjector().getInstance(DpwsTestService1.class);
        DpwsTestService2 service2 = getInjector().getInstance(DpwsTestService2.class);

        ObjectFactory factory = new ObjectFactory();

        Thread t1 = new Thread(() -> {
            int count = 0;
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(1000);
                    TestNotification notification = factory.createTestNotification();
                    notification.setParam1("Event " + count);
                    notification.setParam2(count++);
                    service1.sendNotifications(Arrays.asList(notification));

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        t1.start();
//        Thread t2 = new Thread(() -> {
//            int count = 0;
//            while (!Thread.interrupted()) {
//                try {
//                    Thread.sleep(1000);
//                    TestNotification notification = factory.createTestNotification();
//                    notification.setParam1("Event " + count);
//                    notification.setParam2(count++);
//                    service2.sendNotifications(Arrays.asList(notification));
//
//                } catch (InterruptedException e) {
//                    break;
//                }
//            }
//        });
//        t2.start();

        InputStream wsdlResource1 = getClass().getClassLoader().getResourceAsStream("it/org/ieee11073/sdc/dpws/TestService1.wsdl");
        InputStream wsdlResource2 = getClass().getClassLoader().getResourceAsStream("it/org/ieee11073/sdc/dpws/TestService2.wsdl");
        device.getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                DevicePeerMetadata.SERVICE_ID_1,
                Arrays.asList(
                        new QName(DevicePeerMetadata.NAMESPACE_SRV, DevicePeerMetadata.PORT_TYPE_NAME_1),
                        new QName(DevicePeerMetadata.NAMESPACE_SRV, DevicePeerMetadata.PORT_TYPE_NAME_2)),
                service1,
                wsdlResource1));

        device.getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                DevicePeerMetadata.SERVICE_ID_2,
                Arrays.asList(new QName(DevicePeerMetadata.NAMESPACE_SRV, DevicePeerMetadata.PORT_TYPE_NAME_3)),
                service2,
                wsdlResource2));

        DpwsFramework dpwsFramework = getInjector().getInstance(DpwsFramework.class);
        dpwsFramework.startAsync().awaitRunning();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> dpwsFramework.stopAsync().awaitTerminated()));

        device.startAsync().awaitRunning();
    }

    @Override
    protected void run() throws Exception {
        while (!Thread.interrupted()) {
            Thread.sleep(1000);
        }
    }

    @Override
    protected void shutDown() {
        device.stopAsync().awaitTerminated();
    }
}
