package org.somda.sdc.proto.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.consumer.access.factory.RemoteMdibAccessFactory;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.proto.UnitTestUtil;
import org.somda.sdc.proto.mapping.factory.PojoToProtoTreeMapperFactory;
import org.somda.sdc.proto.mapping.factory.ProtoToPojoModificationsBuilderFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.function.BiConsumer;

@ExtendWith(LoggingTestWatcher.class)
class RoundTripTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    LocalMdibAccess mdibAccessSource;
    private PojoToProtoTreeMapper pojoToProtoMapper;
    private RemoteMdibAccess mdibAccessSink;
    private ProtoToPojoModificationsBuilderFactory protoToPojoMapperFactory;

    @BeforeEach
    void beforeEach() {
        mdibAccessSource = UT.getInjector().getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        pojoToProtoMapper = UT.getInjector().getInstance(PojoToProtoTreeMapperFactory.class).create(mdibAccessSource);
        mdibAccessSink = UT.getInjector().getInstance(RemoteMdibAccessFactory.class).createRemoteMdibAccess();
        protoToPojoMapperFactory = UT.getInjector().getInstance(ProtoToPojoModificationsBuilderFactory.class);
    }

    @Test
    @DisplayName("Full MDIB")
    void mapMdib() throws Exception {
        var modifications = MdibDescriptionModifications.create();
        var resultsToCompare = new ArrayList<BiConsumer<LocalMdibAccess, RemoteMdibAccess>>();

        attachTreeTests(modifications, resultsToCompare);

        mdibAccessSource.writeDescription(modifications);
        var mdibMsg = pojoToProtoMapper.mapMdib();
        var builder = protoToPojoMapperFactory.create(mdibMsg);
        mdibAccessSink.writeDescription(MdibVersion.create(), BigInteger.ZERO, BigInteger.ZERO, builder.get());

        for (var consumer : resultsToCompare) {
            consumer.accept(mdibAccessSource, mdibAccessSink);
        }
    }

    private void attachTreeTests(MdibDescriptionModifications modifications,
                                 ArrayList<BiConsumer<LocalMdibAccess, RemoteMdibAccess>> resultsToCompare) {
        resultsToCompare.add(new MdsRoundTrip(modifications));
        resultsToCompare.add(new VmdRoundTrip(modifications));
        resultsToCompare.add(new ChannelRoundTrip(modifications));
        resultsToCompare.add(new StringMetricRoundTrip(modifications));
        resultsToCompare.add(new EnumStringMetricRoundTrip(modifications));
        resultsToCompare.add(new NumericMetricRoundTrip(modifications));
        resultsToCompare.add(new RealTimeDistributionSampleArrayRoundTrip(modifications));
        resultsToCompare.add(new SystemContextRoundTrip(modifications));
        resultsToCompare.add(new EnsembleContextRoundTrip(modifications));
        resultsToCompare.add(new LocationContextRoundTrip(modifications));
        resultsToCompare.add(new AlertSystemRoundTrip(modifications));
        resultsToCompare.add(new ScoRoundTrip(modifications));
        resultsToCompare.add(new AlertConditionRoundTrip(modifications));
        resultsToCompare.add(new AlertSignalRoundTrip(modifications));
        resultsToCompare.add(new SetMetricStateOperationRoundTrip(modifications));
        resultsToCompare.add(new SetComponentStateOperationRoundTrip(modifications));
        resultsToCompare.add(new SetContextStateOperationRoundTrip(modifications));
        resultsToCompare.add(new SetAlertStateOperationRoundTrip(modifications));
        resultsToCompare.add(new ActivateOperationRoundTrip(modifications));
    }
}
