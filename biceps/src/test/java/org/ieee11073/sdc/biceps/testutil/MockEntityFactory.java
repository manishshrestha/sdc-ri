package org.ieee11073.sdc.biceps.testutil;

import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;
import org.ieee11073.sdc.biceps.common.factory.MdibEntityFactory;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MockEntityFactory {
    private MdibEntityFactory mdibEntityFactory;
    private MdibVersion baseVersion;

    public MockEntityFactory(MdibEntityFactory mdibEntityFactory,
                             MdibVersion baseVersion) {
        this.mdibEntityFactory = mdibEntityFactory;
        this.baseVersion = baseVersion;
    }

    MdibEntity createEntity(String handle,
                            @Nullable String parentHandle,
                            @Nullable List<String> childHandles,
                            AbstractDescriptor descriptor,
                            @Nullable List<AbstractState> states,
                            @Nullable BigInteger mdibVersionCounter) {
        mdibVersionCounter = Optional.ofNullable(mdibVersionCounter).orElse(baseVersion.getVersion());
        return mdibEntityFactory.createMdibEntity(
                parentHandle,
                Optional.ofNullable(childHandles).orElse(Collections.emptyList()),
                descriptor,
                Optional.ofNullable(states).orElse(Collections.emptyList()),
                MdibVersion.setVersionCounter(baseVersion, mdibVersionCounter));
    }

    MdibEntity createEntity(@Nullable String parentHandle,
                            @Nullable List<String> childHandles,
                            AbstractDescriptor descriptor,
                            @Nullable List<AbstractState> states,
                            @Nullable BigInteger mdibVersionCounter) {
        mdibVersionCounter = Optional.ofNullable(mdibVersionCounter).orElse(baseVersion.getVersion());
        return mdibEntityFactory.createMdibEntity(
                parentHandle,
                Optional.ofNullable(childHandles).orElse(Collections.emptyList()),
                descriptor,
                Optional.ofNullable(states).orElse(Collections.emptyList()),
                MdibVersion.setVersionCounter(baseVersion, mdibVersionCounter));
    }
}
