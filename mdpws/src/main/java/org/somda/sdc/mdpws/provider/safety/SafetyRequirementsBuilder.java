package org.somda.sdc.mdpws.provider.safety;

import org.somda.sdc.mdpws.model.DualChannelDefinitionType;
import org.somda.sdc.mdpws.model.SafetyRequirementsType;
import org.somda.sdc.mdpws.model.SelectorType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class to create safety requirements to be attached as MDIB extension.
 */
public class SafetyRequirementsBuilder {
    private List<SelectorType> dualChannelSelectors;
    private List<SelectorType> safetyContextSelectors;

    private SafetyRequirementsBuilder() {
        dualChannelSelectors = new ArrayList<>();
        safetyContextSelectors = new ArrayList<>();
    }

    /**
     * Creates a builder instance.
     *
     * @return a new builder instance.
     */
    public static SafetyRequirementsBuilder create() {
        return new SafetyRequirementsBuilder();
    }

    /**
     * Adds a dual channel requirement.
     *
     * @param selectorId the selector to be used.
     *                   It's up to the caller to ensure uniqueness.
     * @param xPath      the XPath expression to be used for this dual channel selector.
     *                   XPath expressions can be generated by using {@link XPathBuilder}, which can be created by
     *                   using the {@link SafetyXPath} class.
     * @return this object for fluent interfacing.
     * @see SafetyXPath
     */
    public SafetyRequirementsBuilder addDualChannel(String selectorId, String xPath) {
        dualChannelSelectors.add(createSelector(selectorId, xPath));
        return this;
    }

    /**
     * Adds a dual channel requirement with UUID selector.
     * <p>
     * Uses an automatically generated UUID as a selector in order to increase the probability for unique selector IDs.
     *
     * @param xPath the XPath expression to be used for this dual channel selector.
     *              XPath expressions can be generated by using {@link XPathBuilder}, which can be created by
     *              using the {@link SafetyXPath} class.
     * @return this object for fluent interfacing.
     * @see SafetyXPath
     */
    public SafetyRequirementsBuilder addDualChannel(String xPath) {
        dualChannelSelectors.add(createSelector(UUID.randomUUID().toString(), xPath));
        return this;
    }

    /**
     * Adds a safety context requirement.
     *
     * @param selectorId the selector to be used.
     *                   It's up to the caller to ensure uniqueness.
     * @param xPath      the XPath expression to be used for this safety context selector.
     *                   XPath expressions can be generated by using {@link XPathBuilder}, which can be created by
     *                   using the {@link SafetyXPath} class.
     * @return this object for fluent interfacing.
     * @see SafetyXPath
     */
    public SafetyRequirementsBuilder addSafetyContext(String selectorId, String xPath) {
        safetyContextSelectors.add(createSelector(selectorId, xPath));
        return this;
    }

    /**
     * Adds a safety context requirement with UUID selector.
     * <p>
     * Uses an automatically generated UUID as a selector in order to increase the probability for unique selector IDs.
     *
     * @param xPath the XPath expression to be used for this safety context selector.
     *              XPath expressions can be generated by using {@link XPathBuilder}, which can be created by
     *              using the {@link SafetyXPath} class.
     * @return this object for fluent interfacing.
     * @see SafetyXPath
     */
    public SafetyRequirementsBuilder addSafetyContext(String xPath) {
        safetyContextSelectors.add(createSelector(UUID.randomUUID().toString(), xPath));
        return this;
    }

    /**
     * Once all requirements are added, this method can be used to create the actual safety requirements object.
     *
     * @return JAXB-compatible safety requirements object that can be attached to a BICEPS extension.
     */
    public SafetyRequirementsType get() {
        var requirements = new SafetyRequirementsType();
        var dualChannel = new SafetyRequirementsType.DualChannelDefinition();
        var safetyContext = new SafetyRequirementsType.SafetyContextDefinition();

        var dualChannelType = new DualChannelDefinitionType();
        dualChannelType.getSelector().addAll(dualChannelSelectors);
        dualChannel.setValue(dualChannelType);

        var safetyContextType = new org.somda.sdc.mdpws.model.SafetyContextDefinitionType();
        safetyContextType.getSelector().addAll(safetyContextSelectors);
        safetyContext.setValue(safetyContextType);

        requirements.setDualChannelDefinition(dualChannel);
        requirements.setSafetyContextDefinition(safetyContext);

        return requirements;
    }

    private SelectorType createSelector(String id, String xPath) {
        var selector = new SelectorType();
        selector.setId(id);
        selector.setValue(xPath);
        return selector;
    }

} 
