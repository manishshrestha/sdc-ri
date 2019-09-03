package it.org.ieee11073.sdc.dpws.soap;


import it.org.ieee11073.sdc.dpws.TestServiceMetadata;
import org.ieee11073.sdc.dpws.DpwsFramework;
import org.ieee11073.sdc.dpws.DpwsUtil;
import org.ieee11073.sdc.dpws.device.DeviceSettings;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.soap.SoapConfig;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

public class BasicPopulatedDevice extends DevicePeer {
    public static final String SERVICE_ID_1 = "TestServiceId1";
    public static final String SERVICE_ID_2 = "TestServiceId2";

    public static final URI SCOPE_1 = URI.create("http://integration-test-scope1");
    public static final URI SCOPE_2 = URI.create("http://integration-test-scope2");

    public static final QName QNAME_1 = new QName("http://type-ns", "integration-test-type1");
    public static final QName QNAME_2 = new QName("http://type-ns", "integration-test-type2");

    private DpwsTestService1 service1;
    private DpwsTestService2 service2;

    public BasicPopulatedDevice() {
        this(null);
    }


    public BasicPopulatedDevice(@Nullable DeviceSettings deviceSettings) {
        setup(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        }, deviceSettings);
    }

    public BasicPopulatedDevice(@Nullable DeviceSettings deviceSettings, DefaultDpwsConfigModule configModule) {
        configModule.bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                TestServiceMetadata.JAXB_CONTEXT_PATH);
        setup(configModule, deviceSettings);
    }

    public DpwsTestService1 getService1() {
        return service1;
    }
    public DpwsTestService2 getService2() {
        return service2;
    }

    @Override
    protected void startUp() {
        getDevice().getDiscoveryAccess().setScopes(Arrays.asList(SCOPE_1, SCOPE_2));
        getDevice().getDiscoveryAccess().setTypes(Arrays.asList(QNAME_1, QNAME_2));

        DpwsUtil dpwsUtil = getInjector().getInstance(DpwsUtil.class);

        getDevice().getHostingServiceAccess().setThisDevice(dpwsUtil.createThisDevice(
                dpwsUtil.createLocalizedStrings("en", "BasicPopulatedDevice peer").get(), null, null));

        HostedServiceFactory hostedServiceFactory = getInjector().getInstance(HostedServiceFactory.class);
        service1 = getInjector().getInstance(DpwsTestService1.class);
        service2 = getInjector().getInstance(DpwsTestService2.class);

        final ClassLoader classLoader = getClass().getClassLoader();
        InputStream wsdlResource1 = classLoader.getResourceAsStream("it/org/ieee11073/sdc/dpws/TestService1.wsdl");
        InputStream wsdlResource2 = classLoader.getResourceAsStream("it/org/ieee11073/sdc/dpws/TestService2.wsdl");
        getDevice().getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                DevicePeerMetadata.SERVICE_ID_1,
                Arrays.asList(
                        new QName(DevicePeerMetadata.NAMESPACE_SRV, DevicePeerMetadata.PORT_TYPE_NAME_1),
                        new QName(DevicePeerMetadata.NAMESPACE_SRV, DevicePeerMetadata.PORT_TYPE_NAME_2)),
                service1,
                wsdlResource1));

        getDevice().getHostingServiceAccess().addHostedService(hostedServiceFactory.createHostedService(
                DevicePeerMetadata.SERVICE_ID_2,
                Collections.singletonList(new QName(DevicePeerMetadata.NAMESPACE_SRV, DevicePeerMetadata.PORT_TYPE_NAME_3)),
                service2,
                wsdlResource2));

        getInjector().getInstance(DpwsFramework.class).startAsync().awaitRunning();
        getDevice().startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() {
        getDevice().stopAsync().awaitTerminated();
        getInjector().getInstance(DpwsFramework.class).stopAsync().awaitTerminated();
    }
}
