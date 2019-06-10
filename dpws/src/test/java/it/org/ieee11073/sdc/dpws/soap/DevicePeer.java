package it.org.ieee11073.sdc.dpws.soap;

import it.org.ieee11073.sdc.dpws.IntegrationTestPeer;
import it.org.ieee11073.sdc.dpws.TestServiceMetadata;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.soap.SoapConfig;
import org.ieee11073.sdc.dpws.soap.SoapUtil;

import java.net.URI;
import java.util.UUID;

public abstract class DevicePeer extends IntegrationTestPeer {
    private URI eprAddress;
    private Device device;

    public DevicePeer() {
        this(new DefaultDpwsConfigModule());
    }

    public DevicePeer(URI eprAddress, DefaultDpwsConfigModule configModule) {
        super(configModule);
        this.eprAddress = eprAddress;
        this.device = getInjector().getInstance(Device.class);
    }

    public DevicePeer(URI eprAddress) {
        this(eprAddress, new DefaultDpwsConfigModule() {
            @Override
            protected void customConfigure() {
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        });
    }

    public DevicePeer(DefaultDpwsConfigModule configModule) {
        super(configModule);
        this.eprAddress = getInjector().getInstance(SoapUtil.class).createUriFromUuid(UUID.randomUUID());
        this.device = getInjector().getInstance(Device.class);

    }

    public URI getEprAddress() {
        return eprAddress;
    }

    public Device getDevice() {
        return device;
    }
}
