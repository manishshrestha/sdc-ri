package org.ieee11073.sdc.dpws.client.helper;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.client.ClientConfig;
import org.ieee11073.sdc.dpws.client.DeviceProxy;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.HelloMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.ProbeMatchesMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ProbeMatchType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provide different functions to resolve a {@link DeviceProxy} object from hello or probe messages.
 */
public class DeviceProxyResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceProxyResolver.class);
    private final WsDiscoveryClient wsDiscoveryClient;
    private final Duration maxWaitForResolveMatches;
    private final WsAddressingUtil wsaUtil;
    private final Boolean autoResolve;

    /**
     * Assisted constructor.
     *
     * @param wsDiscoveryClient WS-Discovery client to send explicit resolve request if xAddrs are missing.
     */
    @AssistedInject
    DeviceProxyResolver(@Assisted WsDiscoveryClient wsDiscoveryClient,
                        @Named(ClientConfig.MAX_WAIT_FOR_RESOLVE_MATCHES) Duration maxWaitForResolveMatches,
                        @Named(ClientConfig.AUTO_RESOLVE) Boolean autoResolve,
                        WsAddressingUtil wsaUtil) {
        this.wsDiscoveryClient = wsDiscoveryClient;
        this.maxWaitForResolveMatches = maxWaitForResolveMatches;
        this.autoResolve = autoResolve;
        this.wsaUtil = wsaUtil;
    }

    /**
     * Take a hello message to resolve {@link DeviceProxy} object.
     *
     * @param helloMessage The hello message retrieved by a {@link WsDiscoveryClient} implementation.
     * @return The device proxy instance or {@link Optional#empty()} if resolving failed.
     */
    public Optional<DeviceProxy> resolve(HelloMessage helloMessage) {
        HelloType hm = helloMessage.getPayload();
        return resolve(hm.getEndpointReference(), hm.getTypes(), hm.getScopes().getValue(), hm.getXAddrs(),
                hm.getMetadataVersion());
    }

    /**
     * Take a probe matches message to resolve {@link DeviceProxy} object.
     *
     * @param probeMatchesMessage The probe matches message retrieved by a {@link WsDiscoveryClient} implementation.
     * @return The device proxy instance or {@link Optional#empty()} if resolving failed.
     */
    public Optional<DeviceProxy> resolve(ProbeMatchesMessage probeMatchesMessage) {
        ProbeMatchesType probe = probeMatchesMessage.getPayload();
        if (probe.getProbeMatch().size() != 1) {
            return Optional.empty();
        }

        ProbeMatchType pm = probe.getProbeMatch().get(0);
        return resolve(pm.getEndpointReference(), pm.getTypes(), pm.getScopes().getValue(), pm.getXAddrs(),
                pm.getMetadataVersion());
    }

    private Optional<DeviceProxy> resolve(EndpointReferenceType epr,
                                          List<QName> types,
                                          List<String> scopes,
                                          List<String> xAddrs,
                                          long metadataVersion) {
        if (!wsaUtil.getAddressUriAsString(epr).isPresent()) {
            LOG.info("Empty device endpoint reference found. Skip resolve");
            return Optional.empty();
        }

        if (xAddrs.isEmpty() && autoResolve) {
            return sendResolve(epr).map(rms ->
                    Optional.ofNullable(rms.getResolveMatch()).map(rm ->
                            wsaUtil.getAddressUri(rm.getEndpointReference()).map(uri ->
                                    Optional.of(new DeviceProxy(
                                            uri,
                                            rm.getTypes(),
                                            rm.getScopes().getValue(),
                                            rm.getXAddrs(),
                                            rm.getMetadataVersion())))
                                    .orElse(Optional.empty()))
                            .orElse(Optional.empty()))
                    .orElse(Optional.empty());
        }

        return wsaUtil.getAddressUri(epr).map(uri -> new DeviceProxy(uri, types, scopes, xAddrs, metadataVersion));
    }

    private Optional<ResolveMatchesType> sendResolve(EndpointReferenceType epr) {
        try {
            ListenableFuture<ResolveMatchesType> resolveMatches = wsDiscoveryClient.sendResolve(epr);
            return Optional.ofNullable(resolveMatches.get(maxWaitForResolveMatches.toMillis(), TimeUnit.MILLISECONDS));
        } catch (MarshallingException e) {
            LOG.info("Resolve of '{}' failed due to marshalling exception", e.getCause());
        } catch (TransportException e) {
            LOG.info("Transmission of resolve request to '{}' failed", e.getCause());
        } catch (InterruptedException e) {
            LOG.info("Resolve of '{}' failed due to thread interruption", e.getCause());
        } catch (ExecutionException e) {
            LOG.info("Resolve of '{}' failed", e.getCause());
        } catch (TimeoutException e) {
            LOG.debug("Did not get resolve answer from '{}' within {} ms", wsaUtil.getAddressUriAsString(epr),
                    maxWaitForResolveMatches.toMillis());
        }

        return Optional.empty();
    }
}
