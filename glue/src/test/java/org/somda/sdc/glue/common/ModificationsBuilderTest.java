package org.somda.sdc.glue.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import test.org.somda.common.LoggingTestWatcher;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class ModificationsBuilderTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    @Test
    void createSingleStateIfNotExists() {
        var builderFactory = UT.getInjector().getInstance(ModificationsBuilderFactory.class);

        var vmd = MockModelFactory.createDescriptor(Handles.VMD_0, VmdDescriptor.builder()).build();

        var mds = MockModelFactory.createDescriptor(Handles.MDS_0, MdsDescriptor.builder())
            .addVmd(vmd)
            .build();

        var mdDescription = MdDescription.builder()
            .addMds(mds)
            .build();

        var mdib = Mdib.builder()
            .withMdDescription(mdDescription)
            .build();

        assertThrows(RuntimeException.class, () ->
                builderFactory.createModificationsBuilder(mdib, false));

        assertDoesNotThrow(() -> builderFactory.createModificationsBuilder(mdib, true));

        var modifications = builderFactory.createModificationsBuilder(mdib, true).get();
        assertEquals(2, modifications.getModifications().size());
        assertEquals(1, modifications.getModifications().get(0).getStates().size());
        assertEquals(1, modifications.getModifications().get(1).getStates().size());
    }

    @Test
    void applyDefaultStates() {
        var builderFactory = UT.getInjector().getInstance(ModificationsBuilderFactory.class);

        var mdDescription = MdDescription.builder();
        var mds = MockModelFactory.createDescriptor(Handles.MDS_0, MdsDescriptor.builder());
        var clock = MockModelFactory.createDescriptor(Handles.CLOCK_0, ClockDescriptor.builder());
        mds.withClock(clock.build());
        mdDescription.addMds(mds.build());
        var mdib = Mdib.builder()
            .withMdDescription(mdDescription.build())
            .build();

        ModificationsBuilder modificationsBuilder = builderFactory.createModificationsBuilder(mdib, true);
        MdibDescriptionModifications modifications = modificationsBuilder.get();
        assertEquals(2, modifications.getModifications().size());
        assertEquals(1,  modifications.getModifications().get(1).getStates().size());
        var state = modifications.getModifications().get(1).getStates().get(0);
        assertTrue(state instanceof ClockState);
        assertFalse(((ClockState)state).isRemoteSync());
    }
}