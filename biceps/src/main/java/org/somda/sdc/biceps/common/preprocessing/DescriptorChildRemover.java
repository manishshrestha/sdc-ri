package org.somda.sdc.biceps.common.preprocessing;

import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Removes children from descriptors in order to avoid redundant information in the MDIB storage.
 */
public class DescriptorChildRemover implements DescriptionPreprocessingSegment {

    @Override
    public List<MdibDescriptionModification> process(List<MdibDescriptionModification> allModifications,
                                                     MdibStorage storage) throws Exception {
        return allModifications.stream().map(mod -> {
            final var descriptorBuilder = mod.getDescriptor().newCopyBuilder();
            if (descriptorBuilder instanceof MdsDescriptor.Builder) {
                removeChildren((MdsDescriptor.Builder) descriptorBuilder);
            } else if (descriptorBuilder instanceof VmdDescriptor.Builder) {
                removeChildren((VmdDescriptor.Builder) descriptorBuilder);
            } else if (descriptorBuilder instanceof ChannelDescriptor.Builder) {
                removeChildren((ChannelDescriptor.Builder) descriptorBuilder);
            } else if (descriptorBuilder instanceof ScoDescriptor.Builder) {
                removeChildren((ScoDescriptor.Builder) descriptorBuilder);
            } else if (descriptorBuilder instanceof SystemContextDescriptor.Builder) {
                removeChildren((SystemContextDescriptor.Builder) descriptorBuilder);
            } else if (descriptorBuilder instanceof AlertSystemDescriptor.Builder) {
                removeChildren((AlertSystemDescriptor.Builder) descriptorBuilder);
            }
            final var descriptor = descriptorBuilder.build();

            mod.setDescriptor(descriptor);
            return mod;
        }).collect(Collectors.toList());
    }

    private void removeChildren(AlertSystemDescriptor.Builder descriptor) {
        descriptor.withAlertSignal()
                .withAlertCondition();
    }

    private void removeChildren(SystemContextDescriptor.Builder descriptor) {
        descriptor.withLocationContext(null)
            .withPatientContext(null)
            .withMeansContext()
            .withOperatorContext()
            .withWorkflowContext()
            .withEnsembleContext();
    }

    private void removeChildren(ScoDescriptor.Builder descriptor) {
        descriptor.withOperation();
    }

    private void removeChildren(ChannelDescriptor.Builder descriptor) {
        descriptor.withMetric();
    }

    private void removeChildren(VmdDescriptor.Builder descriptor) {
        descriptor.withChannel()
            .withAlertSystem(null);
    }

    /**
     * Removes the children from a provided mds.
     * <p>
     * Removes the battery, clock, system context, vmd, alert system and sco
     * @param mds without the children
     */
    private void removeChildren(MdsDescriptor.Builder mds) {
        mds.withBattery()
            .withClock(null)
            .withSystemContext(null)
            .withVmd()
            .withAlertSystem(null)
            .withSco(null);
    }
}
