package org.ieee11073.sdc.biceps.common;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.BasicConfigurator;
import org.ieee11073.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.ieee11073.sdc.biceps.guice.DefaultBicepsModule;
import org.ieee11073.sdc.biceps.model.participant.*;
import org.ieee11073.sdc.biceps.testutil.Handles;
import org.ieee11073.sdc.biceps.testutil.MockModelFactory;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MdibStorageImplTest {
    private Injector injector;
    private MdibStorage mdibStorage;

    @Before
    public void setUp() {
        BasicConfigurator.configure();
        injector = Guice.createInjector(new DefaultHelperModule(), new DefaultBicepsModule(), new DefaultBicepsConfigModule());
        mdibStorage = injector.getInstance(MdibStorage.class);
    }

    private void applyDescriptionWithVersion(MdibDescriptionModification.Type type,
                                             BigInteger version) throws InstantiationException, IllegalAccessException {
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.MDS_0, version, MdsDescriptor.class),
                MockModelFactory.createState(Handles.MDS_0, version, MdsState.class));
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.SYSTEMCONTEXT_0, version, MdsDescriptor.class),
                MockModelFactory.createState(Handles.SYSTEMCONTEXT_0, version, SystemContextState.class),
                Handles.MDS_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.VMD_0, version, VmdDescriptor.class),
                MockModelFactory.createState(Handles.VMD_0, version, VmdState.class),
                Handles.MDS_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CHANNEL_0, version, ChannelDescriptor.class),
                MockModelFactory.createState(Handles.CHANNEL_0, version, ChannelState.class),
                Handles.VMD_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CHANNEL_1, version, ChannelDescriptor.class),
                MockModelFactory.createState(Handles.CHANNEL_1, version, ChannelState.class),
                Handles.VMD_0);
        modifications.add(type,
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_0,
                        version,
                        PatientContextDescriptor.class),
                Arrays.asList(
                        MockModelFactory.createContextState(Handles.CONTEXT_0, Handles.CONTEXTDESCRIPTOR_0,
                                version, version, PatientContextState.class),
                        MockModelFactory.createContextState(Handles.CONTEXT_1, Handles.CONTEXTDESCRIPTOR_0,
                                version, version, PatientContextState.class)
                ),
                Handles.SYSTEMCONTEXT_0);

        mdibStorage.apply(modifications);
    }
    
    private void testWithVersion(List<String> testedHandles, BigInteger version) {
        testWithVersion(testedHandles, version, version);
    }

    private void testWithVersion(List<String> testedHandles, BigInteger descrVersion, BigInteger stateVersion) {
        testedHandles.stream().forEach(handle -> {
            assertThat(mdibStorage.getEntity(handle).isPresent(), is(true));
            assertThat(mdibStorage.getEntity(handle).get().getDescriptor().getDescriptorVersion(), is(descrVersion));
            mdibStorage.getEntity(handle).get().getStates().stream().forEach(state ->
                    assertThat(state.getStateVersion(), is(stateVersion)));
        });
    }
    
    @Test
    public void writeDescription() throws InstantiationException, IllegalAccessException {
        List<String> testedHandles = Arrays.asList(
                Handles.MDS_0,
                Handles.SYSTEMCONTEXT_0,
                Handles.VMD_0,
                Handles.CHANNEL_0,
                Handles.CHANNEL_1,
                Handles.CONTEXTDESCRIPTOR_0);

        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);
        testWithVersion(testedHandles, BigInteger.ZERO);
        applyDescriptionWithVersion(MdibDescriptionModification.Type.UPDATE, BigInteger.ONE);
        testWithVersion(testedHandles, BigInteger.ONE);
        applyDescriptionWithVersion(MdibDescriptionModification.Type.UPDATE, BigInteger.TEN);
        testWithVersion(testedHandles, BigInteger.TEN);

        applyDescriptionWithVersion(MdibDescriptionModification.Type.DELETE, BigInteger.ZERO);
        testedHandles.stream().forEach(handle -> {
            assertThat(mdibStorage.getEntity(handle).isPresent(), is(false));
        });
    }

    @Test
    public void mdibAccess() throws InstantiationException, IllegalAccessException {
        applyDescriptionWithVersion(MdibDescriptionModification.Type.INSERT, BigInteger.ZERO);
        assertThat(mdibStorage.getEntity(Handles.UNKNOWN).isPresent(), is(false));
        assertThat(mdibStorage.getEntity(Handles.MDS_0).isPresent(), is(true));

        assertThat(mdibStorage.getDescriptor(Handles.UNKNOWN, MdsDescriptor.class).isPresent(), is(false));
        assertThat(mdibStorage.getDescriptor(Handles.VMD_0, MdsDescriptor.class).isPresent(), is(false));
        assertThat(mdibStorage.getDescriptor(Handles.VMD_0, VmdDescriptor.class).isPresent(), is(true));

        assertThat(mdibStorage.getContextStates(Handles.UNKNOWN, PatientContextState.class).isEmpty(), is(true));
        assertThat(mdibStorage.getContextStates(Handles.VMD_0, PatientContextState.class).isEmpty(), is(true));
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_0, LocationContextState.class).isEmpty(), is(true));
        assertThat(mdibStorage.getContextStates(Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class).size(), is(2));
    }

    @Test
    public void writeStates() throws IllegalAccessException, InstantiationException {
        List<String> testedHandles = Arrays.asList(
                Handles.ALERTSYSTEM_0,
                Handles.ALERTCONDITION_0,
                Handles.ALERTCONDITION_1);

        final MdibDescriptionModifications descriptionModifications = MdibDescriptionModifications.create();
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.MDS_0, MdsDescriptor.class),
                MockModelFactory.createState(Handles.MDS_0, MdsState.class));
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.ALERTSYSTEM_0, AlertSystemDescriptor.class),
                MockModelFactory.createState(Handles.ALERTSYSTEM_0, AlertSystemState.class),
                Handles.MDS_0);
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.ALERTCONDITION_0, AlertConditionDescriptor.class),
                MockModelFactory.createState(Handles.ALERTCONDITION_0, AlertConditionState.class),
                Handles.ALERTSYSTEM_0);
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.ALERTCONDITION_1, AlertConditionDescriptor.class),
                MockModelFactory.createState(Handles.ALERTCONDITION_1, AlertConditionState.class),
                Handles.ALERTSYSTEM_0);
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.SYSTEMCONTEXT_0, SystemContextDescriptor.class),
                MockModelFactory.createState(Handles.SYSTEMCONTEXT_0, SystemContextState.class));
        descriptionModifications.insert(
                MockModelFactory.createDescriptor(Handles.CONTEXTDESCRIPTOR_0, PatientContextDescriptor.class),
                Arrays.asList(
                        MockModelFactory.createContextState(Handles.CONTEXT_0, Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class),
                        MockModelFactory.createContextState(Handles.CONTEXT_1, Handles.CONTEXTDESCRIPTOR_0, PatientContextState.class)));

        mdibStorage.apply(descriptionModifications);

        testWithVersion(testedHandles, BigInteger.ZERO);

        MdibStateModifications stateModifications = MdibStateModifications.create(MdibStateModifications.Type.ALERT);
        stateModifications.add(MockModelFactory.createState(Handles.ALERTSYSTEM_0, BigInteger.ONE, AlertSystemState.class));
        stateModifications.add(MockModelFactory.createState(Handles.ALERTCONDITION_0, BigInteger.ONE, AlertConditionState.class));
        stateModifications.add(MockModelFactory.createState(Handles.ALERTCONDITION_1, BigInteger.ONE, AlertConditionState.class));
        try {
            stateModifications.add(MockModelFactory.createState(Handles.MDS_0, BigInteger.ONE, MdsState.class));
            Assert.fail("Could add MDS to alert state change set");
        } catch (Exception e){
        }

        mdibStorage.apply(stateModifications);
        testWithVersion(testedHandles, BigInteger.ZERO, BigInteger.ONE);

        testedHandles = Arrays.asList(Handles.CONTEXTDESCRIPTOR_0);

        stateModifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);

        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_0, Handles.CONTEXTDESCRIPTOR_0, BigInteger.ONE, PatientContextState.class));
        stateModifications.add(MockModelFactory.createContextState(Handles.CONTEXT_1, Handles.CONTEXTDESCRIPTOR_0, BigInteger.ONE, PatientContextState.class));
        mdibStorage.apply(stateModifications);

        testWithVersion(testedHandles, BigInteger.ZERO, BigInteger.ONE);
    }
}
