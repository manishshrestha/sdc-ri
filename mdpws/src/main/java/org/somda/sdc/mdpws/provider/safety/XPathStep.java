package org.somda.sdc.mdpws.provider.safety;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Representation of an XML tree step being used by {@linkplain XPathBuilder} to create XPath expressions.
 */
public class XPathStep {
    private final String nonColonizedName;
    private final QName qualifiedName;
    private final XPathCondition condition;

    /**
     * Creates a plain step based on an unqualified element name.
     *
     * @param name the element name.
     * @return a new instance that represents the XPath step.
     */
    public static XPathStep create(String name) {
        return new XPathStep(name, null, null);
    }

    /**
     * Creates a step based on an unqualified element name with an appended condition.
     *
     * @param name      the element name.
     * @param condition the condition to append.
     * @return a new instance that represents the XPath step.
     */
    public static XPathStep create(String name, @Nullable XPathCondition condition) {
        return new XPathStep(name, null, condition);
    }

    /**
     * Creates a plain step based on a qualified element name.
     *
     * @param name the element name.
     * @return a new instance that represents the XPath step.
     */
    public static XPathStep create(QName name) {
        return new XPathStep(null, name, null);
    }

    /**
     * Creates a step based on a qualified element name with an appended condition.
     *
     * @param name      the element name.
     * @param condition the condition to append.
     * @return a new instance that represents the XPath step.
     */
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

    /**
     * Creates the serialized XPath string, supposed to be used by {@linkplain XPathBuilder} only.
     *
     * @param namespacesToPrefix the namespace/prefix mapping to be used for qualified names.
     * @return an XPath string matching this step prepended with a slash.
     */
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
