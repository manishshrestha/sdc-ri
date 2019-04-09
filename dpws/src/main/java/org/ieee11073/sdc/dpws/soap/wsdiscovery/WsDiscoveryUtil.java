package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import com.google.common.primitives.UnsignedInteger;
import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.AppSequenceType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Utility class for WS-Eventing plugin.
 */
public class WsDiscoveryUtil {

    private final ObjectFactory wsdFactory;

    private final AtomicInteger messageIdCounter;

    @Inject
    WsDiscoveryUtil(ObjectFactory wsdFactory) {
        this.wsdFactory = wsdFactory;
        messageIdCounter = new AtomicInteger(0);
    }

    /**
     * Check if QName superset includes subset.
     *
     * @return True if subset is in superset or subset is equal to superset, otherwise false.
     */
    public boolean isTypesMatching(List<QName> superset, List<QName> subset) {
        return isMatching(superset, subset, (o1, o2) -> o1.equals(o2) ? 0 : 1);
    }

    /**
     * Check if scope superset includes subset.
     *
     * @param matchBy The rule how to compare the URI strings. {@link MatchBy#RFC3986} and {@link MatchBy#STRCMP0} are
     *                supported.
     * @return True if subset is in superset or subset is equal to superset given a matching algorithm in matchBy,
     *         otherwise false. If the matching algorithm is not supported, this method always returns false.
     */
    public boolean isScopesMatching(List<String> superset, List<String> subset, MatchBy matchBy) {
        switch (matchBy) {
            case RFC3986:
                // \todo this is probably wrong/incomplete RFC3986 matching - room for improvement here!
                return isMatching(superset, subset, (o1, o2) ->
                        URI.create((String) o1).equals(URI.create((String) o2)) ? 0 : 1
                );
            case STRCMP0:
                isMatching(superset, subset, (o1, o2) -> o1.equals(o2) ? 0 : 1);
            default:
                return false;
        }
    }

    /**
     * Create an app sequence based on a given instance id.
     *
     * @return An app sequence that includes the given instance id and an unsigned integer that is incremented with
     * every method request of a specific instance of {@link WsDiscoveryUtil}.
     */
    public AppSequenceType createAppSequence(UnsignedInteger instanceId) {
        UnsignedInteger messageId = UnsignedInteger.valueOf(messageIdCounter.addAndGet(1));
        AppSequenceType appSequence = wsdFactory.createAppSequenceType();
        appSequence.setInstanceId(instanceId.longValue());
        appSequence.setMessageNumber(messageId.longValue());
        return appSequence;
    }

    private boolean isMatching(List<?> superset, List<?> subset, Comparator<Object> comp) {
        return superset.size() >= subset.size() && superset.parallelStream()
                .filter(qName1 -> subset.parallelStream()
                        .filter(qName2 -> comp.compare(qName1, qName2) == 0)
                        .findAny().isPresent())
                .collect(Collectors.toList())
                .size() == subset.size();
    }
}
