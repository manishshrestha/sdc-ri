package org.ieee11073.sdc.dpws.soap.wsdiscovery.factory;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapConstants;
import org.ieee11073.sdc.dpws.soap.factory.SoapFaultFactory;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.MatchBy;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Factory to create WS-Discovery fault messages.
 */
public class WsDiscoveryFaultFactory {
    private final SoapFaultFactory soapFaultFactory;
    private final ObjectFactory wsdFactory;

    @Inject
    WsDiscoveryFaultFactory(SoapFaultFactory soapFaultFactory, ObjectFactory wsdFactory) {
        this.soapFaultFactory = soapFaultFactory;
        this.wsdFactory = wsdFactory;
    }

    /**
     * Creates a MatchingRuleNotSupported fault.
     *
     * @return the instance.
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231831">Probe</a>
     */
    public SoapMessage createMatchingRuleNotSupported() {
        final List<String> supportedMatchBys = new ArrayList<>();
        Arrays.stream(MatchBy.values()).forEach(matchBy -> supportedMatchBys.add(matchBy.getUri()));

        return soapFaultFactory.createFault(
                WsDiscoveryConstants.FAULT_ACTION,
                SoapConstants.SENDER,
                WsDiscoveryConstants.MATCHING_RULE_NOT_SUPPORTED,
                "The matching rule specified is not supported.",
                wsdFactory.createSupportedMatchingRules(supportedMatchBys));
    }
}
