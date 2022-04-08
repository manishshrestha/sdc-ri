package org.somda.sdc.biceps.common;

import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;

import static org.junit.jupiter.api.Assertions.*;

class BicepsModelCloningTest {

    private static final UnitTestUtil UT = new UnitTestUtil();

    @Test
    void deepCopy() {
        var mdsDescription = new MdsDescriptor();
        var metadata = new MdsDescriptor.MetaData();
        metadata.setModelNumber("initial_model");
        mdsDescription.setMetaData(metadata);

        var bicepsModelCloning = UT.getInjector().getInstance(BicepsModelCloning.class);

        var mdsDescriptionCopy = bicepsModelCloning.deepCopy(mdsDescription);

        assertEquals(mdsDescriptionCopy, mdsDescription);
        // update one object and check if copy isn't changed
        mdsDescription.getMetaData().setModelNumber("updated_model");

        assertNotEquals(mdsDescription.getMetaData(), mdsDescriptionCopy.getMetaData());
        assertEquals(mdsDescription.getMetaData().getModelNumber(), "updated_model");
        assertEquals(mdsDescriptionCopy.getMetaData().getModelNumber(), "initial_model");
    }
}