package org.somda.sdc.mdpws.provider.safety; 
 
import javax.xml.namespace.QName; 
import java.util.ArrayList; 
import java.util.List; 
import java.util.Map; 
 
public class XPathBuilder { 
 
    private final Map<String, String> namespacesToPrefix; 
    private final List<XPathStep> steps; 
 
    public static XPathBuilder create(Map<String, String> namespacesToPrefix) { 
        return new XPathBuilder(namespacesToPrefix); 
    } 
 
    public XPathBuilder add(XPathStep step) { 
        steps.add(step); 
        return this; 
    } 
 
    public String getAttribute(String name) { 
        return buildSteps().append("/@").append(name).toString(); 
    } 
 
    public String getAttribute(QName name) { 
        var prefix = namespacesToPrefix.get(name.getNamespaceURI()); 
        if (prefix == null) { 
            throw new RuntimeException(String.format("Could not resolve prefix for qualified name '%s'", name)); 
        } 
 
        return buildSteps() 
                .append("/@") 
                .append(prefix) 
                .append(':') 
                .append(name.getLocalPart()) 
                .toString(); 
    } 
 
    public String getElementText() { 
        return buildSteps().append("/text()").toString(); 
    } 
 
    public void clear() { 
        steps.clear(); 
    } 
 
    private XPathBuilder(Map<String, String> namespacesToPrefix) { 
        this.namespacesToPrefix = namespacesToPrefix; 
        this.steps = new ArrayList<>(); 
    } 
 
    private StringBuilder buildSteps() { 
        StringBuilder builder = new StringBuilder(); 
        for (XPathStep step : steps) { 
            builder.append(step.createXPathPart(namespacesToPrefix)); 
        } 
        return builder; 
    } 
} 
