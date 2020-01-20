package org.somda.sdc.glue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlueConstantsTest {
    @Test
    void staticInitialization() {
        assertNotNull(GlueConstants.SCOPE_SDC_PROVIDER);
    }
}