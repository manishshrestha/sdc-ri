package org.somda.sdc.mdpws.provider.safety;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to create XPath expressions that comply with restrictions inflicted by MDPWS.
 * <p>
 * The {@link XPathBuilder} relies on {@link XPathStep} objects which can be used plainly or complemented with
 * {@link XPathCondition} objects in order to create XML tree traversal steps separated by slashes.
 * <p>
 * The builder does not do any validations.
 * It's up to the user to create meaningful XPath expressions.
 */
public class XPathBuilder {
    private final Map<String, String> namespacesToPrefix;
    private final List<XPathStep> steps;

    /**
     * Creates a new fluent interface to add XPath steps.
     *
     * @param namespacesToPrefix namespace/prefix mapping to create qualified names.
     * @return an {@link XPathBuilder} instance.
     */
    public static XPathBuilder create(Map<String, String> namespacesToPrefix) {
        return new XPathBuilder(namespacesToPrefix);
    }

    /**
     * Adds an XPath step.
     *
     * @param step the step to add.
     * @return this object for fluent interfacing.
     */
    public XPathBuilder add(XPathStep step) {
        steps.add(step);
        return this;
    }

    /**
     * Once all desired steps are added, this method creates an unqualified attribute selector attached to the added
     * XPath steps.
     *
     * @param name the attribute name.
     * @return the XPath string derived from the added steps plus the attribute selector.
     */
    public String getAttribute(String name) {
        return buildSteps().append("/@").append(name).toString();
    }

    /**
     * Once all desired steps are added, this method creates a qualified attribute selector attached to the added
     * XPath steps.
     *
     * @param name the attribute name.
     * @return the XPath string derived from the added steps plus the attribute selector.
     */
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

    /**
     * Once all desired steps are added, this method creates a XML element text selector to be added to the XPath steps.
     *
     * @return the XPath string derived from the added steps plus the element text selector.
     */
    public String getElementText() {
        return buildSteps().append("/text()").toString();
    }

    /**
     * Resets the steps.
     * <p>
     * This function allows an {@link XPathBuilder} instance to be reused.
     */
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
