package org.somda.sdc.dpws.device;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.DpwsUtil;
import org.somda.sdc.dpws.device.factory.DeviceFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.dpws.model.LocalizedStringType;
import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConfig;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import test.org.somda.common.TestLogging;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class DeviceImplTest implements Runnable {
    public static void main(String[] args) {
        TestLogging.configure();
        new DeviceImplTest().run();
    }

    @Override
    public void run() {
        Injector inj = Guice.createInjector(new DefaultDpwsModule(), new DefaultCommonModule(), new DpwsConfig());

        WsAddressingUtil wsaUtil = inj.getInstance(WsAddressingUtil.class);
        DeviceSettings devConf = new DeviceSettings() {
            @Override
            public synchronized EndpointReferenceType getEndpointReference() {
                return wsaUtil.createEprWithAddress("urn:uuid:00000000-0000-0000-0000-000000000000");
            }

            @Override
            public NetworkInterface getNetworkInterface() {
                try {
                    return NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        DpwsFramework dpwsFramework = inj.getInstance(DpwsFramework.class);
        dpwsFramework.startAsync().awaitRunning();

        Device device = inj.getInstance(DeviceFactory.class).createDevice(devConf);

        device.getDiscoveryAccess().setTypes(Collections.singletonList(new QName("http://test-type", "Device")));
        device.getDiscoveryAccess().setScopes(Collections.singletonList("http://test-scope/Scope"));

        DpwsUtil dpwsUtil = inj.getInstance(DpwsUtil.class);

        device.getHostingServiceAccess().setThisDevice(
            ThisDeviceType.builder()
                .addFriendlyName(
                    dpwsUtil.setLang(
                        LocalizedStringType.builder()
                            .withValue("Draeger IACS Monitoring Unit")
                            .build(),
                        "en"
                    )
                )
                .withFirmwareVersion("v1.2.3")
                .withSerialNumber("1234-5678-9101-1121")
                .build()
        );

        device.getHostingServiceAccess().setThisModel(
            ThisModelType.builder()
                    .addManufacturer(
                        dpwsUtil.setLang(LocalizedStringType.builder()
                            .withValue("Draegerwerk AG")
                            .build(),
                            "en"
                        )
                    )
                .addManufacturer(
                    dpwsUtil.setLang(LocalizedStringType.builder()
                            .withValue("Drägerwerk AG")
                            .build(),
                        "de"
                    )
                )
                .withModelName(LocalizedStringType.builder().withValue("IACS").build())
                .withPresentationUrl("http://www.google.com")
                .build()
        );


        device.startAsync().awaitRunning();

        System.out.println("Device started");
        try {
            int value;
            while ((value = System.in.read()) != -1) {
                System.out.println((char) value);
                if (((char) value) == '\n') {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Stop device");
        device.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
        System.out.println("Device stopped");
    }

    private static class DpwsConfig extends AbstractModule {
        @Override
        protected void configure() {
            bind(Boolean.class)
                    .annotatedWith(Names.named(WsAddressingConfig.IGNORE_MESSAGE_IDS))
                    .toInstance(Boolean.FALSE);
        }
    }
}