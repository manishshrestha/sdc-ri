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
        mds.setBattery(null);
        buildLeaf(mds.getClock(), mds);
        build(mds.getSystemContext(), mds);
        mds.setSystemContext(null);
        build(mds.getAlertSystem(), mds);
        mds.setAlertSystem(null);
        mds.getVmd().forEach(descr -> build(descr, mds));
        mds.setVmd(null);
        build(mds.getSco(), mds);
        mds.setSco(null);
    }

    private void build(@Nullable ScoDescriptor sco, AbstractDescriptor parent) {
        if (sco == null) {
            return;
        }
        insert(sco, parent);

        sco.getOperation().forEach(descriptor -> buildLeaf(descriptor, sco));
        sco.setOperation(null);
    }

    private void build(@Nullable SystemContextDescriptor systemContext, AbstractDescriptor parent) {
        if (systemContext == null) {
            return;
        }
        insert(systemContext, parent);

        buildMultiStateLeaf(systemContext.getLocationContext(), systemContext);
        systemContext.setLocationContext(null);
        buildMultiStateLeaf(systemContext.getPatientContext(), systemContext);
        systemContext.setPatientContext(null);
        systemContext.getEnsembleContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        systemContext.setEnsembleContext(null);
        systemContext.getWorkflowContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        systemContext.setWorkflowContext(null);
        systemContext.getOperatorContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        systemContext.setOperatorContext(null);
        systemContext.getMeansContext().forEach(descr -> buildMultiStateLeaf(descr, systemContext));
        systemContext.setMeansContext(null);
    }

    private void build(@Nullable AlertSystemDescriptor alertSystem, AbstractDescriptor parent) {
        if (alertSystem == null) {
            return;
        }
        insert(alertSystem, parent);

        alertSystem.getAlertCondition().forEach(descr -> buildLeaf(descr, alertSystem));
        alertSystem.setAlertCondition(null);
        alertSystem.getAlertSignal().forEach(descr -> buildLeaf(descr, alertSystem));
        alertSystem.setAlertSignal(null);
    }

    private void build(@Nullable VmdDescriptor vmd, AbstractDescriptor parent) {
        if (vmd == null) {
            return;
        }
        insert(vmd, parent);

        build(vmd.getSco(), vmd);
        vmd.setSco(null);
        build(vmd.getAlertSystem(), vmd);
        vmd.setAlertSystem(null);
        vmd.getChannel().forEach(descr -> build(descr, vmd));
        vmd.setChannel(null);
    }

    private void build(@Nullable ChannelDescriptor channel, AbstractDescriptor parent) {
        if (channel == null) {
            return;
        }
        insert(channel, parent);
        channel.getMetric().forEach(descr -> buildLeaf(descr, channel));
        channel.setMetric(null);
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
        String parentHandle = null;
        if (parent != null) {
            parentHandle = parent.getHandle();
        }
        modifications.insert(descriptor, singleState(descriptor), parentHandle);
    }
}