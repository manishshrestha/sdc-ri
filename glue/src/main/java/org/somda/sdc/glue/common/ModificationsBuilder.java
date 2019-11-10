package org.somda.sdc.glue.common;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to create an {@linkplain MdibDescriptionModifications} object from an {@linkplain Mdib} container.
 */
public class ModificationsBuilder {
    private final ArrayListMultimap<String, AbstractState> states;
    private final MdibDescriptionModifications modifications;

    @AssistedInject
    ModificationsBuilder(@Assisted Mdib mdib) {
        this.states = ArrayListMultimap.create();
        mdib.getMdState().getState().forEach(state -> states.put(state.getDescriptorHandle(), state));

        this.modifications = MdibDescriptionModifications.create();
        mdib.getMdDescription().getMds().forEach(this::build);
    }

    /**
     * Gets the precompiled {@linkplain MdibDescriptionModifications}.
     *
     * @return the built {@linkplain MdibDescriptionModifications}. As the build process takes place right after object
     * construction, this method returns a pre-assembled object structure.
     */
    public MdibDescriptionModifications get() {
        return modifications;
    }

    private void build(MdsDescriptor mds) {
        insert(mds, null);

        mds.getBattery().forEach(descr -> buildLeaf(descr, mds));
        buildLeaf(mds.getClock(), mds);
        build(mds.getSystemContext(), mds);
        build(mds.getAlertSystem(), mds);
        mds.getVmd().forEach(descr -> build(descr, mds));
        build(mds.getSco(), mds);
    }

    private void build(ScoDescriptor sco, AbstractDescriptor parent) {
        insert(sco, parent);
        
        sco.getOperation().forEach(descriptor -> buildLeaf(descriptor, parent));
    }

    private void build(SystemContextDescriptor systemContext, AbstractDescriptor parent) {
        insert(systemContext, parent);
        
        buildMultiStateLeaf(systemContext.getLocationContext(), systemContext);
        buildMultiStateLeaf(systemContext.getPatientContext(), systemContext);
        systemContext.getEnsembleContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        systemContext.getWorkflowContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        systemContext.getOperatorContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        systemContext.getMeansContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
    }

    private void build(AlertSystemDescriptor alertSystem, AbstractDescriptor parent) {
        insert(alertSystem, parent);
        
        alertSystem.getAlertCondition().forEach(descr -> buildLeaf(descr, alertSystem));
        alertSystem.getAlertSignal().forEach(descr -> buildLeaf(descr, alertSystem));
    }

    private void build(VmdDescriptor vmd, AbstractDescriptor parent) {
        insert(vmd, parent);
        
        build(vmd.getSco(), vmd);
        build(vmd.getAlertSystem(), vmd);
        vmd.getChannel().forEach(descr -> buildLeaf(descr, vmd));
    }

    private void build(ChannelDescriptor channel, AbstractDescriptor parent) {
        insert(channel, parent);
        channel.getMetric().forEach(descr -> buildLeaf(descr, channel));
    }

    private <T extends AbstractDescriptor> void buildLeaf(@Nullable T descriptor, AbstractDescriptor parent) {
        if (descriptor != null) {
            modifications.insert(descriptor, singleState(descriptor), parent.getHandle());
        }
    }

    private <T extends AbstractDescriptor> void buildMultiStateLeaf(@Nullable T descriptor, AbstractDescriptor parent) {
        if (descriptor != null) {
            modifications.insert(descriptor, multiStates(descriptor), parent.getHandle());
        }
    }

    private AbstractState singleState(AbstractDescriptor descriptor) {
        final List<AbstractState> statesCollection = states.get(descriptor.getHandle());
        if (statesCollection.isEmpty()) {
            throw new RuntimeException(String.format("No state found for descriptor handle %s", descriptor.getHandle()));
        }

        return statesCollection.get(0);
    }

    private List<AbstractMultiState> multiStates(AbstractDescriptor descriptor) {
        final List<AbstractState> abstractStates = states.get(descriptor.getHandle());
        ArrayList<AbstractMultiState> abstractMultiStates = new ArrayList<>(abstractStates.size());
        abstractStates.forEach(abstractState -> {
            if (abstractState instanceof AbstractMultiState) {
                abstractMultiStates.add(AbstractMultiState.class.cast(abstractState));
            }
        });

        return abstractMultiStates;
    }

    private void insert(AbstractDescriptor descriptor, @Nullable AbstractDescriptor parent) {
        modifications.insert(descriptor, singleState(descriptor), parent.getHandle());
    }
}