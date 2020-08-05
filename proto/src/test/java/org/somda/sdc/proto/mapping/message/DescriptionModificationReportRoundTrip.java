package org.somda.sdc.proto.mapping.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.message.DescriptionModificationReport;
import org.somda.sdc.biceps.model.message.DescriptionModificationType;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.SetStringOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetStringOperationState;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;
import org.somda.sdc.proto.mapping.participant.factory.PojoToProtoTreeMapperFactory;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
public class DescriptionModificationReportRoundTrip {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private static final String HANDLE = Handles.OPERATION_0;
    private static final String SEQUENCE_ID = "Seq";

    @Test
    void roundTrip() {
        var pojoToProtoMapper = UT.getInjector().getInstance(PojoToProtoMapper.class);
        var protoToPojoMapper = UT.getInjector().getInstance(ProtoToPojoMapper.class);


        var report = new DescriptionModificationReport();
        report.setMdibVersion(BigInteger.TEN);
        report.setInstanceId(BigInteger.TWO);
        report.setSequenceId(SEQUENCE_ID);

        var reportPart = new DescriptionModificationReport.ReportPart();
        reportPart.setParentDescriptor("Somewhere");
        reportPart.setModificationType(DescriptionModificationType.UPT);

        var descriptor = new SetStringOperationDescriptor();
        reportPart.getDescriptor().add(descriptor);
        {
            descriptor.setHandle(HANDLE);
            descriptor.setOperationTarget(Handles.MDS_0);
            descriptor.setAccessLevel(AbstractOperationDescriptor.AccessLevel.RO);
            descriptor.setInvocationEffectiveTimeout(Duration.ofMinutes(1));
            descriptor.setMaxTimeToFinish(Duration.ofMinutes(12));
        }

        var state = new SetStringOperationState();
        reportPart.getState().add(state);
        {
            state.setOperatingMode(OperatingMode.DIS);
        }

        var mappedReport = pojoToProtoMapper.mapDescriptionModificationReport(report);
        var remappedReport = protoToPojoMapper.map(mappedReport);

        assertNotSame(report, remappedReport);
        assertEquals(report, remappedReport);
    }

}
