package it.org.ieee11073.sdc.dpws.soap;


import org.ieee11073.sdc.dpws.DpwsFramework;
import org.ieee11073.sdc.dpws.DpwsUtil;
import org.ieee11073.sdc.dpws.device.DeviceConfiguration;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class BasicPopulatedDevice extends DevicePeer {
    public BasicPopulatedDevice() {
    }

    public BasicPopulatedDevice(URI eprAddress, DefaultDpwsConfigModule configModule) {
        super(eprAddress, configModule);
    }

    public BasicPopulatedDevice(URI eprAddress) {
        super(eprAddress);
    }

    public BasicPopulatedDevice(DefaultDpwsConfigModule configModule) {
        super(configModule);
    }

    @Override
    protected void startUp() {
        getDevice().setConfiguration(new DeviceConfiguration() {
            @Override
            public EndpointReferenceType getEndpointReference() {
                return getInjector().getInstance(WsAddressingUtil.class)
                        .createEprWithAddress(getEprAddress());
            }

            @Override
            public List<URI> getHostingServiceBindings() {
                return Arrays.asList(URI.create("http://localhost:8080"));
            }
        });

        getDevice().getDiscoveryAccess().setScopes(Arrays.asList(
                URI.create("http://integration-test-scope1"),
                URI.create("http://integration-test-scope2")
        ));
        getDevice().getDiscoveryAccess().setTypes(Arrays.asList(
                new QName("http://type-ns", "integration-test-type1"),
                new QName("http://type-ns", "integration-test-type2")
        ));

        DpwsUtil dpwsUtil = getInjector().getInstance(DpwsUtil.class);

        getDevice().getHostingServiceAccess().setThisDevice(dpwsUtil.createThisDevice(
                dpwsUtil.createLocalizedStrings("en", "TestDevice peer").get(), null, null));

        HostedServiceFactory hostedServiceFactory = getInjector().getInstance(HostedServiceFactory.class);
        DpwsTestService1 service1 = getInjector().getInstance(DpwsTestService1.class);
        DpwsTestService2 service2 = getInjector().getInstance(DpwsTestService2.class);

//        ObjectFactory factory = new ObjectFactory();
//
//        Thread t1 = new Thread(() -> {
//            int count = 0;
//            while (!Thread.interrupted()) {
//                try {
//                    Thread.sleep(1000);
//                    TestNotification notification = factory.createTestNotification();
//                    notification.setParam1("Event " + count);
//                    notification.setParam2(count++);
//                    service1.sendNotifications(Arrays.asList(notification));
//
//                } catch (InterruptedException e) {
//                    break;
//                }
//            }
//        });
//        t1.start();
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
        getDevice().getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                DevicePeerMetadata.SERVICE_ID_1,
                Arrays.asList(
                        new QName(DevicePeerMetadata.NAMESPACE_SRV, DevicePeerMetadata.PORT_TYPE_NAME_1),
                        new QName(DevicePeerMetadata.NAMESPACE_SRV, DevicePeerMetadata.PORT_TYPE_NAME_2)),
                service1,
                wsdlResource1));

        getDevice().getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                DevicePeerMetadata.SERVICE_ID_2,
                Arrays.asList(new QName(DevicePeerMetadata.NAMESPACE_SRV, DevicePeerMetadata.PORT_TYPE_NAME_3)),
                service2,
                wsdlResource2));

        DpwsFramework dpwsFramework = getInjector().getInstance(DpwsFramework.class);
        dpwsFramework.startAsync().awaitRunning();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> dpwsFramework.stopAsync().awaitTerminated()));

        getDevice().startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
        getDevice().stopAsync().awaitTerminated();
    }
}
