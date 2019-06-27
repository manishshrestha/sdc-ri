package it.org.ieee11073.sdc.dpws.soap;

import it.org.ieee11073.sdc.dpws.IntegrationTestPeer;
import it.org.ieee11073.sdc.dpws.TestServiceMetadata;
import org.ieee11073.sdc.dpws.device.DefaultDeviceSettings;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.dpws.device.DeviceSettings;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.http.HttpUriBuilder;
import org.ieee11073.sdc.dpws.soap.SoapConfig;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class DevicePeer extends IntegrationTestPeer {
    private final URI eprAddress;
    private final Device device;

    public DevicePeer(@Nullable DeviceSettings deviceSettings, DefaultDpwsConfigModule configModule) {
        super(configModule);
        this.device = getInjector().getInstance(Device.class);
        if (deviceSettings == null) {
            this.eprAddress = getInjector().getInstance(SoapUtil.class).createUriFromUuid(UUID.randomUUID());
            final WsAddressingUtil wsaUtil = getInjector().getInstance(WsAddressingUtil.class);
            final HttpUriBuilder uriBuilder = getInjector().getInstance(HttpUriBuilder.class);
            final EndpointReferenceType epr = wsaUtil.createEprWithAddress(this.eprAddress);
            final List<URI> hostingServiceBinding = Collections.singletonList(uriBuilder.buildUri("localhost",
                    uriBuilder.buildRandomPort()));
            this.device.setConfiguration(new DeviceSettings() {
                @Override
                public EndpointReferenceType getEndpointReference() {
                    return epr;
                }

                @Override
                public List<URI> getHostingServiceBindings() {
                    return hostingServiceBinding;
                }
            });
        }
        else {
            this.eprAddress = URI.create(deviceSettings.getEndpointReference().getAddress().getValue());
            this.device.setConfiguration(deviceSettings);
        }
    }

    public DevicePeer(DefaultDpwsConfigModule configModule) {
        this(null, configModule);
    }

    public URI getEprAddress() {
        return eprAddress;
    }

    public Device getDevice() {
        return device;
    }

}
