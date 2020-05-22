package org.somda.sdc.mdpws.provider.safety;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.somda.sdc.common.util.PrefixNamespaceMappingParser;
import org.somda.sdc.dpws.soap.SoapConfig;

import java.util.HashMap;
import java.util.Map;

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

    public XPathBuilder createXPathBuilder() {
        return XPathBuilder.create(namespaceToPrefix);
    }

    public SafetyRequirementsBuilder createSafetyRequirementsBuilder() {
        return SafetyRequirementsBuilder.create();
    }

//    public JAXBElement<SafetyRequirementsType> createSafetyRequirementsElement(SafetyRequirementsType safetyRequirementsType) {
//        var element = new JAXBElement<SafetyRequirementsType>();
//        element.setValue(safetyRequirementsType);
//        return element;
//    }
} 
