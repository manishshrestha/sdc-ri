package org.ieee11073.sdc.biceps.provider;

import org.ieee11073.sdc.biceps.model.participant.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DescriptionChangeSetTest {
    @Test
    public void singleState() throws InstantiationException, IllegalAccessException {
        final List<AbstractDescriptor> descriptors = Arrays.asList(
                createDescriptor("h1", NumericMetricDescriptor.class),
                createDescriptor("h2", StringMetricDescriptor.class),
                createDescriptor("h3", SystemContextDescriptor.class),
                createDescriptor("h4", BatteryDescriptor.class));
        final List<AbstractState> states = Arrays.asList(
                createState("h1", NumericMetricState.class),
                createState("h2", StringMetricState.class),
                createState("h3", SystemContextState.class),
                createState("h4", BatteryState.class));
        DescriptionChangeSet descriptionChangeSet = DescriptionChangeSet.create();

        for (int i = 0; i < descriptors.size(); ++i) {
            descriptionChangeSet.insert(descriptors.get(i), states.get(i));
        }
        for (int i = 0; i < descriptors.size(); ++i) {
            descriptionChangeSet.update(descriptors.get(i), states.get(i));
        }
        for (int i = 0; i < descriptors.size(); ++i) {
            descriptionChangeSet.addItem(DescriptionChangeSet.ModificationType.DELETE, descriptors.get(i),
                    states.get(i));
        }

        checkResultsSingleState(descriptionChangeSet, descriptors, states,
                DescriptionChangeSet.ModificationType.INSERT, descriptors.size(), 0);
        checkResultsSingleState(descriptionChangeSet, descriptors, states,
                DescriptionChangeSet.ModificationType.UPDATE, descriptors.size(), descriptors.size());
        checkResultsSingleState(descriptionChangeSet, descriptors, states,
                DescriptionChangeSet.ModificationType.DELETE, descriptors.size(), descriptors.size() * 2);
    }

    void checkResultsSingleState(DescriptionChangeSet actualChangeSet,
                                 List<AbstractDescriptor> expectedDescriptors,
                                 List<AbstractState> expectedStates,
                                 DescriptionChangeSet.ModificationType modificationType,
                                 int size,
                                 int offset) {
        for (int i = 0; i < size; ++i) {
            DescriptionChangeSet.ChangeItem item = actualChangeSet.getChanges().get(offset + i);
            assertThat(item.getModificationType(), is(modificationType));
            assertThat(item.getDescriptor().getHandle(), is(expectedDescriptors.get(i).getHandle()));
            assertThat(item.getStates().size(), is(1));
            assertThat(item.getStates().get(0).getDescriptorHandle(), is(expectedStates.get(i).getDescriptorHandle()));
        }
    }

    @Test
    public void multiState() throws InstantiationException, IllegalAccessException {
        final List<AbstractContextDescriptor> descriptors = Arrays.asList(
                createDescriptor("c1", LocationContextDescriptor.class),
                createDescriptor("c2", PatientContextDescriptor.class));
        final Map<String, List<AbstractContextState>> states = new HashMap<>();
        states.put("c1", Arrays.asList(
                createContextState("cs1", "c1", LocationContextState.class),
                createContextState("cs2", "c1", LocationContextState.class)));
        states.put("c2", Arrays.asList(
                createContextState("cs3", "c2", PatientContextState.class),
                createContextState("cs4", "c2", PatientContextState.class)));

        DescriptionChangeSet descriptionChangeSet = DescriptionChangeSet.create();

        for (int i = 0; i < descriptors.size(); ++i) {
            descriptionChangeSet.insert(descriptors.get(i), states.get(descriptors.get(i).getHandle()));
        }
        for (int i = 0; i < descriptors.size(); ++i) {
            descriptionChangeSet.update(descriptors.get(i), states.get(descriptors.get(i).getHandle()));
        }
        for (int i = 0; i < descriptors.size(); ++i) {
            descriptionChangeSet.addItem(DescriptionChangeSet.ModificationType.DELETE, descriptors.get(i),
                    states.get(descriptors.get(i).getHandle()));
        }

        checkResultsMultiState(descriptionChangeSet, descriptors, states,
                DescriptionChangeSet.ModificationType.INSERT, descriptors.size(), 0);
        checkResultsMultiState(descriptionChangeSet, descriptors, states,
                DescriptionChangeSet.ModificationType.UPDATE, descriptors.size(), descriptors.size());
        checkResultsMultiState(descriptionChangeSet, descriptors, states,
                DescriptionChangeSet.ModificationType.DELETE, descriptors.size(), descriptors.size() * 2);
    }

    void checkResultsMultiState(DescriptionChangeSet actualChangeSet,
                                List<AbstractContextDescriptor> expectedDescriptors,
                                Map<String, List<AbstractContextState>> expectedStates,
                                DescriptionChangeSet.ModificationType modificationType,
                                int size,
                                int offset) {
        for (int i = 0; i < size; ++i) {
            DescriptionChangeSet.ChangeItem item = actualChangeSet.getChanges().get(offset + i);
            assertThat(item.getModificationType(), is(modificationType));
            assertThat(item.getDescriptor().getHandle(), is(expectedDescriptors.get(i).getHandle()));
            assertThat(item.getStates().size(), is(expectedStates.get(item.getDescriptor().getHandle()).size()));
            for (int j = 0; j < item.getStates().size(); ++j) {
                final AbstractContextState state = expectedStates.get(item.getDescriptor().getHandle()).get(j);
                assertThat(item.getStates().get(j).getDescriptorHandle(), is(state.getDescriptorHandle()));
            }
        }
    }

    @Test
    public void handleDuplicate() throws InstantiationException, IllegalAccessException {
        final List<AbstractDescriptor> descriptors = Arrays.asList(
                createDescriptor("h1", NumericMetricDescriptor.class),
                createDescriptor("h2", AlertSignalDescriptor.class));

        DescriptionChangeSet descriptionChangeSet = DescriptionChangeSet.create();
        try {
            descriptionChangeSet.insert(descriptors.get(0));
            descriptionChangeSet.insert(descriptors.get(1));
            descriptionChangeSet.insert(descriptors.get(2));
            Assert.fail("Expected duplicate runtime exception not thrown");
        } catch (RuntimeException e) {
        }

        descriptionChangeSet.update(descriptors.get(0));
        descriptionChangeSet.update(descriptors.get(1));

        try {
            descriptionChangeSet.update(descriptors.get(0));
            Assert.fail("Expected duplicate runtime exception not thrown");
        } catch (RuntimeException e) {
        }

        descriptionChangeSet.delete(descriptors.get(0));
        descriptionChangeSet.delete(descriptors.get(1));

        try {
            descriptionChangeSet.delete(descriptors.get(0));
            Assert.fail("Expected duplicate runtime exception not thrown");
        } catch (RuntimeException e) {
        }

        final List<EnsembleContextState> contextStates = Arrays.asList(
                createContextState("cs1", "h3", EnsembleContextState.class),
                createContextState("h1", "h3", EnsembleContextState.class));

        try {
            descriptionChangeSet.insert(createDescriptor("h3", EnsembleContextDescriptor.class), contextStates);
            Assert.fail("Expected duplicate runtime exception not thrown");
        } catch (RuntimeException e) {
        }
    }

    static <T extends AbstractDescriptor> T createDescriptor(String handle, Class<T> type)
            throws IllegalAccessException, InstantiationException {
        T instance = type.newInstance();
        instance.setHandle(handle);
        return instance;
    }

    static <T extends AbstractState> T createState(String handle, Class<T> type)
            throws IllegalAccessException, InstantiationException {
        T instance = type.newInstance();
        instance.setDescriptorHandle(handle);
        return instance;
    }

    static <T extends AbstractContextState> T createContextState(String handle, String descrHandle, Class<T> type)
            throws IllegalAccessException, InstantiationException {
        T instance = type.newInstance();
        instance.setHandle(handle);
        instance.setDescriptorHandle(descrHandle);
        return instance;
    }
}