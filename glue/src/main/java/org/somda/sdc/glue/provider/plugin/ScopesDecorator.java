package org.somda.sdc.glue.provider.plugin;

import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.net.URI;
import java.util.Set;

/**
 * Decorator interface to allow extending WS-Discovery Scopes during runtime.
 * <p>
 * The decorator does only allow changes to scopes based on context and description modifications.
 * This interface can be used to extend the required types and scopes implementation {@link SdcRequiredTypesAndScopes}
 * by means of the decorator pattern.
 */
public interface ScopesDecorator {
    /**
     * To be called when context changes come up.
     * <p>
     * Please note that context changes are also triggered after description modifications have been triggered.
     * Make sure to not change scopes twice by using {@link #mdibVersionSeenBefore(MdibVersion)}.
     *
     * @param message the modification message as being delivered by BICEPS.
     */
    void updateContexts(ContextStateModificationMessage message);

    /**
     * To be called when description changes come up.
     *
     * @param message the modification message as being delivered by BICEPS.
     */
    void updateDescription(DescriptionModificationMessage message);

    /**
     * Gives the order to append the given set of scopes and send a the Hello afterwards.
     * <p>
     * Call this function after context or description modification message have been processed.
     *
     * @param scopes the scopes to update.
     */
    void appendScopesAndSendHello(Set<URI> scopes);

    /**
     * Compares an MDIB version against one that was seen already.
     * <p>
     * This function helps you classifying if a change to the MDIB should be followed by resending a Hello.
     * If this function is called the first time, it returns false as there hasn't been a version seen before.
     * If this function is called two times and more, then it compares the MDIB version previously seen with the given
     * MDIB version and returns
     * <ul>
     * <li>false if the given MDIB version is newer than the previous version (based upon the MDIB versions's compare function)
     * <li>false if the sequence id has changed (one better sends out Hellos then even if this sequence id change should not happen under normal conditions)
     * <li>true otherwise
     * </ul>
     * By calling this function, cancel updating scopes and resending Hellos if the result is true, and continue
     * otherwise.
     *
     * @param mdibVersion the version to compare against the internally stored one.
     * @return true or false based on the conditions defined in the description of this function.
     * @see MdibVersion#compareTo(MdibVersion)
     */
    boolean mdibVersionSeenBefore(MdibVersion mdibVersion);
}
