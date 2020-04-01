package org.somda.sdc.mdpws.provider.safety; 
 
import javax.annotation.Nullable; 
import javax.xml.namespace.QName; 
import java.util.Collections; 
import java.util.Map; 
 
public class XPathStep { 
    private final String nonColonizedName; 
    private final QName qualifiedName; 
    private final XPathCondition condition; 
 
    public static XPathStep create(String name) { 
        return new XPathStep(name, null, null); 
    } 
 
    public static XPathStep create(String name, @Nullable XPathCondition condition) { 
        return new XPathStep(name, null, condition); 
    } 
 
    public static XPathStep create(QName name) { 
        return new XPathStep(null, name, null); 
    } 
 
    public static XPathStep create(QName name, @Nullable XPathCondition condition) { 
        return new XPathStep(null, name, condition); 
    } 
 
    private XPathStep(@Nullable String nonColonizedName, 
                      @Nullable QName qualifiedName, 
                      @Nullable XPathCondition condition) { 
        this.nonColonizedName = nonColonizedName; 
        this.qualifiedName = qualifiedName; 
        this.condition = condition; 
    } 
 
    public String createXPathPart(Map<String, String> namespacesToPrefix) { 
        var xPathPart = new StringBuilder(); 
        xPathPart.append('/'); 
        appendName(xPathPart, namespacesToPrefix); 
        appendCondition(xPathPart, namespacesToPrefix); 
        return xPathPart.toString(); 
    } 
 
    private void appendCondition(StringBuilder xPathPart, Map<String, String> namespacesToPrefix) { 
        if (condition != null) { 
            xPathPart.append(condition.createXPathPart(namespacesToPrefix)); 
        } 
    } 
 
    private void appendName(StringBuilder xPathPart, Map<String, String> namespacesToPrefix) { 
        if (nonColonizedName != null) { 
            xPathPart.append(nonColonizedName); 
            return; 
        } 
        if (qualifiedName != null) { 
            var prefix = namespacesToPrefix.get(qualifiedName.getNamespaceURI()); 
            if (prefix != null) { 
                xPathPart.append(prefix); 
                xPathPart.append(':'); 
                xPathPart.append(qualifiedName.getLocalPart()); 
                return; 
            } 
        } 
 
        throw new RuntimeException(String.format("Could neither resolve unqualified (%s) nor qualified (%s) name", 
                nonColonizedName, qualifiedName)); 
    } 
} 
