package org.ieee11073.sdc.biceps.common.preprocessing;

import org.ieee11073.sdc.biceps.UnitTestUtil;
import org.ieee11073.sdc.biceps.common.MdibDescriptionModification;
import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.common.storage.factory.MdibStorageFactory;
import org.ieee11073.sdc.biceps.model.participant.ChannelDescriptor;
import org.ieee11073.sdc.biceps.model.participant.MdsDescriptor;
import org.ieee11073.sdc.biceps.model.participant.NumericMetricDescriptor;
import org.ieee11073.sdc.biceps.model.participant.VmdDescriptor;
import org.ieee11073.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class TypeConsistencyCheckerTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private TypeConsistencyChecker consistencyHandler;
    private MdibStorage mdibStorage;

    private String mds;
    private String vmd1;
    private String vmd2;
    private String channel;

    @BeforeEach
    void beforeEach() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        consistencyHandler = UT.getInjector().getInstance(TypeConsistencyChecker.class);
        mdibStorage = UT.getInjector().getInstance(MdibStorageFactory.class).createMdibStorage();

        mds = "mds";
        vmd1 = "vmd1";
        vmd2 = "vmd2";
        channel = "channel";

        MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(MockModelFactory.createDescriptor(mds, MdsDescriptor.class));
        modifications.insert(MockModelFactory.createDescriptor(vmd1, VmdDescriptor.class));
        modifications.insert(MockModelFactory.createDescriptor(channel, ChannelDescriptor.class));
        mdibStorage.apply(modifications);
    }

    @Test
    void missingParent() throws Exception {
        MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(MockModelFactory.createDescriptor("vmd3", VmdDescriptor.class));
        assertThrows(Exception.class, () -> apply(modifications));
    }

    @Test
    void mdsWithParent() throws Exception {
        MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(MockModelFactory.createDescriptor("mds", MdsDescriptor.class), "invalid");
        assertThrows(Exception.class, () -> apply(modifications));
    }

    @Test
    void invalidParent() throws Exception {
        MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(MockModelFactory.createDescriptor("vmd", VmdDescriptor.class), "invalid");
        assertThrows(Exception.class, () -> apply(modifications));
    }

    @Test
    void validParentInMdibStorage() throws Exception {
        MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(MockModelFactory.createDescriptor("metric", NumericMetricDescriptor.class), channel);
        assertDoesNotThrow(() -> apply(modifications));
    }

    @Test
    void validParentInModifications() throws Exception {
        MdibDescriptionModifications modifications = MdibDescriptionModifications.create();
        modifications.insert(MockModelFactory.createDescriptor(vmd2, VmdDescriptor.class), mds);
        modifications.insert(MockModelFactory.createDescriptor("channel", ChannelDescriptor.class), vmd2);
        assertDoesNotThrow(() -> apply(modifications));
    }

    private void apply(MdibDescriptionModifications modifications) throws Exception {
        for (MdibDescriptionModification modification : modifications.getModifications()) {
            consistencyHandler.process(modifications, modification, mdibStorage);
        }
    }
}