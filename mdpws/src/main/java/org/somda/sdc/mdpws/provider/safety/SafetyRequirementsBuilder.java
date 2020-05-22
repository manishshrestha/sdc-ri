package org.somda.sdc.mdpws.provider.safety; 
 
import org.somda.sdc.mdpws.model.*;

import java.util.ArrayList; 
import java.util.List; 
import java.util.UUID; 
 
public class SafetyRequirementsBuilder { 
    private List<SelectorType> dualChannelSelectors;
    private List<SelectorType> safetyContextSelectors; 
 
    private SafetyRequirementsBuilder() { 
        dualChannelSelectors = new ArrayList<>(); 
        safetyContextSelectors = new ArrayList<>(); 
    } 
 
    public static SafetyRequirementsBuilder create() { 
        return new SafetyRequirementsBuilder(); 
    } 
 
    public SafetyRequirementsBuilder addDualChannel(String selectorId, String xPath) { 
        dualChannelSelectors.add(createSelector(selectorId, xPath)); 
        return this; 
    } 
 
    public SafetyRequirementsBuilder addDualChannel(String xPath) { 
        dualChannelSelectors.add(createSelector(UUID.randomUUID().toString(), xPath)); 
        return this; 
    } 
 
    public SafetyRequirementsBuilder addSafetyContext(String selectorId, String xPath) { 
        safetyContextSelectors.add(createSelector(selectorId, xPath)); 
        return this; 
    } 
 
    public SafetyRequirementsBuilder addSafetyContext(String xPath) { 
        safetyContextSelectors.add(createSelector(UUID.randomUUID().toString(), xPath)); 
        return this; 
    } 
 
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
