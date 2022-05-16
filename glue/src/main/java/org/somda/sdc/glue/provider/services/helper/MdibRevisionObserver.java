package org.somda.sdc.glue.provider.services.helper;

import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.common.event.StateModificationMessage;
import org.somda.sdc.biceps.model.history.ChangeSequenceReportType;
import org.somda.sdc.biceps.model.history.ChangeSequenceType;
import org.somda.sdc.biceps.model.history.HistoricMdibType;
import org.somda.sdc.biceps.model.history.HistoricReportType;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.common.MdibVersionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 TODO #142 docs
 */
public class MdibRevisionObserver implements MdibAccessObserver {
    private static final Logger LOG = LogManager.getLogger(MdibRevisionObserver.class);

    private final EventSourceAccess eventSourceAccess;
    private final ObjectFactory bicepsMessageFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final Logger instanceLogger;

    private ChangeSequenceReportType fullReport = new ChangeSequenceReportType();

    @AssistedInject
    MdibRevisionObserver(@Assisted EventSourceAccess eventSourceAccess,
                         ObjectFactory bicepsMessageFactory,
                         MdibVersionUtil mdibVersionUtil,
                         @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.eventSourceAccess = eventSourceAccess;
        this.bicepsMessageFactory = bicepsMessageFactory;
        this.mdibVersionUtil = mdibVersionUtil;
    }

    public void createInitialReport(MdibVersion mdibVersion) {
        var seqId = mdibVersion.getSequenceId();
        var insId = mdibVersion.getInstanceId();

        // First initial historic MDIB
        var historicMdib = new HistoricMdibType();
        historicMdib.setMdibVersion(mdibVersion.getVersion());
        historicMdib.setSequenceId(seqId);
        historicMdib.setInstanceId(insId);
        //TODO #142
        //historicMdib.setTime();
        //historicMdib.setMdDescription();  //TODO #142 use MdibMapper to get copy
        //historicMdib.setMdState();


        ChangeSequenceType changeSequence = new ChangeSequenceType();
        changeSequence.setSequenceId(seqId);
        changeSequence.setInstanceId(insId);
        changeSequence.setHistoricMdib(historicMdib);
        changeSequence.setHistoricReport(new ArrayList<>()); // TODO #142 no initial reports most likely?
        //changeSequence.setHistoricLocalizedText(); //TODO #142

        fullReport.setChangeSequence(new ArrayList<>(List.of(changeSequence)));
    }

    @Subscribe
    void onStateChange(StateModificationMessage modificationMessage) {
        var mdibVersion = modificationMessage.getMdibAccess().getMdibVersion();
        var historicReport = new HistoricReportType();

        //TODO #142
    }

    @Subscribe
    void onDescriptionChange(DescriptionModificationMessage modificationMessage) {
        instanceLogger.debug(modificationMessage);
        //TODO #142
    }
}
