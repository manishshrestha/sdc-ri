package org.somda.sdc.mdpws.provider.safety; 
 
import com.google.inject.Inject; 
import com.google.inject.Provider; 
import com.google.inject.name.Named; 
import org.somda.sdc.common.util.PrefixNamespaceMappingParser; 
import org.somda.sdc.dpws.soap.SoapConfig; 
import org.somda.sdc.mdpws.model.SafetyRequirementsType; 
 
import javax.xml.bind.JAXBElement; 
import java.util.HashMap; 
import java.util.Map; 
 
public class SafetyXPath { 
    private final Map<String, String> namespaceToPrefix; 
    private final Provider<SafetyRequirementsBuilder> safetyRequirementsBuilderProvider; 
 
    @Inject 
    SafetyXPath(@Named(SoapConfig.NAMESPACE_MAPPINGS) String namespaceMappings, 
                PrefixNamespaceMappingParser prefixNamespaceMappingParser, 
                Provider<SafetyRequirementsBuilder> safetyRequirementsBuilderProvider) { 
        this.safetyRequirementsBuilderProvider = safetyRequirementsBuilderProvider; 
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
        return safetyRequirementsBuilderProvider.get(); 
    } 
 
//    public JAXBElement<SafetyRequirementsType> createSafetyRequirementsElement(SafetyRequirementsType safetyRequirementsType) {
//        var element = new JAXBElement<SafetyRequirementsType>();
//        element.setValue(safetyRequirementsType);
//        return element;
//    }
} 
