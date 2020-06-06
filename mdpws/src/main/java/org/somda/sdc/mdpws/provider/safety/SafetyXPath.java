package org.somda.sdc.mdpws.provider.safety;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.somda.sdc.common.util.PrefixNamespaceMappingParser;
import org.somda.sdc.dpws.soap.SoapConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Injectable utility class to create {@linkplain XPathBuilder} with suitable namespace/prefix mappings.
 */
public class SafetyXPath {
    private final Map<String, String> namespaceToPrefix;

    @Inject
    SafetyXPath(@Named(SoapConfig.NAMESPACE_MAPPINGS) String namespaceMappings,
                PrefixNamespaceMappingParser prefixNamespaceMappingParser) {
        this.namespaceToPrefix = new HashMap<>();
        var parsedMapping = prefixNamespaceMappingParser.parse(namespaceMappings);

        for (PrefixNamespaceMappingParser.PrefixNamespacePair value : parsedMapping.values()) {
            namespaceToPrefix.put(value.getNamespace(), value.getPrefix());
        }
    }

    /**
     * Creates an {@linkplain XPathBuilder} with namespace/prefix mappings derived from the globally used mapping.
     * <p>
     * This ensures the XPath string to contain valid prefixes when be added to a JAXB object.
     *
     * @return an {@linkplain XPathBuilder} that is capable of creating XPath strings valid in marshalled JAXB objects.
     */
    public XPathBuilder createXPathBuilder() {
        return XPathBuilder.create(namespaceToPrefix);
    }
} 
