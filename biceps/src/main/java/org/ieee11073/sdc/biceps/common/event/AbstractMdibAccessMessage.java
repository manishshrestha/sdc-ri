package org.ieee11073.sdc.biceps.common.event;

import org.ieee11073.sdc.biceps.common.access.MdibAccess;

public abstract class AbstractMdibAccessMessage {
    private final MdibAccess mdibAccess;

    AbstractMdibAccessMessage(MdibAccess mdibAccess) {
        this.mdibAccess = mdibAccess;
    }

    public MdibAccess getMdibAccess() {
        return mdibAccess;
    }
}
