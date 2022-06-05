package org.somda.sdc.glue.provider.services;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.WebService;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.provider.services.helper.MdibRevisionObserver;
import org.somda.sdc.glue.provider.services.helper.factory.MdibRevisionObserverFactory;

/**
 * Implementation of the history service.
 */
public class HistoryService extends WebService {
    private final LocalMdibAccess mdibAccess;
    private final SoapUtil soapUtil;
    private final SoapFaultFactory faultFactory;
    private final ObjectFactory messageModelFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final WsAddressingUtil wsaUtil;
    private final MdibRevisionObserver mdibRevisionObserver;

    @AssistedInject
    HistoryService(@Assisted LocalMdibAccess mdibAccess,
                   SoapUtil soapUtil,
                   SoapFaultFactory faultFactory,
                   ObjectFactory messageModelFactory,
                   MdibVersionUtil mdibVersionUtil,
                   WsAddressingUtil wsaUtil,
                   MdibRevisionObserverFactory mdibRevisionObserverFactory) {
        this.mdibAccess = mdibAccess;
        this.soapUtil = soapUtil;
        this.faultFactory = faultFactory;
        this.messageModelFactory = messageModelFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.wsaUtil = wsaUtil;
        mdibRevisionObserver = mdibRevisionObserverFactory.createMdibRevisionObserver(this, mdibAccess);
        mdibRevisionObserver.createInitialReport(mdibAccess.getMdibVersion());
        mdibAccess.registerObserver(mdibRevisionObserver);
    }

    /*@MessageInterceptor(ActionConstants.ACTION_HISTORY_MDIB_REPORT)
    void historyMdibReport(RequestResponseObject requestResponseObject) throws SoapFaultException {
        //TODO #142
    }*/
}
