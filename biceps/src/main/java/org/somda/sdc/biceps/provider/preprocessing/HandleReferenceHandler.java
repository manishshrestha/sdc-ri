package org.somda.sdc.biceps.provider.preprocessing;

import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Preprocessing segment that appends descriptor references to states.
 * <p>
 * This segment allows the user to spare calling
 * {@link org.somda.sdc.biceps.model.participant.AbstractState#setDescriptorHandle(String)}
 * by automatically setting the descriptor reference at each state.
 */
public class HandleReferenceHandler implements DescriptionPreprocessingSegment {
    @Override
    public List<MdibDescriptionModification> process(List<MdibDescriptionModification> modifications,
                                                     MdibStorage storage) {
        // TODO LDe: I do not like this handler, remove?
        return modifications.stream().map(it -> {
            var updatedStates = it.getStates().stream().map(it2 ->
                it2.newCopyBuilder().withDescriptorHandle(it.getDescriptor().getHandle()).build()
            ).collect(Collectors.toList());

            return new MdibDescriptionModification(
                it.getModificationType(), it.getDescriptor(), updatedStates,
                it.getParentHandle().orElse(null)
            );
        }).collect(Collectors.toList());
    }
}
